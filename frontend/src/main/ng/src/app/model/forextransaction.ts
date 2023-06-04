export interface ForexTransaction {
    forexTransactionId: number;
    fromAmount: number;
    toAmount: number;
    buySell: string;
    transactionDate: Date;
    fromCurrencyId: string;
    toCurrencyId: string;
    liveValue?: number;
    liveChangeRate?: number;
    changeRate: number;
}