export interface ForexTransaction {
    forexTransactionId: number;
    fromAmount: number;
    toAmount: number;
    buySell: string;
    transactionDate: Date;
    fromCurrencyId: string;
    toCurrencyId: string;
    liveValue?: number;
    stalePrice?: boolean;
    liveChangeRate?: number;
    changeRate: number;
}