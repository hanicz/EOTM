export interface StockWatch {
    tickerWatchId: number;
    stockName: string;
    stockShortName: string;
    liveValue: number;
    currencyId: string;
    change: number;
    pchange: number;
}