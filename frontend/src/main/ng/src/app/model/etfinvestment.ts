export interface ETFInvestment {
    id: number;
    quantity: number;
    buySell: string;
    transactionDate: Date;
    shortName: string;
    name: string;
    exchange: string;
    amount: number;
    currencyId: string;
    liveValue?: number;
    stalePrice?: boolean;
    valueDiff?: number;
    fee: number;
    eodDate: Date;
    accountId: number;
    accountName: string;
}