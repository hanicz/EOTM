export interface ETFDividend {
    id: number;
    dividendDate: Date;
    shortName: string;
    name: string;
    exchange: string;
    amount: number;
    currencyId: string;
}