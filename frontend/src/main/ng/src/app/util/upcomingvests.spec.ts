import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { RSUGrant, RSUVest } from '../model/equity';
import { buildUpcomingVests } from './upcomingvests';

const TODAY = new Date(2026, 8, 5, 10, 30);

function vest(sequence: number, vestDate: string, vested: boolean, netInHuf = 1000000): RSUVest {
  return {
    grantId: 1,
    shortName: 'ACME',
    exchange: 'US',
    grantDate: '2024-03-01',
    sequence,
    vestDate,
    quantity: 100,
    currency: 'USD',
    price: 100,
    priceDate: vestDate,
    amount: 10000,
    rate: 350,
    rateDate: vestDate,
    amountInHuf: 1370000,
    netInHuf,
    vested,
    projected: !vested,
    tax: { amount: 1370000, taxBase: 1219300, szocho: 158509, szja: 182895, total: 341404 }
  };
}

function grant(id: number, shortName: string, vests: RSUVest[] | undefined, error: string | null = null): RSUGrant {
  return {
    id,
    shortName,
    exchange: 'US',
    currency: null,
    grantDate: '2024-03-01',
    quantity: 400,
    vestingYears: 4,
    vestingFrequency: 'ANNUAL',
    note: null,
    vests,
    error
  };
}

describe('buildUpcomingVests', () => {

  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(TODAY);
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('takes the first unvested tranche of each grant', () => {
    const vests = buildUpcomingVests([grant(1, 'ACME', [
      vest(1, '2025-03-01', true),
      vest(2, '2026-03-01', true),
      vest(3, '2027-03-01', false),
      vest(4, '2028-03-01', false)
    ])]);

    expect(vests.length).toBe(1);
    expect(vests[0].vestDate).toBe('2027-03-01');
    expect(vests[0].quantity).toBe(100);
    expect(vests[0].shortName).toBe('ACME');
  });

  it('drops a grant that has fully vested', () => {
    const vests = buildUpcomingVests([grant(1, 'ACME', [
      vest(1, '2025-03-01', true),
      vest(2, '2026-03-01', true)
    ])]);

    expect(vests).toEqual([]);
  });

  it('drops a grant with no vests at all', () => {
    expect(buildUpcomingVests([grant(1, 'ACME', [])])).toEqual([]);
    expect(buildUpcomingVests([grant(1, 'ACME', undefined)])).toEqual([]);
  });

  it('orders grants by whichever vests soonest', () => {
    const vests = buildUpcomingVests([
      grant(1, 'LATER', [vest(1, '2028-01-01', false)]),
      grant(2, 'SOONEST', [vest(1, '2026-10-01', false)]),
      grant(3, 'MIDDLE', [vest(1, '2027-05-01', false)])
    ]);

    expect(vests.map(v => v.shortName)).toEqual(['SOONEST', 'MIDDLE', 'LATER']);
  });

  it('counts the days to the vest from today', () => {
    const vests = buildUpcomingVests([grant(1, 'ACME', [vest(1, '2026-09-06', false)])]);

    expect(vests[0].daysUntil).toBe(1);
  });

  it('leaves the amount empty when the grant could not be priced', () => {
    const vests = buildUpcomingVests([
      grant(1, 'GONE', [vest(1, '2027-03-01', false, 0)], 'Could not value GONE.US')
    ]);

    expect(vests.length).toBe(1);
    expect(vests[0].netInHuf).toBeNull();
    expect(vests[0].vestDate).toBe('2027-03-01');
    expect(vests[0].quantity).toBe(100);
  });

  it('respects a limit, keeping the soonest', () => {
    const vests = buildUpcomingVests([
      grant(1, 'LATER', [vest(1, '2028-01-01', false)]),
      grant(2, 'SOONEST', [vest(1, '2026-10-01', false)]),
      grant(3, 'MIDDLE', [vest(1, '2027-05-01', false)])
    ], 2);

    expect(vests.map(v => v.shortName)).toEqual(['SOONEST', 'MIDDLE']);
  });

  it('returns nothing for no grants', () => {
    expect(buildUpcomingVests([])).toEqual([]);
  });
});
