export interface AssetClassValue {
    assetClass: string;
    spent: number;
    worth: number;
    changePct: number;
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
