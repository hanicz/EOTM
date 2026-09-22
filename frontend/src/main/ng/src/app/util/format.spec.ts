import { describe, expect, it } from 'vitest';
import { compactAmount } from './format';

describe('compactAmount', () => {
  it('keeps small amounts whole', () => {
    expect(compactAmount(0)).toBe('0');
    expect(compactAmount(950)).toBe('950');
    expect(compactAmount(999.4)).toBe('999');
  });

  it('moves up a unit when rounding reaches a thousand', () => {
    expect(compactAmount(999.6)).toBe('1k');
    expect(compactAmount(999_950)).toBe('1M');
    expect(compactAmount(999_950_000)).toBe('1B');
  });

  it('stays in the unit when rounding stays below a thousand', () => {
    expect(compactAmount(999_949)).toBe('999.9k');
  });

  it('shortens with one decimal only when needed', () => {
    expect(compactAmount(1_500)).toBe('1.5k');
    expect(compactAmount(2_000_000)).toBe('2M');
    expect(compactAmount(3_250_000_000)).toBe('3.3B');
  });

  it('keeps the sign on negative amounts', () => {
    expect(compactAmount(-1_500)).toBe('-1.5k');
    expect(compactAmount(-999_950)).toBe('-1M');
    expect(compactAmount(-0.4)).toBe('0');
  });
});
