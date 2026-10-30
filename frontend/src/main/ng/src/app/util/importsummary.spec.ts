import { describe, expect, it } from 'vitest';
import { describeImport } from './importsummary';

describe('describeImport', () => {
  it('reports a reimport of the same file as unchanged', () => {
    expect(describeImport({ created: 0, updated: 0, unchanged: 6 })).toBe('Import finished. 0 added, 6 unchanged.');
  });

  it('lists every non-zero count', () => {
    expect(describeImport({ created: 2, updated: 1, unchanged: 3 }))
      .toBe('Import finished. 2 added, 1 updated, 3 unchanged.');
  });

  it('always shows the added count and appends the suffix', () => {
    expect(describeImport({ created: 0, updated: 0, unchanged: 0 }, ' from 2 files'))
      .toBe('Import finished. 0 added from 2 files.');
  });
});
