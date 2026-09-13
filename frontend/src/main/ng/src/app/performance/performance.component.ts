import { ChangeDetectionStrategy, Component, OnInit, computed, signal } from '@angular/core';
import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { PrimeTemplate } from 'primeng/api';
import { Panel } from 'primeng/panel';
import { ButtonDirective } from 'primeng/button';
import { Skeleton } from 'primeng/skeleton';
import { Tooltip } from 'primeng/tooltip';
import { TableModule } from 'primeng/table';
import {
  ApexAxisChartSeries, ApexChart, ApexDataLabels, ApexFill, ApexLegend, ApexStroke, ApexTooltip, ApexXAxis,
  ApexYAxis, ChartComponent
} from 'ng-apexcharts';
import { MenuComponent } from '../menu/menu.component';
import { NetWorthService } from '../service/networth.service';
import { NetWorthHistory, NetWorthPoint } from '../model/networth';
import { DEFAULT_CURRENCY } from '../model/currency';
import { ASSET_COLOURS, FALLBACK_ASSET_COLOUR } from '../util/assetcolours';

export type RangeId = '1M' | '3M' | '6M' | 'YTD' | '1Y' | 'ALL';

export interface RangeOption {
  id: RangeId;
  label: string;
}

export interface RangeSummary {
  endWorth: number;
  change: number;
  changePct: number | null;
  invested: number;
  marketGain: number;
}

export type PerformanceChartOptions = {
  series: ApexAxisChartSeries;
  chart: ApexChart;
  xaxis: ApexXAxis;
  yaxis: ApexYAxis;
  legend: ApexLegend;
  dataLabels: ApexDataLabels;
  tooltip: ApexTooltip;
  stroke: ApexStroke;
  fill: ApexFill;
  colors: string[];
};

const RANGE_MONTHS: { [range: string]: number } = { '1M': 1, '3M': 3, '6M': 6, '1Y': 12 };

const CHART_HEIGHT = 340;

export function rangeStart(range: RangeId, anchor: string): string | null {
  if (range === 'ALL') return null;
  const [year, month, day] = anchor.split('-').map(Number);
  if (range === 'YTD') return `${year}-01-01`;

  const targetMonth = month - 1 - RANGE_MONTHS[range];
  const lastDay = new Date(Date.UTC(year, targetMonth + 1, 0)).getUTCDate();
  return new Date(Date.UTC(year, targetMonth, Math.min(day, lastDay))).toISOString().slice(0, 10);
}

export function toTimestamp(date: string): number {
  const [year, month, day] = date.split('-').map(Number);
  return Date.UTC(year, month - 1, day);
}

export function compactAmount(value: number): string {
  const absolute = Math.abs(value);
  if (absolute >= 1_000_000) {
    return `${(value / 1_000_000).toFixed(1)}M`;
  }
  if (absolute >= 1_000) {
    return `${(value / 1_000).toFixed(0)}k`;
  }
  return `${Math.round(value)}`;
}

@Component({
  selector: 'app-performance',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './performance.component.html',
  styleUrls: ['./performance.component.css'],
  imports: [MenuComponent, Panel, ButtonDirective, Skeleton, Tooltip, TableModule, PrimeTemplate, CurrencyPipe,
    DecimalPipe, ChartComponent]
})
export class PerformanceComponent implements OnInit {

  readonly ranges: RangeOption[] = [
    { id: '1M', label: '1M' },
    { id: '3M', label: '3M' },
    { id: '6M', label: '6M' },
    { id: 'YTD', label: 'YTD' },
    { id: '1Y', label: '1Y' },
    { id: 'ALL', label: 'All' }
  ];

  readonly loading = signal(true);
  readonly history = signal<NetWorthHistory | null>(null);
  readonly range = signal<RangeId>('ALL');

  readonly currency = computed(() => this.history()?.currency ?? DEFAULT_CURRENCY);

