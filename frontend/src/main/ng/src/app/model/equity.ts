import { TaxBreakdown } from './rsu';

export type VestingFrequency = 'ANNUAL' | 'QUARTERLY';

export interface RSUVest {
    grantId: number;
    shortName: string;
    exchange: string;
    grantDate: string;
    sequence: number;
    vestDate: string;
    quantity: number;
    currency: string | null;
    price: number | null;
    priceDate: string | null;
    amount: number;
    rate: number | null;
    rateDate: string | null;
    amountInHuf: number;
    netInHuf: number;
    vested: boolean;
    projected: boolean;
    tax: TaxBreakdown;
}

export interface RSUGrant {
    id?: number;
    shortName: string;
    exchange: string;
    currency: string | null;
    grantDate: string;
    quantity: number | null;
    vestingYears: number;
    vestingFrequency: VestingFrequency;
    note: string | null;
    vests?: RSUVest[];
    totalAmountInHuf?: number;
    totalTax?: TaxBreakdown;
    totalNetInHuf?: number;
    vestedNetInHuf?: number;
    upcomingNetInHuf?: number;
    error?: string | null;
}

export interface RSUGrantReport {
    items: RSUGrant[];
    totalAmountInHuf: number;
    totalTax: TaxBreakdown;
    totalNetInHuf: number;
}

export interface STARVest {
    grantId: number;
    name: string;
    commencementDate: string;
    sequence: number;
    vestDate: string;
    quantity: number;
    cliff: boolean;
    currency: string;
    spreadPerUnit: number;
    amount: number;
    rate: number | null;
    rateDate: string | null;
    amountInHuf: number;
    netInHuf: number;
    vested: boolean;
    projected: boolean;
    tax: TaxBreakdown;
}

export interface STARGrant {
    id?: number;
    name: string;
    currency: string;
    commencementDate: string;
    quantity: number | null;
    baseValue: number | null;
    currentValue: number | null;
    vestingYears: number;
    note: string | null;
    applyValueToAll?: boolean;
    spreadPerUnit?: number;
    vests?: STARVest[];
    totalAmountInHuf?: number;
    totalTax?: TaxBreakdown;
    totalNetInHuf?: number;
    vestedNetInHuf?: number;
    upcomingNetInHuf?: number;
    error?: string | null;
}

export interface STARGrantReport {
    items: STARGrant[];
    totalAmountInHuf: number;
    totalTax: TaxBreakdown;
    totalNetInHuf: number;
}
