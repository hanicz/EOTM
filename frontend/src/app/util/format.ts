const SCALES: { size: number; suffix: string }[] = [
  { size: 1, suffix: '' },
  { size: 1_000, suffix: 'k' },
  { size: 1_000_000, suffix: 'M' },
  { size: 1_000_000_000, suffix: 'B' }
];

export function compactAmount(value: number): string {
  const absolute = Math.abs(value);
  let index = 0;
  while (index < SCALES.length - 1 && absolute >= SCALES[index + 1].size) {
    index++;
  }

  let shown = scaled(absolute, index);
  if (shown >= 1_000 && index < SCALES.length - 1) {
    index++;
    shown = scaled(absolute, index);
  }

  const sign = value < 0 && shown !== 0 ? '-' : '';
  const digits = Number.isInteger(shown) ? `${shown}` : shown.toFixed(1);
  return `${sign}${digits}${SCALES[index].suffix}`;
}

function scaled(absolute: number, index: number): number {
  const value = absolute / SCALES[index].size;
  return index === 0 ? Math.round(value) : Math.round(value * 10) / 10;
}

export function money(value: number, currency: string): string {
  try {
    return new Intl.NumberFormat(undefined, {
      style: 'currency', currency: currency, maximumFractionDigits: 0
    }).format(value);
  } catch {
    return value.toFixed(0);
  }
}
