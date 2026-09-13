import { ChangeDetectionStrategy, Component, OnInit, computed, signal } from '@angular/core';
import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Skeleton } from 'primeng/skeleton';
import { ApexChart, ApexAxisChartSeries, ApexFill, ApexStroke, ApexTooltip, ApexXAxis, ChartComponent } from 'ng-apexcharts';
import { NetWorthService } from '../../service/networth.service';
import { NetWorthHistory, NetWorthPoint } from '../../model/networth';
import { DEFAULT_CURRENCY } from '../../model/currency';
import { toTimestamp } from '../../performance/performance.component';

const WINDOW_DAYS = 90;
const DAY_MS = 24 * 60 * 60 * 1000;

export type SparklineOptions = {
  series: ApexAxisChartSeries;
  chart: ApexChart;
  xaxis: ApexXAxis;
  stroke: ApexStroke;
  fill: ApexFill;
  tooltip: ApexTooltip;
  colors: string[];
};

@Component({
  selector: 'app-performance-summary',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CurrencyPipe, DecimalPipe, Skeleton, ChartComponent],
  templateUrl: './performance-summary.component.html',
  styleUrls: ['./performance-summary.component.css']
})
export class PerformanceSummaryComponent implements OnInit {

  readonly loading = signal(true);
  readonly history = signal<NetWorthHistory | null>(null);

  readonly currency = computed(() => this.history()?.currency ?? DEFAULT_CURRENCY);

  readonly points = computed<NetWorthPoint[]>(() => {
    const all = this.history()?.points ?? [];
    if (all.length === 0) return [];
    const start = toTimestamp(all[all.length - 1].date) - WINDOW_DAYS * DAY_MS;
    return all.filter(point => toTimestamp(point.date) >= start);
  });

  readonly latest = computed(() => {
    const points = this.points();
    return points.length === 0 ? null : points[points.length - 1];
  });

  readonly change = computed(() => {
    const points = this.points();
    if (points.length < 2) return null;
    return points[points.length - 1].totalWorth - points[0].totalWorth;
  });

  readonly changePct = computed(() => {
    const points = this.points();
    const change = this.change();
    if (change === null || points[0].totalWorth === 0) return null;
    return change / points[0].totalWorth * 100;
  });

  readonly chart = computed<Partial<SparklineOptions>>(() => ({
    series: [{ name: 'Net worth', data: this.points().map(point => [toTimestamp(point.date), point.totalWorth]) }],
    chart: { type: 'area', height: 90, sparkline: { enabled: true } },
    xaxis: { type: 'datetime' },
    stroke: { width: 2, curve: 'straight' },
    fill: { type: 'solid', opacity: 0.12 },
    colors: ['#1b1b1b'],
    tooltip: { x: { format: 'yyyy-MM-dd' }, y: { formatter: (value: number) => this.money(value) } }
  }));

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
