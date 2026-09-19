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
  DegressioMode,
  PensionProjection,
  PensionProjectionInput,
  PensionScenarioInput,
  PensionStep
} from '../../model/pension-projection';
import { SalaryService } from '../../service/salary.service';
import { DeltaComponent } from '../../util/delta.component';

interface SeriesPoint {
  x: number;
  y: number;
  value: number;
}

interface ChartSeries {
  name: string;
  color: string;
  path: string;
  labelY: number;
  labelValue: number;
}

interface ChartColumn {
  stopAge: number;
  x: number;
  serviceYears: number;
  readings: { name: string; color: string; value: number; y: number }[];
}

interface AxisTick {
  position: number;
  label: string;
}

interface Chart {
  series: ChartSeries[];
  columns: ChartColumn[];
  xTicks: AxisTick[];
  yTicks: AxisTick[];
  stopX: number | null;
  firstAge: number;
  lastAge: number;
}

interface ExtraYearRow {
  fromAge: number;
  toAge: number;
  serviceYears: number;
  monthlyPension: number;
  gain: number;
}

const CHART = { width: 820, height: 340, left: 78, right: 104, top: 16, bottom: 30 };

const SERIES_COLORS = ['#a85f00', '#0f8a7a', '#4a7fd4', '#c0407a'];

const LABEL_GAP = 13;

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
        Select, Toast, Skeleton, FormsModule, DecimalPipe, DeltaComponent]
})
export class SalaryPensionComponent {

  readonly chartBox = CHART;

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
  chart: Chart | null = null;
  salaryLoading: boolean = true;
  calculating: boolean = false;
  hoveredStopAge: number | null = null;

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
        this.hoveredStopAge = null;
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

  onChartMove(event: MouseEvent): void {
    if (!this.chart) {
      return;
    }
    const bounds = (event.currentTarget as SVGSVGElement).getBoundingClientRect();
    if (!bounds.width) {
      return;
    }
    const svgX = ((event.clientX - bounds.left) / bounds.width) * CHART.width;
    const plotWidth = CHART.width - CHART.left - CHART.right;
    const span = Math.max(1, this.chart.lastAge - this.chart.firstAge);
    const age = Math.round(this.chart.firstAge + ((svgX - CHART.left) / plotWidth) * span);

    this.hoveredStopAge = (age < this.chart.firstAge || age > this.chart.lastAge) ? null : age;
  }

  setHoveredStopAge(age: number | null): void {
    this.hoveredStopAge = age;
  }

  get hoveredColumn(): ChartColumn | null {
    if (this.hoveredStopAge == null) {
      return null;
    }
    return this.chart?.columns.find(column => column.stopAge === this.hoveredStopAge) ?? null;
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

  private buildChart(projection: PensionProjection): Chart | null {
    const scenarios = projection.scenarios ?? [];
    const curve = scenarios[0]?.byStopAge ?? [];
    if (scenarios.length === 0 || curve.length < 2) {
      return null;
    }

    const plotWidth = CHART.width - CHART.left - CHART.right;
    const plotHeight = CHART.height - CHART.top - CHART.bottom;
    const firstAge = curve[0].stopAge;
    const lastAge = curve[curve.length - 1].stopAge;
    const span = Math.max(1, lastAge - firstAge);

    const peak = Math.max(
      ...scenarios.flatMap(scenario => scenario.byStopAge.map(point => point.monthlyPension)), 1);
    const top = this.niceCeiling(peak);

    const xFor = (age: number) => CHART.left + ((age - firstAge) / span) * plotWidth;
    const yFor = (value: number) => CHART.top + plotHeight - (value / top) * plotHeight;

    const series: ChartSeries[] = scenarios.map((scenario, index) => {
      const points = scenario.byStopAge.map(point => ({
        x: xFor(point.stopAge),
        y: yFor(point.monthlyPension),
        value: point.monthlyPension
      }));
      const last = points[points.length - 1];
      return {
        name: scenario.name,
        color: this.scenarioColor(index),
        path: this.toPath(points),
        labelY: last.y,
        labelValue: last.value
      };
    });

    this.spreadLabels(series);

    const columns: ChartColumn[] = curve.map((point, index) => ({
      stopAge: point.stopAge,
      x: xFor(point.stopAge),
      serviceYears: point.serviceYears,
      readings: scenarios.map((scenario, position) => ({
        name: scenario.name,
        color: this.scenarioColor(position),
        value: scenario.byStopAge[index]?.monthlyPension ?? 0,
        y: yFor(scenario.byStopAge[index]?.monthlyPension ?? 0)
      }))
    }));

    return {
      series: series,
      columns: columns,
      xTicks: this.xTicks(firstAge, lastAge, xFor),
      yTicks: this.yTicks(top, yFor),
      stopX: (projection.stopWorkingAge >= firstAge && projection.stopWorkingAge <= lastAge)
        ? xFor(projection.stopWorkingAge) : null,
      firstAge: firstAge,
      lastAge: lastAge
    };
  }

  private spreadLabels(series: ChartSeries[]): void {
    const ordered = [...series].sort((left, right) => left.labelY - right.labelY);
    for (let index = 1; index < ordered.length; index++) {
      const minimum = ordered[index - 1].labelY + LABEL_GAP;
      if (ordered[index].labelY < minimum) {
        ordered[index].labelY = minimum;
      }
    }
  }

  private toPath(points: SeriesPoint[]): string {
    return points.map((point, index) =>
      `${index === 0 ? 'M' : 'L'}${point.x.toFixed(2)},${point.y.toFixed(2)}`).join(' ');
  }

  private xTicks(firstAge: number, lastAge: number, xFor: (age: number) => number): AxisTick[] {
    const step = Math.max(1, Math.round((lastAge - firstAge) / 6));
    const ticks: AxisTick[] = [];
    for (let age = firstAge; age <= lastAge; age += step) {
      ticks.push({ position: xFor(age), label: `${age}` });
    }
    return ticks;
  }

  private yTicks(top: number, yFor: (value: number) => number): AxisTick[] {
    const ticks: AxisTick[] = [];
    for (let index = 0; index <= 4; index++) {
      const value = (top / 4) * index;
      ticks.push({ position: yFor(value), label: this.formatCompact(value) });
    }
    return ticks;
  }

  private niceCeiling(value: number): number {
    const magnitude = Math.pow(10, Math.floor(Math.log10(value)));
    const normalised = value / magnitude;
    const rounded = normalised <= 1 ? 1 : normalised <= 2 ? 2 : normalised <= 5 ? 5 : 10;
    return rounded * magnitude;
  }

  formatCompact(value: number): string {
    return new Intl.NumberFormat(undefined, { notation: 'compact', maximumFractionDigits: 1 }).format(value);
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
