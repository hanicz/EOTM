export interface StockWatch {
    tickerWatchId: number;
    stockName: string;
    stockShortName: string;
    liveValue: number;
    stalePrice?: boolean;
    currencyId: string;
    change: number;
    pchange: number;
    stockExchange: string;
    groupId: number | null;
    groupName: string | null;
}
