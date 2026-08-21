import { ChangeDetectorRef, Component } from '@angular/core';
import { MessageService, PrimeTemplate } from 'primeng/api';
import { FireProjection, FireProjectionInput, FireYear } from '../model/fire';
import { FireService } from '../service/fire.service';
import { NetWorthService } from '../service/networth.service';
import { MenuComponent } from '../menu/menu.component';
import { Bind } from 'primeng/bind';
import { Panel } from 'primeng/panel';
import { ButtonDirective } from 'primeng/button';
import { Ripple } from 'primeng/ripple';
import { Tooltip } from 'primeng/tooltip';
import { TableModule } from 'primeng/table';
import { InputNumber } from 'primeng/inputnumber';
import { Select } from 'primeng/select';
import { Checkbox } from 'primeng/checkbox';
import { Tag } from 'primeng/tag';
import { Toast } from 'primeng/toast';
import { Skeleton } from 'primeng/skeleton';
import { FormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';

interface ChartPoint {
  year: number;
  age: number;
  x: number;
  nominalY: number;
  realY: number;
  balance: number;
  realBalance: number;
  phase: string;
}

interface AxisTick {
  position: number;
  label: string;
}

interface Chart {
  nominalPath: string;
  realPath: string;
  points: ChartPoint[];
  lastYear: number;
  fireY: number | null;
  retirementX: number | null;
  pensionX: number | null;
  xTicks: AxisTick[];
  yTicks: AxisTick[];
}

const DEFAULT_CURRENCY = 'HUF';

/** The plot area inside the SVG viewBox, leaving room for the axis labels. */
const CHART = { width: 820, height: 280, left: 68, right: 16, top: 16, bottom: 30 };

@Component({
    selector: 'app-fire',
    templateUrl: './fire.component.html',
    styleUrls: ['./fire.component.css'],
    imports: [MenuComponent, Bind, Panel, ButtonDirective, Ripple, Tooltip, TableModule, PrimeTemplate,
        InputNumber, Select, Checkbox, Tag, Toast, Skeleton, FormsModule, DecimalPipe]
})
export class FireComponent {

  readonly chartBox = CHART;

  currency: string = DEFAULT_CURRENCY;
  currencyOptions: string[] = [DEFAULT_CURRENCY];

  portfolioValue: number = 0;
  portfolioLoading: boolean = true;
  unconvertedCurrencies: string[] = [];

  otherAssets: number = 0;
  monthlyContribution: number = 200000;
  annualContributionIncrease: number = 3;
  annualReturn: number = 4;
  inflation: number = 3;

  annualSpending: number = 6000000;
  withdrawalRate: number = 3;
  useCustomFireNumber: boolean = false;
  customFireNumber: number | null = null;

  hasPension: boolean = false;
  monthlyPension: number | null = null;
  pensionAge: number | null = 65;

  currentAge: number = 32;
  retireWhenReady: boolean = true;
  retirementAge: number | null = null;
  lifeExpectancy: number = 80;

  projection: FireProjection | null = null;
  chart: Chart | null = null;
  calculating: boolean = false;

  hoveredYear: number | null = null;

  constructor(
    private fireService: FireService,
    private netWorthService: NetWorthService,
    private messageService: MessageService,
    private cdr: ChangeDetectorRef
  ) {
    this.loadPortfolio();
  }

  /** The starting pot before any growth: what is tracked, plus whatever is not. */
  get startingValue(): number {
    return this.portfolioValue + (this.otherAssets ?? 0);
  }

  /** Mirrors the backend so the target updates as you type, rather than only after calculating. */
  get derivedFireNumber(): number {
    if (this.useCustomFireNumber) return this.customFireNumber ?? 0;
    if (!this.withdrawalRate) return 0;
    return (this.annualSpending ?? 0) / (this.withdrawalRate / 100);
  }

  /** A typed-in target implies the income it can support, which is the more meaningful number. */
  get impliedSpending(): number {
    return (this.customFireNumber ?? 0) * (this.withdrawalRate ?? 0) / 100;
  }

  get canCalculate(): boolean {
    if (this.currentAge == null || this.lifeExpectancy == null) return false;
    if (this.lifeExpectancy <= this.currentAge) return false;
    if (!this.retireWhenReady && this.retirementAge == null) return false;
    return this.derivedFireNumber > 0;
  }

  onCurrencyChange(): void {
    this.projection = null;
    this.chart = null;
    this.loadPortfolio();
  }

  private loadPortfolio(): void {
    this.portfolioLoading = true;
    this.netWorthService.getNetWorth(this.currency).subscribe({
      next: (data) => {
        this.portfolioValue = data.totalWorth;
        this.unconvertedCurrencies = data.unconvertedCurrencies ?? [];
        if (data.availableCurrencies?.length) {
          this.currencyOptions = data.availableCurrencies;
        }
        this.portfolioLoading = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.portfolioValue = 0;
        this.portfolioLoading = false;
        this.showError(error, 'Could not read your portfolio');
        this.cdr.markForCheck();
      }
    });
  }

  calculate(): void {
    if (!this.canCalculate) return;

    this.calculating = true;
    this.fireService.project(this.toInput()).subscribe({
      next: (data) => {
        this.calculating = false;
        this.projection = data;
        this.portfolioValue = data.portfolioValue;
        this.unconvertedCurrencies = data.unconvertedCurrencies ?? [];
        this.chart = this.buildChart(data);
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.calculating = false;
        this.projection = null;
        this.chart = null;
        this.showError(error, 'Could not calculate');
        this.cdr.markForCheck();
      }
    });
  }

  download(): void {
    if (!this.canCalculate) return;

    this.fireService.downloadCsv(this.toInput()).subscribe({
      next: (data) => {
        let a = document.createElement('a');
        a.href = window.URL.createObjectURL(data as Blob);
        a.download = 'fire-projection.csv';
        a.click();
      },
      error: (error) => this.showError(error, 'Could not export')
    });
  }

  private toInput(): FireProjectionInput {
    return {
      currency: this.currency,
      otherAssets: this.otherAssets ?? 0,
      monthlyContribution: this.monthlyContribution ?? 0,
      annualContributionIncrease: this.annualContributionIncrease ?? 0,
      annualReturn: this.annualReturn,
      inflation: this.inflation,
      annualSpending: this.useCustomFireNumber ? null : this.annualSpending,
      withdrawalRate: this.withdrawalRate,
      fireNumber: this.useCustomFireNumber ? this.customFireNumber : null,
      monthlyPension: this.hasPension ? this.monthlyPension : null,
      pensionAge: this.hasPension ? this.pensionAge : null,
      currentAge: this.currentAge,
      retirementAge: this.retireWhenReady ? null : this.retirementAge,
      lifeExpectancy: this.lifeExpectancy
    };
  }

  /**
   * Lays the timeline out in SVG user units. Both curves share one scale so the gap between them reads as
   * what inflation takes out.
   */
  private buildChart(projection: FireProjection): Chart | null {
    const timeline = projection.timeline ?? [];
    if (timeline.length < 2) return null;

    const plotWidth = CHART.width - CHART.left - CHART.right;
    const plotHeight = CHART.height - CHART.top - CHART.bottom;
    const lastYear = timeline[timeline.length - 1].year || 1;

    // The target sits on the same axis as the curves, so a target far above the pot must still fit.
    const peak = Math.max(
      ...timeline.map(point => point.balance),
      projection.fireNumber || 0,
      1);
    const top = this.niceCeiling(peak);

    const xFor = (year: number) => CHART.left + (year / lastYear) * plotWidth;
    const yFor = (value: number) => CHART.top + plotHeight - (value / top) * plotHeight;

    const points: ChartPoint[] = timeline.map(point => ({
      year: point.year,
      age: point.age,
      x: xFor(point.year),
      nominalY: yFor(point.balance),
      realY: yFor(point.realBalance),
      balance: point.balance,
      realBalance: point.realBalance,
      phase: point.phase,
    }));

    return {
      nominalPath: this.toPath(points, p => p.nominalY),
      realPath: this.toPath(points, p => p.realY),
      points: points,
      lastYear: lastYear,
      fireY: projection.fireNumber > 0 ? yFor(projection.fireNumber) : null,
      retirementX: projection.retirementYear != null ? xFor(projection.retirementYear) : null,
      pensionX: this.pensionYear(timeline),
      xTicks: this.xTicks(lastYear, xFor),
      yTicks: this.yTicks(top, yFor),
    };
  }

  /** Where the pension starts, read off the timeline rather than recomputed from the inputs. */
  private pensionYear(timeline: FireYear[]): number | null {
    const first = timeline.find(point => point.pension > 0);
    if (!first || first.year === 0) return null;

    const plotWidth = CHART.width - CHART.left - CHART.right;
    const lastYear = timeline[timeline.length - 1].year || 1;
    return CHART.left + (first.year / lastYear) * plotWidth;
  }

  private toPath(points: ChartPoint[], y: (point: ChartPoint) => number): string {
    return points.map((point, index) =>
      `${index === 0 ? 'M' : 'L'}${point.x.toFixed(2)},${y(point).toFixed(2)}`).join(' ');
  }

  private xTicks(lastYear: number, xFor: (year: number) => number): AxisTick[] {
    const step = Math.max(1, Math.round(lastYear / 6));
    const ticks: AxisTick[] = [];
    for (let year = 0; year <= lastYear; year += step) {
      ticks.push({ position: xFor(year), label: `${year}y` });
    }
    return ticks;
  }

  private yTicks(top: number, yFor: (value: number) => number): AxisTick[] {
    const ticks: AxisTick[] = [];
    for (let i = 0; i <= 4; i++) {
      const value = (top / 4) * i;
      ticks.push({ position: yFor(value), label: this.formatCompact(value) });
    }
    return ticks;
  }

  /** Rounds the axis top up to 1, 2 or 5 times a power of ten so the gridline labels stay readable. */
  private niceCeiling(value: number): number {
    const magnitude = Math.pow(10, Math.floor(Math.log10(value)));
    const normalised = value / magnitude;
    const rounded = normalised <= 1 ? 1 : normalised <= 2 ? 2 : normalised <= 5 ? 5 : 10;
    return rounded * magnitude;
  }

  /**
   * The plot stretches to fill its container, so the pointer maps back onto a year by simple proportion.
   */
  onChartMove(event: MouseEvent): void {
    if (!this.chart) return;

    const bounds = (event.currentTarget as SVGSVGElement).getBoundingClientRect();
    if (!bounds.width) return;

    const svgX = ((event.clientX - bounds.left) / bounds.width) * CHART.width;
    const plotWidth = CHART.width - CHART.left - CHART.right;
    const year = Math.round(((svgX - CHART.left) / plotWidth) * this.chart.lastYear);

    this.hoveredYear = (year < 0 || year > this.chart.lastYear) ? null : year;
  }

  setHoveredYear(year: number | null): void {
    this.hoveredYear = year;
  }

  /**
   * Retirement at five-year steps, always including the year it starts, the year the money runs out and the
   * end of the plan. Years after depletion are all zero, so they are left out.
   */
  get drawdownRows(): FireYear[] {
    const drawdown = (this.projection?.timeline ?? []).filter(point => point.phase === 'DRAWDOWN');
    if (drawdown.length === 0) return [];

    const depletedAt = this.projection?.depletedAtAge;
    const shown = (depletedAt != null) ? drawdown.filter(point => point.age <= depletedAt) : drawdown;
    if (shown.length === 0) return [];

    const first = shown[0];
    const last = shown[shown.length - 1];
    return shown.filter(point => point === first || point === last || point.age % 5 === 0);
  }

  get hoveredPoint(): ChartPoint | null {
    if (this.hoveredYear == null) return null;
    return this.chart?.points.find(point => point.year === this.hoveredYear) ?? null;
  }

  /** Keeps the hover readout inside the plot when hovering near the right-hand edge. */
  tooltipX(point: ChartPoint): number {
    const width = 132;
    return Math.min(point.x + 10, CHART.width - CHART.right - width);
  }

  /** Which balance the progress column is measured on depends on how the target was set. */
  get fireNumberBasisHint(): string {
    return this.projection?.fireNumberInTodaysMoney
      ? `Measured on the balance in today's money, because the target came from spending in today's money.`
      : `Measured on the balance itself, because the target was set as a plain amount.`;
  }

  /** The year the target is cleared is shown alongside the fixed horizons, and flagged as such. */
  isFiYear(year: FireYear): boolean {
    return this.projection?.fiReached === true && year.year === this.projection.fiYear;
  }

  /** The pension columns only earn their space when a pension is actually being paid. */
  get hasPensionRows(): boolean {
    return this.projection?.timeline.some(point => point.pension > 0) ?? false;
  }

  isDrawdown(year: FireYear): boolean {
    return year.phase === 'DRAWDOWN';
  }

  formatCompact(value: number): string {
    try {
      return new Intl.NumberFormat(undefined, {
        style: 'currency',
        currency: this.currency,
        notation: 'compact',
        maximumFractionDigits: 1
      }).format(value);
    } catch {
      return value.toFixed(0);
    }
  }

  private showError(error: any, summary: string): void {
    this.messageService.add({
      severity: 'error',
      summary: summary,
      detail: error?.error?.error ?? 'Something went wrong, please try again.',
      life: 8000
    });
  }
}
