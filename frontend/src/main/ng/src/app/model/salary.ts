export type SalaryBasis = 'MONTHLY' | 'ANNUAL';

export interface Salary {
  id?: number;
  amount: number | null;
  basis: SalaryBasis;
  currencyId: string;
  validFrom: string;
  validTo: string | null;
  dependents: number;
  note: string | null;
  grossMonthly?: number;
  grossAnnual?: number;
  netMonthly?: number;
  netAnnual?: number;
  szjaMonthly?: number;
  szjaAnnual?: number;
  tbMonthly?: number;
  tbAnnual?: number;
  familyAllowanceMonthly?: number;
  familyAllowanceApplied?: boolean;
  raiseAmount?: number | null;
  raisePercent?: number | null;
}
