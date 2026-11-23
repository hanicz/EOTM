export interface EtfWeight {
    name: string;
    isin?: string;
    weight: number | null;
}

export interface EtfProfile {
    isin: string;
    provider: string | null;
    indexName: string | null;
    investmentFocus: string | null;
    fundSize: string | null;
    ter: string | null;
    replication: string | null;
    distributionPolicy: string | null;
    fundCurrency: string | null;
    currencyHedge: string | null;
    inceptionDate: string | null;
    volatility: string | null;
    totalHoldings: number | null;
    holdings: EtfWeight[];
    countries: EtfWeight[];
    sectors: EtfWeight[];
}
