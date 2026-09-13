export interface AssetClassValue {
    assetClass: string;
    spent: number;
    worth: number;
    changePct: number;
    expectedRatePct?: number;
}

export interface NetWorthPoint {
    date: string;
    totalSpent: number;
    totalWorth: number;
    assetWorth: { [assetClass: string]: number };
}

export interface MonthlyPerformance {
    month: string;
    endWorth: number;
    endSpent: number;
    change: number;
    changePct: number | null;
    contributions: number;
    marketGain: number;
}

export interface NetWorthHistory {
    currency: string;
    points: NetWorthPoint[];
    months: MonthlyPerformance[];
}

export interface NetWorth {
    currency: string;
    totalSpent: number;
    totalWorth: number;
    totalChangePct: number;
    assets: AssetClassValue[];
    availableCurrencies: string[];
    unconvertedCurrencies: string[];
}
