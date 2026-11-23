import { FALLBACK_ASSET_COLOUR } from './assetcolours';

export interface AllocationItem {
  label: string;
  value: number;
}

export interface AllocationSlice {
  label: string;
  value: number;
  percentage: number;
  color: string;
  dashArray: string;
  dashOffset: string;
}

export const ALLOCATION_PALETTE: string[] = [
  '#ef9f27',
  '#1b1b1b',
  '#7a8c5c',
  '#5c7a8c',
  '#a35c4a',
  '#8c6f9e',
  '#4f8a7e',
  '#5f5e5a',
];

export const OTHER_LABEL = 'Other';
export const DONUT_RADIUS = 40;

export function buildAllocation(items: AllocationItem[], maxSlices = 8,
  colors: { [label: string]: string } = {}): AllocationSlice[] {

  const merged = new Map<string, number>();
  items.forEach(item => {
    if (!(item.value > 0)) {
      return;
    }
    merged.set(item.label, (merged.get(item.label) ?? 0) + item.value);
  });

  let ranked = Array.from(merged, ([label, value]) => ({ label, value }))
    .sort((left, right) => right.value - left.value);

  const limit = Math.max(maxSlices, 1);
  let other = 0;
  if (ranked.length > limit) {
    other = ranked.slice(limit - 1).reduce((sum, item) => sum + item.value, 0);
    ranked = ranked.slice(0, limit - 1);
  }

  const total = ranked.reduce((sum, item) => sum + item.value, 0) + other;
  if (total <= 0) {
    return [];
  }

  const coloured = ranked.map((item, index) => ({
    ...item,
    color: colors[item.label] ?? ALLOCATION_PALETTE[index % ALLOCATION_PALETTE.length]
  }));
  if (other > 0) {
    coloured.push({ label: OTHER_LABEL, value: other, color: colors[OTHER_LABEL] ?? FALLBACK_ASSET_COLOUR });
  }

  const circumference = 2 * Math.PI * DONUT_RADIUS;
  let offset = 0;
  return coloured.map(item => {
    const percentage = item.value / total * 100;
    const length = percentage / 100 * circumference;
    const slice: AllocationSlice = {
      ...item,
      percentage,
      dashArray: `${length} ${circumference - length}`,
      dashOffset: `${-offset}`,
    };
    offset += length;
    return slice;
  });
}
