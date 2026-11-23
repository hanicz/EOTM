export interface Recommendation {
    buy: number;
    symbol: string;
    sell: number;
    hold: number;
    strongSell: number;
    strongBuy: number;
    period: Date;
}