import { SalaryBasis } from './salary';

export type CompensationTaxTreatment = 'TAXED_AS_SALARY' | 'TAX_FREE' | 'RECEIVED_NET';

export type CompensationAmountMode = 'MONTHLY_AMOUNT' | 'PERCENT_OF_ANNUAL';

export interface CompensationItem {
  id?: number;
  name: string;
  amountMode: CompensationAmountMode;
  monthlyAmount: number | null;
  percent: number | null;
  taxTreatment: CompensationTaxTreatment;
  note: string | null;
  currencyId?: string;
  grossMonthly?: number | null;
  grossAnnual?: number | null;
  netMonthly?: number;
  netAnnual?: number;
  base?: boolean;
}

export interface CompensationPackage {
  label: string | null;
  currencyId: string;
  basis: SalaryBasis;
  dependents: number;
  validFrom: string | null;
  validTo: string | null;
  baseGrossMonthly: number;
  baseGrossAnnual: number;
  baseNetMonthly: number;
  baseNetAnnual: number;
  items: CompensationItem[];
  totalGrossMonthly: number;
  totalGrossAnnual: number;
  totalNetMonthly: number;
  totalNetAnnual: number;
}

export interface CompensationDraft {
  label: string | null;
  baseAmount: number | null;
  baseBasis: SalaryBasis;
  currencyId: string;
  dependents: number;
  items: CompensationItem[];
}