  readonly points = computed<NetWorthPoint[]>(() => {
    const all = this.history()?.points ?? [];
    if (all.length === 0) return [];
    const start = rangeStart(this.range(), all[all.length - 1].date);
    return start === null ? all : all.filter(point => point.date >= start);
  });

  readonly hasHistory = computed(() => (this.history()?.points.length ?? 0) >= 2);

  readonly summary = computed<RangeSummary | null>(() => {
    const points = this.points();
    if (points.length < 2) return null;

    const first = points[0];
    const last = points[points.length - 1];
    const change = last.totalWorth - first.totalWorth;
    const invested = last.totalSpent - first.totalSpent;
    return {
      endWorth: last.totalWorth,
      change,
      changePct: first.totalWorth === 0 ? null : change / first.totalWorth * 100,
      invested,
      marketGain: change - invested
    };
  });

  readonly assetClasses = computed<string[]>(() => {
    const points = this.points();
    const found = new Set(points.flatMap(point =>
      Object.keys(point.assetWorth).filter(assetClass => point.assetWorth[assetClass] !== 0)));
    const known = Object.keys(ASSET_COLOURS).filter(assetClass => found.has(assetClass));
    const unknown = [...found].filter(assetClass => !(assetClass in ASSET_COLOURS)).sort();
    return [...known, ...unknown];
  });

  readonly worthChart = computed<Partial<PerformanceChartOptions>>(() => {
    const points = this.points();
    return {
      series: [
        { name: 'Worth', data: points.map(point => [toTimestamp(point.date), point.totalWorth]) },
        { name: 'Invested', data: points.map(point => [toTimestamp(point.date), point.totalSpent]) }
      ],
      chart: { type: 'area', height: CHART_HEIGHT, toolbar: { show: false }, zoom: { enabled: false } },
      colors: ['#1b1b1b', '#ef9f27'],
      stroke: { width: [2, 2], curve: 'straight', dashArray: [0, 5] },
      fill: { type: 'solid', opacity: [0.12, 0] },
      ...this.sharedOptions()
    };
  });

  readonly assetChart = computed<Partial<PerformanceChartOptions>>(() => {
    const points = this.points();
    const assetClasses = this.assetClasses();
    return {
      series: assetClasses.map(assetClass => ({
        name: assetClass,
        data: points.map(point => [toTimestamp(point.date), point.assetWorth[assetClass] ?? 0])
      })),
      chart: {
        type: 'area', height: CHART_HEIGHT, stacked: true, toolbar: { show: false }, zoom: { enabled: false }
      },
      colors: assetClasses.map(assetClass => ASSET_COLOURS[assetClass] ?? FALLBACK_ASSET_COLOUR),
      stroke: { width: 1, curve: 'straight' },
      fill: { type: 'solid', opacity: 0.75 },
      ...this.sharedOptions()
    };
  });

  constructor(private netWorthService: NetWorthService) { }

  ngOnInit(): void {
    this.netWorthService.getHistory().subscribe({
      next: data => {
        this.history.set(data);
        this.loading.set(false);
      },
      error: error => {
        console.log(error);
        this.loading.set(false);
      }
    });
  }

  setRange(range: RangeId): void {
    this.range.set(range);
  }

  private sharedOptions(): Partial<PerformanceChartOptions> {
    return {
      dataLabels: { enabled: false },
      xaxis: { type: 'datetime' },
      yaxis: { labels: { formatter: (value: number) => compactAmount(value) } },
      legend: { position: 'bottom' },
      tooltip: {
        shared: true,
        x: { format: 'yyyy-MM-dd' },
        y: { formatter: (value: number) => this.money(value) }
      }
    };
  }

  private money(value: number): string {
    try {
      return new Intl.NumberFormat(undefined, {
        style: 'currency', currency: this.currency(), maximumFractionDigits: 0
      }).format(value);
    } catch {
      return value.toFixed(0);
    }
  }
}
