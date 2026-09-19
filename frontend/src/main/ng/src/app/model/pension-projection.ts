export type PensionScenarioType = 'REAL_FLAT' | 'REAL_GROWTH' | 'NOMINAL_LOCK' | 'STEPS';

export type DegressioMode = 'INDEXED' | 'FROZEN' | 'IGNORED';

export interface PensionStep {
  untilAge: number | null;
  realGrowthPct: number | null;
}

export interface PensionScenarioInput {
  name: string;
  type: PensionScenarioType;
  realGrowthPct: number | null;
  steps: PensionStep[] | null;
}

export interface PensionProjectionInput {
  currentAge: number;
  stopWorkingAge: number;
  retirementAge: number;
  yearsAlreadyWorked: number;
  priorAverageGrossMonthly: number | null;
  currentGrossMonthlyOverride: number | null;
  realWageGrowth: number;
  inflation: number;
  degresszio: DegressioMode;
  includeThirteenthMonth: boolean;
  scenarios: PensionScenarioInput[];
}

export interface PensionStopAgePoint {
  stopAge: number;
  serviceYears: number;
  scalePct: number;
  monthlyPension: number;
}

export interface PensionYear {
  scenario: string;
  year: number;
  age: number;
  working: boolean;
  counted: boolean;
  grossMonthly: number;
  netMonthly: number;
  valorizedNetMonthly: number;
}

export interface PensionScenarioResult {
  name: string;
  type: PensionScenarioType;
  pensionBaseMonthly: number;
  degressedBaseMonthly: number;
  monthlyPension: number;
  annualPension: number;
  finalGrossMonthly: number;
  finalNetMonthly: number;
  replacementRatePct: number;
  minimumApplied: boolean;
  byStopAge: PensionStopAgePoint[];
  timeline: PensionYear[];
}

export interface PensionSensitivityPoint {
  realWageGrowth: number;
  valorizationUplift: number;
  monthlyPension: number;
  selected: boolean;
}

export interface PensionProjection {
  currency: string;
  currentAge: number;
  stopWorkingAge: number;
  retirementAge: number;
  retirementYear: number;
  gapYears: number;
  serviceYears: number;
  scalePct: number;
  eligible: boolean;
  partialPension: boolean;
  degresszio: DegressioMode;
  degressioLowerThreshold: number;
  degressioUpperThreshold: number;
  currentGrossMonthly: number;
  realWageGrowth: number;
  valorizationUplift: number;
  wageGrowthSensitivity: PensionSensitivityPoint[];
  nationalAverageGrossMonthly: number;
  nationalAveragePension: number;
  nationalAverageMultiple: number;
  salaryMultipleOfNationalAverage: number;
  scenarios: PensionScenarioResult[];
  warnings: string[];
}
