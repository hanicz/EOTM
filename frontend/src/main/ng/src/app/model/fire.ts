export interface FireProjectionInput {
    currency: string;
    otherAssets: number;
    monthlyContribution: number;
    annualContributionIncrease: number;
    annualReturn: number;
    inflation: number;
    annualSpending: number | null;
    withdrawalRate: number;
    fireNumber: number | null;
    monthlyPension: number | null;
    pensionAge: number | null;
    unemploymentBenefit: number | null;
    currentAge: number;
    retirementAge: number | null;
    lifeExpectancy: number;
}

export interface FireYear {
    year: number;
    age: number;
    phase: 'ACCUMULATION' | 'DRAWDOWN';
    contributions: number;
    growth: number;
    pension: number;
    withdrawals: number;
    balance: number;
    realBalance: number;
    pctOfFireNumber: number;
}

export interface FireProjection {
    currency: string;

    portfolioValue: number;
    otherAssets: number;
    startingValue: number;
    unconvertedCurrencies: string[];

    timeline: FireYear[];
    milestones: FireYear[];

    fireNumber: number;
    annualSpending: number;
    withdrawalRate: number;
    fireNumberOverridden: boolean;
    fireNumberInTodaysMoney: boolean;
    firstYearWithdrawal: number;

    fiReached: boolean;
    fiYear: number | null;
    fiAge: number | null;

    retirementYear: number | null;
    retirementAge: number | null;

    pensionYear: number | null;

    depletedAtAge: number | null;
    lastsThroughRetirement: boolean;

    finalAge: number;
    finalBalance: number;
    finalRealBalance: number;
}

export interface FireSnapshot {
    currency: string;

    netWorth: number;
    fireNumber: number | null;
    targetSource: 'FIXED' | 'DERIVED' | null;
    progressPct: number | null;
    yearsToFire: number | null;
    fiReached: boolean;

    monthlyIncome: number;
    monthlySpending: number;
    monthlySavings: number;
    savingsRatePct: number | null;

    withdrawalRate: number;
    annualReturn: number;
    annualContributionIncrease: number;
    inflation: number;
    horizonYears: number;

    hasCashFlow: boolean;
    monthsCounted: number;
    windowStart: string | null;
    windowEnd: string | null;

    ignoredCurrencies: string[];
    unconvertedCurrencies: string[];
}
