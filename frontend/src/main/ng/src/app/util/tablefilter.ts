export interface PortfolioFilter {
  search: string;
  account: string | null;
}

export const EMPTY_FILTER: PortfolioFilter = { search: '', account: null };

/**
 * One filter drives every tab on a page, but not every tab has an account to match on. Passing no
 * account field leaves those rows untouched rather than emptying the table.
 */
export function filterRows<T>(rows: T[], filter: PortfolioFilter, searchFields: (keyof T)[],
                              accountField?: keyof T): T[] {
  const term = (filter?.search ?? '').trim().toLowerCase();
  const account = filter?.account ?? null;
  if (!term && !account) return rows;

  return rows.filter(row => {
    if (account && accountField && String(row[accountField] ?? '') !== account) return false;
    if (!term) return true;
    return searchFields.some(field => String(row[field] ?? '').toLowerCase().includes(term));
  });
}
