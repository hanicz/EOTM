import { describe, expect, it } from 'vitest';
import { dotYears, nextMilestone } from './fireplan';
import { FireProjection, FireYear } from '../model/fire';

function year(value: number, balance: number): FireYear {
  return {
    year: value,
    age: 30 + value,
    phase: 'ACCUMULATION',
    contributions: 0,
    growth: 0,
    pension: 0,
    withdrawals: 0,
    balance: balance,
    realBalance: balance,
    pctOfFireNumber: 0
  };
}

function projection(overrides: Partial<FireProjection> = {}): FireProjection {
  const timeline = [year(0, 40), year(1, 90), year(2, 140), year(3, 210), year(4, 320), year(5, 560)];
  return {
    currency: 'XTS',
    portfolioValue: 40,
    otherAssets: 0,
    startingValue: 40,
    unconvertedCurrencies: [],
    timeline: timeline,
    milestones: timeline,
    fireNumber: 500,
    annualSpending: 24,
    withdrawalRate: 4,
    fireNumberInTodaysMoney: false,
    fiReached: true,
    fiYear: 5,
    fiAge: 35,
    retirementYear: 5,
    retirementAge: 35,
    pensionYear: null,
    depletedAtAge: null,
    lastsThroughRetirement: true,
    finalAge: 80,
    finalRealBalance: 100,
    ...overrides
  };
}

describe('nextMilestone', () => {

  it('picks the next round number above the starting value', () => {
    const milestone = nextMilestone(projection());
    expect(milestone?.target).toBe(50);
    expect(milestone?.years).toBe(1);
    expect(milestone?.age).toBe(31);
    expect(milestone?.pct).toBeCloseTo(80);
  });

  it('steps through the one, two and five ladder', () => {
    const targets = [6, 12, 30, 60, 120, 300].map(start =>
      nextMilestone(projection({ startingValue: start }))?.target);
    expect(targets).toEqual([10, 20, 50, 100, 200, 500]);
  });

  it('never proposes a milestone beyond the target itself', () => {
    const milestone = nextMilestone(projection({ startingValue: 300, fireNumber: 400 }));
    expect(milestone?.target).toBe(400);
  });

  it('returns nothing once the target is already met', () => {
    expect(nextMilestone(projection({ startingValue: 500 }))).toBeNull();
    expect(nextMilestone(projection({ startingValue: 800 }))).toBeNull();
  });

  it('returns nothing when the pot never reaches the milestone', () => {
    const flat = [year(0, 40), year(1, 41), year(2, 42)];
    expect(nextMilestone(projection({ timeline: flat }))).toBeNull();
  });

  it('returns nothing when there is nothing to start from', () => {
    expect(nextMilestone(projection({ startingValue: 0 }))).toBeNull();
  });
});

describe('dotYears', () => {

  it('spreads four dots evenly up to the year the target is cleared', () => {
    expect(dotYears(projection({ fiYear: 12 }))).toEqual([0, 4, 8, 12]);
  });

  it('runs to the end of the plan when the target is never cleared', () => {
    const timeline = Array.from({ length: 10 }, (_, index) => year(index, index * 10));
    expect(dotYears(projection({ fiYear: null, timeline: timeline }))).toEqual([0, 3, 6, 9]);
  });

  it('does not repeat a year on a short plan', () => {
    expect(dotYears(projection({ fiYear: 2 }))).toEqual([0, 1, 2]);
  });

  it('collapses to a single dot when the target is already met', () => {
    expect(dotYears(projection({ fiYear: 0 }))).toEqual([0]);
  });
});
