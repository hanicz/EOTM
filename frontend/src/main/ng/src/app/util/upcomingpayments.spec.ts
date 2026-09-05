import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { SecurityTransaction } from '../model/securityTransaction';
import { buildUpcomingPayments, daysUntil, dueInLabel } from './upcomingpayments';

const TODAY = new Date(2026, 8, 5, 10, 30);

function holding(securityName: string, nextPaymentDate?: string, nextPaymentAmount?: number): SecurityTransaction {
  return {
    transactionId: 1,
    quantity: 100,
    buySell: 'B',
    transactionDate: new Date(2024, 0, 1),
    securityId: securityName,
    securityName: securityName,
    amount: 100000,
    currencyId: 'HUF',
    nextPaymentDate: nextPaymentDate as unknown as Date,
    nextPaymentAmount: nextPaymentAmount
  };
}

describe('daysUntil', () => {

  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(TODAY);
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('counts whole days from the yyyy-MM-dd string the API sends', () => {
    expect(daysUntil('2026-09-05')).toBe(0);
    expect(daysUntil('2026-09-06')).toBe(1);
    expect(daysUntil('2026-12-01')).toBe(87);
  });

  it('reads the date in local time, so it does not slip a day', () => {
    vi.setSystemTime(new Date(2026, 8, 5, 23, 59));
    expect(daysUntil('2026-09-06')).toBe(1);

    vi.setSystemTime(new Date(2026, 8, 5, 0, 1));
    expect(daysUntil('2026-09-06')).toBe(1);
  });

  it('accepts a Date as well as a string', () => {
    expect(daysUntil(new Date(2026, 8, 12))).toBe(7);
  });
});

describe('dueInLabel', () => {
  it('names today and tomorrow, and counts the rest', () => {
    expect(dueInLabel(0)).toBe('today');
    expect(dueInLabel(1)).toBe('tomorrow');
    expect(dueInLabel(14)).toBe('14 days');
  });
});

describe('buildUpcomingPayments', () => {

  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(TODAY);
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('sorts soonest first', () => {
    const result = buildUpcomingPayments([
      holding('Bond C', '2026-12-01', 4000),
      holding('Bond A', '2026-09-22', 1000),
      holding('Bond B', '2026-10-15', 2000)
    ]);

    expect(result.map(p => p.securityName)).toEqual(['Bond A', 'Bond B', 'Bond C']);
    expect(result[0].daysUntil).toBe(17);
  });

  it('leaves out holdings with no payment date or no amount', () => {
    const result = buildUpcomingPayments([
      holding('Bond A', '2026-09-22', 1000),
      holding('No date', undefined, 2000),
      holding('No amount', '2026-10-15', undefined)
    ]);

    expect(result.map(p => p.securityName)).toEqual(['Bond A']);
  });

  it('honours the limit', () => {
    const result = buildUpcomingPayments([
      holding('Bond C', '2026-12-01', 4000),
      holding('Bond A', '2026-09-22', 1000),
      holding('Bond B', '2026-10-15', 2000),
      holding('Bond D', '2027-01-08', 5000)
    ], 3);

    expect(result.map(p => p.securityName)).toEqual(['Bond A', 'Bond B', 'Bond C']);
  });

  it('returns fewer than the limit when there are fewer', () => {
    expect(buildUpcomingPayments([holding('Bond A', '2026-09-22', 1000)], 3)).toHaveLength(1);
  });

  it('returns everything when no limit is given', () => {
    const result = buildUpcomingPayments([
      holding('Bond A', '2026-09-22', 1000),
      holding('Bond B', '2026-10-15', 2000),
      holding('Bond C', '2026-12-01', 4000),
      holding('Bond D', '2027-01-08', 5000)
    ]);

    expect(result).toHaveLength(4);
  });

  it('carries the currency and amount through untouched', () => {
    const euro = holding('Euro bond', '2026-09-22', 12.5);
    euro.currencyId = 'EUR';

    const [payment] = buildUpcomingPayments([euro]);

    expect(payment.currencyId).toBe('EUR');
    expect(payment.nextPaymentAmount).toBe(12.5);
  });

  it('is empty when nothing is held', () => {
    expect(buildUpcomingPayments([], 3)).toEqual([]);
  });
});
