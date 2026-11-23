export interface Investment {
    investmentId: number;
    quantity: number;
    buySell: string;
    transactionDate: Date;
    shortName: string;
    amount: number;
    currencyId: string;
    liveValue?: number;
    stalePrice?: boolean;
    valueDiff?: number;
    dayChange?: number;
    dayChangePercent?: number;
    fee: number;
    exchange: string;
    name: string;
    accountId: number;
    accountName: string;
    rsu: boolean;
}