import { describe, expect, it } from 'vitest';
import { EMPTY_FILTER, filterRows } from './tablefilter';

interface Row {
  shortName: string;
  name: string;
  accountName?: string;
}

const rows: Row[] = [
  { shortName: 'AAA', name: 'Alpha Fund', accountName: 'Main' },
  { shortName: 'BBB', name: 'Beta Trust', accountName: 'Spare' },
  { shortName: 'CCC', name: 'Alpha Reserve', accountName: 'Main' }
];

const fields: (keyof Row)[] = ['shortName', 'name'];

describe('filterRows', () => {

  it('returns the same array when nothing is filtered', () => {
    expect(filterRows(rows, EMPTY_FILTER, fields, 'accountName')).toBe(rows);
  });

  it('matches on any of the search fields', () => {
    const byId = filterRows(rows, { search: 'bbb', account: null }, fields, 'accountName');
    expect(byId.map(row => row.shortName)).toEqual(['BBB']);

    const byName = filterRows(rows, { search: 'alpha', account: null }, fields, 'accountName');
    expect(byName.map(row => row.shortName)).toEqual(['AAA', 'CCC']);
  });

  it('ignores case and surrounding spaces', () => {
    const found = filterRows(rows, { search: '  BeTa  ', account: null }, fields, 'accountName');
    expect(found.map(row => row.shortName)).toEqual(['BBB']);
  });

  it('narrows to one account', () => {
    const found = filterRows(rows, { search: '', account: 'Main' }, fields, 'accountName');
    expect(found.map(row => row.shortName)).toEqual(['AAA', 'CCC']);
  });

  it('applies the search and the account together', () => {
    const found = filterRows(rows, { search: 'alpha', account: 'Spare' }, fields, 'accountName');
    expect(found).toEqual([]);
  });

  it('leaves rows alone when the tab has no account field', () => {
    const found = filterRows(rows, { search: '', account: 'Main' }, fields);
    expect(found).toEqual(rows);
  });

  it('survives a missing filter', () => {
    expect(filterRows(rows, undefined as any, fields, 'accountName')).toBe(rows);
  });

  it('does not match rows whose field is missing', () => {
    const sparse: Row[] = [{ shortName: 'DDD', name: '' }];
    expect(filterRows(sparse, { search: 'alpha', account: null }, fields)).toEqual([]);
  });
});
