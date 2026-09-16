import { describe, expect, it } from 'vitest';
import { ALLOCATION_PALETTE, OTHER_LABEL, buildAllocation } from './allocation';
import { FALLBACK_ASSET_COLOUR } from './assetcolours';

describe('buildAllocation', () => {

  it('merges items with the same label and sorts the largest first', () => {
    const slices = buildAllocation([
      { label: 'AAA', value: 100 },
      { label: 'BBB', value: 300 },
      { label: 'AAA', value: 250 }
    ]);

    expect(slices.map(s => s.label)).toEqual(['AAA', 'BBB']);
    expect(slices[0].value).toBe(350);
    expect(slices[0].percentage).toBeCloseTo(53.846, 3);
  });

  it('drops empty, negative and missing values', () => {
    const slices = buildAllocation([
      { label: 'AAA', value: 0 },
      { label: 'BBB', value: -20 },
      { label: 'CCC', value: Number.NaN },
      { label: 'DDD', value: 40 }
    ]);

    expect(slices.map(s => s.label)).toEqual(['DDD']);
    expect(slices[0].percentage).toBe(100);
  });

  it('folds the tail into an Other slice', () => {
    const items = [50, 40, 30, 20, 10].map((value, index) => ({ label: `T${index}`, value }));
    const slices = buildAllocation(items, 3);

    expect(slices.map(s => s.label)).toEqual(['T0', 'T1', OTHER_LABEL]);
    expect(slices[2].value).toBe(60);
    expect(slices[2].color).toBe(FALLBACK_ASSET_COLOUR);
  });

  it('keeps every item when they fit', () => {
    const items = [30, 20, 10].map((value, index) => ({ label: `T${index}`, value }));

    expect(buildAllocation(items, 3).map(s => s.label)).toEqual(['T0', 'T1', 'T2']);
  });

  it('adds up to a full ring', () => {
    const items = [13, 29, 7, 51].map((value, index) => ({ label: `T${index}`, value }));
    const slices = buildAllocation(items);

    const total = slices.reduce((sum, s) => sum + s.percentage, 0);
    expect(total).toBeCloseTo(100, 6);
    const last = slices[slices.length - 1];
    const lastEnd = -Number(last.dashOffset) + Number(last.dashArray.split(' ')[0]);
    expect(lastEnd).toBeCloseTo(2 * Math.PI * 40, 6);
  });

  it('colours by rank unless a colour is given', () => {
    const slices = buildAllocation([
      { label: 'AAA', value: 10 },
      { label: 'BBB', value: 20 }
    ], 8, { AAA: '#123456' });

    expect(slices[0].color).toBe(ALLOCATION_PALETTE[0]);
    expect(slices[1].color).toBe('#123456');
  });

  it('returns nothing when there is no worth', () => {
    expect(buildAllocation([])).toEqual([]);
    expect(buildAllocation([{ label: 'AAA', value: 0 }])).toEqual([]);
  });
});
