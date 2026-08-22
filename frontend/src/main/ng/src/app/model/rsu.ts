export interface RSU {
    shortName: string;
    exchange?: string;
    currency?: string;
    date: string;
    quantity: number;
}

export interface TaxBreakdown {
    amount: number;
    taxBase: number;
    szocho: number;
    szja: number;
    total: number;
}

export interface RSUTax {
    shortName: string;
    exchange: string;
    date: string;
    quantity: number;
    currency: string;
    price: number;
    priceDate: string;
    amount: number;
    rate: number;
    rateDate: string;
    amountInHuf: number;
    tax: TaxBreakdown;
}

export interface TaxReport {
    items: RSUTax[];
    totalAmountInHuf: number;
    totalTax: TaxBreakdown;
}

export interface TaxableEvent {
    id: number;
    bookingDate: string;
    type: string;
    partnerName: string;
    memo: string;
    amount: number;
    currencyId: string;
    rate: number;
    rateDate: string;
    amountInHuf: number;
    calculatedOn: string;
    tax: TaxBreakdown;
    paid: boolean;
}

export interface TaxableEventReport {
    items: TaxableEvent[];
    totalAmountInHuf: number;
    totalTax: TaxBreakdown;
}
