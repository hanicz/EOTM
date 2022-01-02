export interface ForexWatch {
    forexWatchID: number;
    fromCurrencyId: string;
    liveValue: number;
    toCurrencyId: string;
    change: number;
    pchange: number;
}