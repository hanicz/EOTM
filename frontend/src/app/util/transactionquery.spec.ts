import { describe, expect, it } from 'vitest';
import { DEFAULT_PAGING, lastPageStart, TransactionFilters, transactionParams } from './transactionquery';

const NO_FILTERS: TransactionFilters = { search: '', flag: null, categoryId: null, from: '', to: '' };

describe('transactionParams', () => {

  it('sends only paging and sorting when nothing is filtered', () => {
    expect(transactionParams(NO_FILTERS, DEFAULT_PAGING))
      .toEqual({ page: '0', size: '25', sort: 'bookingDate', dir: 'desc' });
  });

  it('turns the first row into a page number', () => {
    const params = transactionParams(NO_FILTERS, { first: 100, rows: 50, sortField: 'amount', sortOrder: 1 });

    expect(params['page']).toBe('2');
    expect(params['size']).toBe('50');
    expect(params['sort']).toBe('amount');
    expect(params['dir']).toBe('asc');
  });

  it('adds every filter that is set and trims the search', () => {
    const filters: TransactionFilters = {
      search: '  blue mart ', flag: 'COUNTED', categoryId: 3, from: '2025-12-01', to: '2025-12-31'
    };

    expect(transactionParams(filters, DEFAULT_PAGING)).toMatchObject({
      search: 'blue mart', flag: 'COUNTED', categoryId: '3', from: '2025-12-01', to: '2025-12-31'
    });
  });

  it('keeps the uncategorized category, which is zero', () => {
    expect(transactionParams({ ...NO_FILTERS, categoryId: 0 }, DEFAULT_PAGING)['categoryId']).toBe('0');
  });

  it('leaves out a blank search', () => {
    expect(transactionParams({ ...NO_FILTERS, search: '   ' }, DEFAULT_PAGING)['search']).toBeUndefined();
  });
});

describe('lastPageStart', () => {

  it('points at the first row of the last page', () => {
    expect(lastPageStart(51, 25)).toBe(50);
    expect(lastPageStart(50, 25)).toBe(25);
  });

  it('is zero when there is nothing left', () => {
    expect(lastPageStart(0, 25)).toBe(0);
  });
});
