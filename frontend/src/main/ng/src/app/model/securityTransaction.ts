export interface SecurityTransaction {
    transactionId: number;
    quantity: number;
    buySell: string;
    transactionDate: Date;
    securityId: string;
    securityName: string;
    amount: number;
    currencyId: string;
}
