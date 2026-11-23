import { describe, expect, it } from 'vitest';
import { AccountTotalsRow, calculateAccountTotals } from './accounttotals';

const identity = (amount: number) => amount;

const rows: AccountTotalsRow[] = [
  { accountName: 'Broker One', amount: 1000, currencyId: 'EUR', liveValue: 1250, dayChange: 25 },
  { accountName: 'Broker One', amount: 500, currencyId: 'EUR', liveValue: 450, dayChange: -10 },
  { accountName: 'Alpha Trade', amount: 2000, currencyId: 'EUR', liveValue: 2400, dayChange: 40 }
];

describe('calculateAccountTotals', () => {

  it('sums every stat per account', () => {
    const totals = calculateAccountTotals(rows, identity);

    expect(totals).toHaveLength(2);
    const broker = totals[1];
    expect(broker.accountName).toBe('Broker One');
    expect(broker.spent).toBe(1500);
    expect(broker.worth).toBe(1700);
    expect(broker.todayDiff).toBe(15);
    expect(broker.diff).toBe(200);
    expect(broker.percentage).toBeCloseTo(13.333, 3);
  });

  it('sorts the accounts by name', () => {
    expect(calculateAccountTotals(rows, identity).map(a => a.accountName))
      .toEqual(['Alpha Trade', 'Broker One']);
  });

  it('falls back to the amount when there is no live value', () => {
    const totals = calculateAccountTotals(
      [{ accountName: 'Alpha Trade', amount: 800, currencyId: 'EUR' }], identity);

    expect(totals[0].worth).toBe(800);
    expect(totals[0].diff).toBe(0);
    expect(totals[0].percentage).toBe(0);
    expect(totals[0].todayDiff).toBe(0);
  });

  it('converts every amount through the supplied converter', () => {
    const double = (amount: number, currency: string) => currency === 'USD' ? amount * 2 : amount;
    const totals = calculateAccountTotals([
      { accountName: 'Alpha Trade', amount: 100, currencyId: 'USD', liveValue: 150, dayChange: 5 },
      { accountName: 'Alpha Trade', amount: 100, currencyId: 'EUR', liveValue: 120, dayChange: 5 }
    ], double);

    expect(totals[0].spent).toBe(300);
    expect(totals[0].worth).toBe(420);
    expect(totals[0].todayDiff).toBe(15);
  });

  it('reports a zero percentage instead of dividing by zero', () => {
    const totals = calculateAccountTotals(
      [{ accountName: 'Alpha Trade', amount: 0, currencyId: 'EUR', liveValue: 0, dayChange: 0 }], identity);

    expect(totals[0].percentage).toBe(0);
    expect(totals[0].todayPercentage).toBe(0);
  });

  it('works out the day percentage against the previous worth', () => {
    const totals = calculateAccountTotals(
      [{ accountName: 'Alpha Trade', amount: 900, currencyId: 'EUR', liveValue: 1100, dayChange: 100 }], identity);

    expect(totals[0].todayPercentage).toBeCloseTo(10, 6);
  });

  it('leaves out rows with no account', () => {
    const totals = calculateAccountTotals([
      { accountName: 'Alpha Trade', amount: 100, currencyId: 'EUR', liveValue: 110 },
      { amount: 700, currencyId: 'EUR', liveValue: 900 },
      { accountName: '', amount: 300, currencyId: 'EUR', liveValue: 400 }
    ], identity);

    expect(totals).toHaveLength(1);
    expect(totals[0].spent).toBe(100);
  });

  it('returns nothing for an empty portfolio', () => {
    expect(calculateAccountTotals([], identity)).toEqual([]);
  });
});
