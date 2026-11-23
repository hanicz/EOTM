export interface ForexWatch {
    forexWatchID: number;
    fromCurrencyId: string;
    liveValue: number;
    stalePrice?: boolean;
    toCurrencyId: string;
    change: number;
    pchange: number;
}