export interface BankTransaction {
    id: number;
    bookingDate: Date;
    bankTransactionId: string;
    type: string;
    accountNumber: string;
    accountName: string;
    partnerAccount: string;
    partnerName: string;
    amount: number;
    currencyId: string;
    memo: string;
    excluded: boolean;
    taxable: boolean;
}

export interface ImportResult {
    created: number;
    updated: number;
}

export interface MonthlyCashFlow {
    year: number;
    month: number;
    currencyId: string;
    moneyIn: number;
    moneyOut: number;
    net: number;
    savedPercent: number | null;
}

export interface MonthlyIncome {
    year: number;
    month: number;
    currencyId: string;
    source: string;
    amount: number;
    transactionCount: number;
}
