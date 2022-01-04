export interface Investment {
    investmentId: number;
    quantity: number;
    buySell: string;
    transactionDate: Date;
    shortName: string;
    amount: number;
    currencyId: string;
    liveValue?: number;
    valueDiff?: number;
    fee: number;
}