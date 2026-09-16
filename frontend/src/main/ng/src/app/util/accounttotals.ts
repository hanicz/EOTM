export interface AccountTotals {
  accountName: string;
  spent: number;
  worth: number;
  todayDiff: number;
  todayPercentage: number;
  diff: number;
  percentage: number;
}

export interface AccountTotalsRow {
  accountName?: string;
  amount: number;
  currencyId: string;
  liveValue?: number;
  dayChange?: number;
}

export function calculateAccountTotals(rows: AccountTotalsRow[],
  convert: (amount: number, currency: string) => number): AccountTotals[] {

  const totals = new Map<string, AccountTotals>();

  rows.forEach(row => {
    if (!row.accountName) {
      return;
    }

    let account = totals.get(row.accountName);
    if (account === undefined) {
      account = {
        accountName: row.accountName,
        spent: 0, worth: 0, todayDiff: 0, todayPercentage: 0, diff: 0, percentage: 0
      };
      totals.set(row.accountName, account);
    }

    const worth = row.liveValue != undefined ? row.liveValue : row.amount;
    account.spent += convert(row.amount, row.currencyId);
    account.worth += convert(worth, row.currencyId);
    account.todayDiff += convert(row.dayChange ?? 0, row.currencyId);
  });

  return Array.from(totals.values())
    .map(account => {
      const previousWorth = account.worth - account.todayDiff;
      return {
        ...account,
        diff: account.worth - account.spent,
        percentage: account.spent !== 0 ? (account.worth - account.spent) / account.spent * 100 : 0,
        todayPercentage: previousWorth !== 0 ? account.todayDiff / previousWorth * 100 : 0
      };
    })
    .sort((left, right) => left.accountName < right.accountName ? -1 : left.accountName > right.accountName ? 1 : 0);
}
