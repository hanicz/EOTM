export interface Transaction {
    transactionId: number;
    quantity: number;
    buySell: string;
    transactionDate: Date;
    symbol: string;
    transactionString?: string;
    amount: number;
    currencyId: string;
    liveValue?: number;
    valueDiff?: number;
    fee: number;
}