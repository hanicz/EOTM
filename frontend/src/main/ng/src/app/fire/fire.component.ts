import { ChangeDetectorRef, Component } from '@angular/core';
import { switchMap } from 'rxjs/operators';
import { MessageService, PrimeTemplate } from 'primeng/api';
import { FireProjection, FireProjectionInput, FireYear } from '../model/fire';
import { FireService } from '../service/fire.service';
import { UserService } from '../service/user.service';
import { DEFAULT_CURRENCY } from '../model/currency';
import { MenuComponent } from '../menu/menu.component';
import { Bind } from 'primeng/bind';
import { Panel } from 'primeng/panel';
import { ButtonDirective } from 'primeng/button';
import { Ripple } from 'primeng/ripple';
import { Tooltip } from 'primeng/tooltip';
import { TableModule } from 'primeng/table';
import { InputNumber } from 'primeng/inputnumber';
import { Checkbox } from 'primeng/checkbox';
import { Toast } from 'primeng/toast';
import { Skeleton } from 'primeng/skeleton';
import { FormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import {
  ApexAnnotations, ApexAxisChartSeries, ApexChart, ApexDataLabels, ApexFill, ApexGrid, ApexLegend,
  ApexMarkers, ApexStroke, ApexTooltip, ApexXAxis, ApexYAxis, ChartComponent
} from 'ng-apexcharts';
import { compactAmount, money } from '../util/format';
import { Milestone, dotYears, nextMilestone } from '../util/fireplan';

export type FireChartOptions = {
  series: ApexAxisChartSeries;
  chart: ApexChart;
  xaxis: ApexXAxis;
  yaxis: ApexYAxis;
  legend: ApexLegend;
  dataLabels: ApexDataLabels;
  tooltip: ApexTooltip;
  stroke: ApexStroke;
  fill: ApexFill;
  grid: ApexGrid;
  markers: ApexMarkers;
  annotations: ApexAnnotations;
  colors: string[];
};

const NOMINAL_COLOUR = '#ef9f27';
const REAL_COLOUR = '#1b1b1b';
const TARGET_COLOUR = '#7a8c5c';
const MARKER_COLOUR = '#b4b2a9';

const CHART_HEIGHT = 560;

const DEFAULT_MONTHLY_CONTRIBUTION = 1200000;

@Component({
    selector: 'app-fire',
    templateUrl: './fire.component.html',
    styleUrls: ['./fire.component.css'],
    imports: [MenuComponent, Bind, Panel, ButtonDirective, Ripple, Tooltip, TableModule, PrimeTemplate,
        InputNumber, Checkbox, Toast, Skeleton, FormsModule, DecimalPipe, ChartComponent]
})
export class FireComponent {

  currency: string = DEFAULT_CURRENCY;

  portfolioValue: number = 0;
  portfolioLoading: boolean = true;
  unconvertedCurrencies: string[] = [];

  otherAssets: number = 0;
  monthlyContribution: number = DEFAULT_MONTHLY_CONTRIBUTION;
  annualContributionIncrease: number = 3;
  annualReturn: number = 5.5;
  inflation: number = 3;

  annualSpending: number = 6000000;
  withdrawalRate: number = 3;
  useCustomFireNumber: boolean = true;
  customFireNumber: number | null = 450000000;

  hasPension: boolean = true;
  monthlyPension: number | null = 700000;
  pensionAge: number | null = 65;

  hasUnemploymentBenefit: boolean = false;
  unemploymentBenefit: number | null = 1000000;

  currentAge: number = 32;
  retireWhenReady: boolean = true;
  retirementAge: number | null = null;
  lifeExpectancy: number = 80;

  projection: FireProjection | null = null;
  chart: Partial<FireChartOptions> | null = null;
  milestone: Milestone | null = null;
  calculating: boolean = false;

  mobileView: 'projection' | 'assumptions' = 'projection';
  pensionOpen: boolean = true;
  timingOpen: boolean = true;

  constructor(
    private fireService: FireService,
    private userService: UserService,
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

  private loadPortfolio(): void {
    this.portfolioLoading = true;
    this.userService.getPreferredCurrency().pipe(
      switchMap(currency => {
        this.currency = currency;
        return this.fireService.getSnapshot(currency);
      })
    ).subscribe({
      next: (data) => {
        this.portfolioValue = data.netWorth;
        this.unconvertedCurrencies = data.unconvertedCurrencies ?? [];
        this.portfolioLoading = false;
        if (data.hasCashFlow && data.monthlySavings > 0
          && this.monthlyContribution === DEFAULT_MONTHLY_CONTRIBUTION) {
          this.monthlyContribution = Math.round(data.monthlySavings);
        }
        this.cdr.markForCheck();
        this.calculate();
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
        this.chart = this.buildChart(data);
        this.milestone = nextMilestone(data);
        this.portfolioValue = data.portfolioValue;
        this.unconvertedCurrencies = data.unconvertedCurrencies ?? [];
        this.mobileView = 'projection';
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.calculating = false;
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

  showAssumptions(): void {
    this.mobileView = 'assumptions';
  }

  showProjection(): void {
    this.mobileView = 'projection';
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
      unemploymentBenefit: this.hasUnemploymentBenefit ? this.unemploymentBenefit : null,
      currentAge: this.currentAge,
      retirementAge: this.retireWhenReady ? null : this.retirementAge,
      lifeExpectancy: this.lifeExpectancy
    };
  }

  /** At year zero the two balances coincide, so this reads the same whichever money the target is in. */
  get progressPct(): number {
    const start = this.projection?.timeline?.[0];
    return start ? Math.max(0, start.pctOfFireNumber) : 0;
  }

  get monthlySpendingSupported(): number {
    return (this.projection?.annualSpending ?? 0) / 12;
  }

  /**
   * One filled curve for the balance, a thin one for what it is worth today, and a flat dashed line for
   * the target so it earns a legend entry rather than being a bare annotation.
   */
  private buildChart(projection: FireProjection): Partial<FireChartOptions> | null {
    const timeline = projection.timeline ?? [];
    if (timeline.length < 2) return null;

    const target = projection.fireNumber;
    return {
      series: [
        {
          name: 'Projected portfolio value',
          data: timeline.map(point => [point.age, point.balance] as [number, number])
        },
        {
          name: "In today's money",
          data: timeline.map(point => [point.age, point.realBalance] as [number, number])
        },
        {
          name: 'FIRE target (' + compactAmount(target) + ' ' + projection.currency + ')',
          data: timeline.map(point => [point.age, target] as [number, number])
        }
      ],
      chart: {
        type: 'area', height: CHART_HEIGHT, toolbar: { show: false }, zoom: { enabled: false },
        animations: { enabled: false }, fontFamily: 'Poppins, sans-serif'
      },
      colors: [NOMINAL_COLOUR, REAL_COLOUR, TARGET_COLOUR],
      stroke: { width: [2.5, 1.5, 1.5], curve: 'straight', dashArray: [0, 0, 5] },
      fill: { type: 'solid', opacity: [0.14, 0, 0] },
      grid: { borderColor: '#ece9e0', strokeDashArray: 4, padding: { left: 4, right: 16, top: 16 } },
      dataLabels: { enabled: false },
      markers: { size: 0, discrete: this.chartDots(projection) },
      annotations: this.chartAnnotations(projection),
      xaxis: {
        type: 'numeric', tickAmount: 6, decimalsInFloat: 0,
        title: { text: 'Age', style: { fontWeight: 500, color: '#8a887f' } },
        labels: { formatter: (value: string) => Math.round(Number(value)).toString() },
        tooltip: { enabled: false }
      },
      yaxis: { labels: { formatter: (value: number) => compactAmount(value) } },
      legend: { position: 'bottom', markers: { strokeWidth: 0 } },
      tooltip: {
        shared: true,
        x: { formatter: (value: number) => 'Age ' + Math.round(value) },
        y: { formatter: (value: number) => money(value, projection.currency) }
      }
    };
  }

  private chartDots(projection: FireProjection): any[] {
    const years = dotYears(projection);
    return projection.timeline
      .map((point, index) => ({ year: point.year, index: index }))
      .filter(entry => years.includes(entry.year))
      .map(entry => ({
        seriesIndex: 0,
        dataPointIndex: entry.index,
        fillColor: NOMINAL_COLOUR,
        strokeColor: '#ffffff',
        size: 5
      }));
  }

  /**
   * The dots carry the balance at each step and the vertical markers carry the events. A plan that
   * retires the moment the target is cleared puts both on the same age, so they share one label.
   */
  private chartAnnotations(projection: FireProjection): ApexAnnotations {
    const years = dotYears(projection);
    const points = projection.timeline
      .filter(point => years.includes(point.year))
      .map(point => ({
        x: point.age,
        y: point.balance,
        marker: { size: 0 },
        label: {
          text: compactAmount(point.balance),
          offsetY: -10,
          borderWidth: 0,
          style: { background: 'transparent', color: REAL_COLOUR, fontSize: '11px', fontWeight: '600' }
        }
      }));

    const events: any[] = [];
    const fiAge = projection.fiReached ? projection.fiAge : null;
    const retireAge = projection.retirementAge;
    const startAge = projection.timeline[0].age;

    if (fiAge != null) {
      events.push(this.marker(fiAge, retireAge === fiAge ? 'Target reached · retire' : 'Target reached'));
    }
    if (retireAge != null && retireAge !== fiAge) {
      events.push(this.marker(retireAge, 'Retire'));
    }
    if (projection.pensionYear != null && projection.pensionYear > 0) {
      events.push(this.marker(startAge + projection.pensionYear, 'Pension'));
    }

    return { points: points, xaxis: events };
  }

  private marker(age: number, text: string): any {
    return {
      x: age,
      strokeDashArray: 4,
      borderColor: MARKER_COLOUR,
      label: {
        text: text,
        orientation: 'horizontal',
        position: 'top',
        offsetY: -2,
        borderColor: '#ece9e0',
        style: { background: '#ffffff', color: '#5f5e5a', fontSize: '11px' }
      }
    };
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

  private showError(error: any, summary: string): void {
    this.messageService.add({
      severity: 'error',
      summary: summary,
      detail: error?.error?.error ?? 'Something went wrong, please try again.',
      life: 8000
    });
  }
}
