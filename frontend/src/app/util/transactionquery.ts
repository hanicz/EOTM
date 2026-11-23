export interface TransactionFilters {
  search: string;
  flag: string | null;
  categoryId: number | null;
  from: string;
  to: string;
}

export interface TransactionPaging {
  first: number;
  rows: number;
  sortField: string;
  sortOrder: number;
}

export const DEFAULT_PAGING: TransactionPaging = { first: 0, rows: 25, sortField: 'bookingDate', sortOrder: -1 };

export function transactionParams(filters: TransactionFilters, paging: TransactionPaging): Record<string, string> {
  const params: Record<string, string> = {
    page: String(Math.floor(paging.first / paging.rows)),
    size: String(paging.rows),
    sort: paging.sortField,
    dir: paging.sortOrder === 1 ? 'asc' : 'desc'
  };
  const search = filters.search.trim();
  if (search) params['search'] = search;
  if (filters.flag) params['flag'] = filters.flag;
  if (filters.categoryId !== null) params['categoryId'] = String(filters.categoryId);
  if (filters.from) params['from'] = filters.from;
  if (filters.to) params['to'] = filters.to;
  return params;
}

export function lastPageStart(total: number, rows: number): number {
  return total > 0 ? Math.floor((total - 1) / rows) * rows : 0;
}
