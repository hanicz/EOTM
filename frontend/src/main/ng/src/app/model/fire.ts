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
    currentAge: number;
    retirementAge: number | null;
    lifeExpectancy: number;
}

export interface FireYear {
    year: number;
    age: number;
    phase: 'ACCUMULATION' | 'DRAWDOWN';
    contributions: number;
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

    depletedAtAge: number | null;
    lastsThroughRetirement: boolean;

    finalAge: number;
    finalBalance: number;
    finalRealBalance: number;
}
