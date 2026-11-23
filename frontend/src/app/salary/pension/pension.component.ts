import { ChangeDetectorRef, Component } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MessageService, PrimeTemplate } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { Checkbox } from 'primeng/checkbox';
import { InputNumber } from 'primeng/inputnumber';
import { Ripple } from 'primeng/ripple';
import { Select } from 'primeng/select';
import { Skeleton } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { Toast } from 'primeng/toast';
import { Tooltip } from 'primeng/tooltip';
import {
  ApexAnnotations, ApexAxisChartSeries, ApexChart, ApexDataLabels, ApexFill, ApexGrid, ApexLegend,
  ApexMarkers, ApexStroke, ApexTooltip, ApexXAxis, ApexYAxis, ChartComponent
} from 'ng-apexcharts';
import {
  DegressioMode,
  PensionProjection,
  PensionProjectionInput,
  PensionScenarioInput,
  PensionStep
} from '../../model/pension-projection';
import { SalaryService } from '../../service/salary.service';
import { DeltaComponent } from '../../util/delta.component';
import { compactAmount, money } from '../../util/format';

export type PensionChartOptions = {
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

interface ExtraYearRow {
  fromAge: number;
  toAge: number;
  serviceYears: number;
  monthlyPension: number;
  gain: number;
}

const SERIES_COLORS = ['#a85f00', '#0f8a7a', '#4a7fd4', '#c0407a'];

const MARKER_COLOUR = '#b4b2a9';

const CHART_HEIGHT = 560;

const DEFAULT_CURRENT_AGE = 32;
const DEFAULT_STOP_AGE = 50;
const DEFAULT_RETIREMENT_AGE = 65;
const DEFAULT_REAL_WAGE_GROWTH = 1.5;
const DEFAULT_INFLATION = 3;
const DEFAULT_REAL_GROWTH = 2;

@Component({
    selector: 'app-salary-pension',
    templateUrl: './pension.component.html',
    styleUrls: ['./pension.component.css'],
    imports: [ButtonDirective, Ripple, Tooltip, TableModule, PrimeTemplate, InputNumber, Checkbox,
        Select, Toast, Skeleton, FormsModule, DecimalPipe, DeltaComponent, ChartComponent]
})
export class SalaryPensionComponent {

  currentAge: number = DEFAULT_CURRENT_AGE;
  stopWorkingAge: number = DEFAULT_STOP_AGE;
  retirementAge: number = DEFAULT_RETIREMENT_AGE;
  yearsAlreadyWorked: number = 10;

  currentGrossMonthly: number | null = null;
  overrideGross: boolean = false;
  customGross: number | null = null;
  priorAverageGrossMonthly: number | null = null;

  realWageGrowth: number = DEFAULT_REAL_WAGE_GROWTH;
  inflation: number = DEFAULT_INFLATION;
  degresszio: DegressioMode = 'FROZEN';
  includeThirteenthMonth: boolean = true;

  useFlat: boolean = true;
  useGrowth: boolean = true;
  growthPct: number = DEFAULT_REAL_GROWTH;
  useNominalLock: boolean = true;
  useSteps: boolean = false;
  steps: PensionStep[] = [
    { untilAge: 45, realGrowthPct: 5 },
    { untilAge: 65, realGrowthPct: 0 }
  ];

  readonly degressioOptions = [
    { label: 'Frozen, as the law stands', value: 'FROZEN' as DegressioMode },
    { label: 'Indexed with wages', value: 'INDEXED' as DegressioMode },
    { label: 'Abolished by then', value: 'IGNORED' as DegressioMode }
  ];

  projection: PensionProjection | null = null;
  chart: Partial<PensionChartOptions> | null = null;
  salaryLoading: boolean = true;
  calculating: boolean = false;

  mobileView: 'projection' | 'assumptions' = 'projection';
  pathOpen: boolean = true;
  assumptionsOpen: boolean = true;

  constructor(
    private salaryService: SalaryService,
    private messageService: MessageService,
    private cdr: ChangeDetectorRef
  ) {
    this.loadSalary();
  }

  get effectiveGross(): number | null {
    return this.overrideGross ? this.customGross : this.currentGrossMonthly;
  }

  get serviceYears(): number {
    return this.yearsAlreadyWorked + Math.max(0, this.stopWorkingAge - this.currentAge);
  }

  get gapYears(): number {
    return Math.max(0, this.retirementAge - this.stopWorkingAge);
  }

  get scenarioCount(): number {
    return this.scenarioInputs().length;
  }

  get canCalculate(): boolean {
    return (this.effectiveGross ?? 0) > 0
      && this.scenarioCount > 0
      && this.stopWorkingAge >= this.currentAge
      && this.retirementAge >= this.stopWorkingAge
      && (!this.useSteps || this.steps.every(step => step.untilAge != null && step.realGrowthPct != null));
  }

  calculate(): void {
    if (!this.canCalculate) {
      return;
    }
    this.calculating = true;

    this.salaryService.projectPension(this.buildInput()).subscribe({
      next: (data) => {
        this.projection = data;
        this.chart = this.buildChart(data);
        this.mobileView = 'projection';
        this.calculating = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.calculating = false;
        this.messageService.add({
          severity: 'error',
          detail: error.error?.error ?? 'Could not work out the pension.'
        });
        this.cdr.markForCheck();
      }
    });
  }

  downloadCsv(): void {
    if (!this.canCalculate) {
      return;
    }
    this.salaryService.downloadPensionCsv(this.buildInput()).subscribe({
      next: (blob) => this.saveBlob(blob),
      error: (error) => this.messageService.add({
        severity: 'error',
        detail: error.error?.error ?? 'Could not download the projection.'
      })
    });
  }

  showAssumptions(): void {
    this.mobileView = 'assumptions';
  }

  showProjection(): void {
    this.mobileView = 'projection';
  }

  addStep(): void {
    const last = this.steps[this.steps.length - 1];
    const nextAge = Math.min(this.retirementAge, (last?.untilAge ?? this.currentAge) + 5);
    this.steps = [...this.steps, { untilAge: nextAge, realGrowthPct: 0 }];
  }

  removeStep(index: number): void {
    this.steps = this.steps.filter((step, position) => position !== index);
  }

  scenarioColor(index: number): string {
    return SERIES_COLORS[index % SERIES_COLORS.length];
  }

  get headline(): string {
    const projection = this.projection;
    if (!projection) return '';
    if (!projection.eligible) return 'No state pension on these assumptions';

    const monthly = projection.scenarios[0]?.monthlyPension ?? 0;
    return money(monthly, projection.currency) + ' a month';
  }

  get yearsToRetirement(): number {
    const projection = this.projection;
    return projection ? Math.max(0, projection.retirementAge - projection.currentAge) : 0;
  }

  /** The share of final take-home the pension replaces is already a nought-to-a-hundred reading. */
  get replacementPct(): number {
    const rate = this.projection?.scenarios[0]?.replacementRatePct ?? 0;
    return Math.max(0, Math.min(100, rate));
  }

  get extraYearRows(): ExtraYearRow[] {
    const curve = this.projection?.scenarios[0]?.byStopAge ?? [];
    const rows: ExtraYearRow[] = [];

    for (let index = 1; index < curve.length; index++) {
      const gain = curve[index].monthlyPension - curve[index - 1].monthlyPension;
      rows.push({
        fromAge: curve[index - 1].stopAge,
        toAge: curve[index].stopAge,
        serviceYears: curve[index].serviceYears,
        monthlyPension: curve[index].monthlyPension,
        gain: gain
      });
    }
    return rows;
  }

  get timelineRows(): { age: number; year: number; working: boolean; grossMonthly: number }[] {
    const timeline = this.projection?.scenarios[0]?.timeline ?? [];
    return timeline.filter((row, index) =>
      index === 0 || index === timeline.length - 1 || row.age % 5 === 0 || !row.working);
  }

  private buildInput(): PensionProjectionInput {
    return {
      currentAge: this.currentAge,
      stopWorkingAge: this.stopWorkingAge,
      retirementAge: this.retirementAge,
      yearsAlreadyWorked: this.yearsAlreadyWorked,
      priorAverageGrossMonthly: this.priorAverageGrossMonthly,
      currentGrossMonthlyOverride: this.overrideGross ? this.customGross : null,
      realWageGrowth: this.realWageGrowth,
      inflation: this.inflation,
      degresszio: this.degresszio,
      includeThirteenthMonth: this.includeThirteenthMonth,
      scenarios: this.scenarioInputs()
    };
  }

  private scenarioInputs(): PensionScenarioInput[] {
    const scenarios: PensionScenarioInput[] = [];

    if (this.useFlat) {
      scenarios.push({ name: 'Frozen (real)', type: 'REAL_FLAT', realGrowthPct: null, steps: null });
    }
    if (this.useGrowth) {
      scenarios.push({ name: 'Rising', type: 'REAL_GROWTH', realGrowthPct: this.growthPct, steps: null });
    }
    if (this.useNominalLock) {
      scenarios.push({ name: 'Nominal lock', type: 'NOMINAL_LOCK', realGrowthPct: null, steps: null });
    }
    if (this.useSteps) {
      scenarios.push({ name: 'Stepped', type: 'STEPS', realGrowthPct: null, steps: this.steps });
    }
    return scenarios;
  }

  private loadSalary(): void {
    this.salaryService.getSalaries().subscribe({
      next: (salaries) => {
        this.applySalaries(salaries ?? []);
        this.salaryLoading = false;
        this.cdr.markForCheck();
        this.calculate();
      },
      error: () => {
        this.salaryLoading = false;
        this.cdr.markForCheck();
      }
    });
  }

  private applySalaries(salaries: { currencyId: string; validFrom: string; validTo: string | null; grossMonthly?: number }[]): void {
    const inForint = salaries.filter(salary => salary.currencyId === 'HUF');
    const current = inForint.find(salary => salary.validTo === null) ?? inForint[0];

    this.currentGrossMonthly = current?.grossMonthly ?? null;
    this.overrideGross = this.currentGrossMonthly == null;

    const years = salaries.map(salary => new Date(salary.validFrom).getFullYear());
    if (years.length > 0) {
      this.yearsAlreadyWorked = Math.max(0, new Date().getFullYear() - Math.min(...years));
    }
  }

  /**
   * One line per salary path against the age you stop working, so the curves can be read off each other.
   * The vertical marker is the age currently set, which is the point the headline figures come from.
   */
  private buildChart(projection: PensionProjection): Partial<PensionChartOptions> | null {
    const scenarios = projection.scenarios ?? [];
    if (scenarios.length === 0 || (scenarios[0]?.byStopAge ?? []).length < 2) {
      return null;
    }

    return {
      series: scenarios.map(scenario => ({
        name: scenario.name,
        data: scenario.byStopAge.map(point =>
          [point.stopAge, point.monthlyPension] as [number, number])
      })),
      chart: {
        type: 'line', height: CHART_HEIGHT, toolbar: { show: false }, zoom: { enabled: false },
        animations: { enabled: false }, fontFamily: 'Poppins, sans-serif'
      },
      colors: scenarios.map((scenario, index) => this.scenarioColor(index)),
      stroke: { width: 2.5, curve: 'straight' },
      grid: { borderColor: '#ece9e0', strokeDashArray: 4, padding: { left: 4, right: 16, top: 16 } },
      dataLabels: { enabled: false },
      markers: { size: 0, hover: { size: 5 } },
      annotations: { xaxis: [this.stopMarker(projection)] },
      xaxis: {
        type: 'numeric', tickAmount: 6, decimalsInFloat: 0,
        title: { text: 'Age you stop working', style: { fontWeight: 500, color: '#8a887f' } },
        labels: { formatter: (value: string) => Math.round(Number(value)).toString() },
        tooltip: { enabled: false }
      },
      yaxis: { labels: { formatter: (value: number) => compactAmount(value) } },
      legend: { position: 'bottom', markers: { strokeWidth: 0 } },
      tooltip: {
        shared: true,
        x: { formatter: (value: number) => 'Stop at ' + Math.round(value) },
        y: { formatter: (value: number) => money(value, projection.currency) }
      }
    };
  }

  private stopMarker(projection: PensionProjection): any {
    return {
      x: projection.stopWorkingAge,
      strokeDashArray: 4,
      borderColor: MARKER_COLOUR,
      label: {
        text: 'You stop',
        orientation: 'horizontal',
        position: 'top',
        offsetY: -2,
        borderColor: '#ece9e0',
        style: { background: '#ffffff', color: '#5f5e5a', fontSize: '11px' }
      }
    };
  }

  private saveBlob(blob: Blob): void {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'pension-projection.csv';
    link.click();
    window.URL.revokeObjectURL(url);
  }
}
