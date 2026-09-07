import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { STARGrant, STARVest } from '../model/equity';
import { buildUpcomingStarVests } from './upcomingstarvests';

const TODAY = new Date(2026, 8, 5, 10, 30);

function vest(sequence: number, vestDate: string, vested: boolean, cliff = false): STARVest {
  return {
    grantId: 1,
    name: 'ORBITAL',
    commencementDate: '2024-04-08',
    sequence,
    vestDate,
    quantity: 250,
    cliff,
    currency: 'EUR',
    spreadPerUnit: 12.5,
    amount: 3125,
    rate: 400,
    rateDate: vestDate,
    amountInHuf: 1250000,
    netInHuf: 890000,
    vested,
    projected: !vested,
    tax: { amount: 1250000, taxBase: 1250000, szocho: 162500, szja: 187500, total: 350000 }
  };
}

function grant(id: number, name: string, vests: STARVest[] | undefined): STARGrant {
  return {
    id,
    name,
    currency: 'EUR',
    commencementDate: '2024-04-08',
    quantity: 1000,
    baseValue: 100,
    currentValue: 112.5,
    vestingYears: 4,
    note: null,
    vests
  };
}

describe('buildUpcomingStarVests', () => {

  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(TODAY);
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('takes the first unvested tranche of each grant', () => {
    const vests = buildUpcomingStarVests([grant(1, 'ORBITAL', [
      vest(4, '2025-04-08', true, true),
      vest(5, '2025-07-08', true),
      vest(6, '2026-10-08', false),
      vest(7, '2027-01-08', false)
    ])]);

    expect(vests.length).toBe(1);
    expect(vests[0].vestDate).toBe('2026-10-08');
    expect(vests[0].quantity).toBe(250);
    expect(vests[0].name).toBe('ORBITAL');
  });

  it('drops a grant that has fully vested', () => {
    const vests = buildUpcomingStarVests([grant(1, 'ORBITAL', [
      vest(4, '2025-04-08', true, true),
      vest(5, '2025-07-08', true)
    ])]);

    expect(vests).toEqual([]);
  });

  it('drops a grant with no vests at all', () => {
    expect(buildUpcomingStarVests([grant(1, 'ORBITAL', [])])).toEqual([]);
    expect(buildUpcomingStarVests([grant(1, 'ORBITAL', undefined)])).toEqual([]);
  });

  it('orders grants by whichever vests soonest', () => {
    const vests = buildUpcomingStarVests([
      grant(1, 'LATER', [vest(4, '2028-01-08', false)]),
      grant(2, 'SOONEST', [vest(4, '2026-10-08', false)]),
      grant(3, 'MIDDLE', [vest(4, '2027-04-08', false)])
    ]);

    expect(vests.map(v => v.name)).toEqual(['SOONEST', 'MIDDLE', 'LATER']);
  });

  it('counts the days to the vest from today', () => {
    const vests = buildUpcomingStarVests([grant(1, 'ORBITAL', [vest(4, '2026-09-06', false)])]);

    expect(vests[0].daysUntil).toBe(1);
  });

  it('carries the cliff marker through', () => {
    const vests = buildUpcomingStarVests([grant(1, 'ORBITAL', [vest(4, '2026-10-08', false, true)])]);

    expect(vests[0].cliff).toBe(true);
  });

  it('respects a limit, keeping the soonest', () => {
    const vests = buildUpcomingStarVests([
      grant(1, 'LATER', [vest(4, '2028-01-08', false)]),
      grant(2, 'SOONEST', [vest(4, '2026-10-08', false)]),
      grant(3, 'MIDDLE', [vest(4, '2027-04-08', false)])
    ], 2);

    expect(vests.map(v => v.name)).toEqual(['SOONEST', 'MIDDLE']);
  });

  it('returns nothing for no grants', () => {
    expect(buildUpcomingStarVests([])).toEqual([]);
  });
});
