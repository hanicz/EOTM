export interface BankTransaction {
    id: number;
    bookingDate: string;
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
    categoryId: number | null;
    categoryName: string | null;
    categoryColor: CategoryColor | null;
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

export interface YearlyCashFlow {
    year: number;
    currencyId: string;
    moneyIn: number;
    moneyOut: number;
    monthsCounted: number;
    net: number;
    savedPercent: number | null;
    averageMonthlySpending: number | null;
}

export interface MonthlyIncome {
    year: number;
    month: number;
    currencyId: string;
    source: string;
    amount: number;
    transactionCount: number;
}

export type AccountSide = 'OWN_ACCOUNT' | 'PARTNER_ACCOUNT' | 'ANY';

export interface ExclusionRule {
    id: number;
    name: string | null;
    accountNumber: string;
    side: AccountSide;
    active: boolean;
}

export interface ExclusionRuleRequest {
    accountNumber: string;
    seq: number;
}

export type CategoryColor = 'BLUE' | 'ORANGE' | 'AQUA' | 'YELLOW' | 'MAGENTA' | 'GREEN' | 'VIOLET' | 'RED';

export const CATEGORY_CHIP_NONE = 'cat-chip-none';

export const CATEGORY_CHIP_CLASS: Record<CategoryColor, string> = {
    BLUE: 'cat-chip-blue',
    ORANGE: 'cat-chip-orange',
    AQUA: 'cat-chip-aqua',
    YELLOW: 'cat-chip-yellow',
    MAGENTA: 'cat-chip-magenta',
    GREEN: 'cat-chip-green',
    VIOLET: 'cat-chip-violet',
    RED: 'cat-chip-red'
};

export const CATEGORY_HEX: Record<CategoryColor, string> = {
    BLUE: '#2a78d6',
    ORANGE: '#eb6834',
    AQUA: '#1baf7a',
    YELLOW: '#eda100',
    MAGENTA: '#e87ba4',
    GREEN: '#008300',
    VIOLET: '#4a3aa7',
    RED: '#e34948'
};

export const CATEGORY_COLORS: { label: string, value: CategoryColor }[] = [
    { label: 'Blue', value: 'BLUE' },
    { label: 'Orange', value: 'ORANGE' },
    { label: 'Aqua', value: 'AQUA' },
    { label: 'Yellow', value: 'YELLOW' },
    { label: 'Magenta', value: 'MAGENTA' },
    { label: 'Green', value: 'GREEN' },
    { label: 'Violet', value: 'VIOLET' },
    { label: 'Red', value: 'RED' }
];

export interface SpendingCategory {
    id: number;
    name: string;
    color: CategoryColor;
    position: number;
}

export interface CategoryRule {
    id: number;
    name: string | null;
    pattern: string;
    categoryId: number;
    categoryName: string;
    categoryColor: CategoryColor;
    priority: number;
    active: boolean;
}

export interface CategorizeResult {
    categorized: number;
    cleared: number;
}

export interface MonthlyCategorySpending {
    year: number;
    month: number;
    currencyId: string;
    categoryId: number | null;
    categoryName: string;
    categoryColor: CategoryColor;
    amount: number;
    transactionCount: number;
}
