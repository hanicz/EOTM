import { describe, expect, it } from 'vitest';
import { withProfit } from './positionprofit';

describe('withProfit', () => {
  it('gives a closed position the money it returned as profit', () => {
    const [row] = withProfit([{ quantity: 0, amount: -250 }]);
    expect(row.profit).toBe(250);
  });

  it('reports a closed loss as negative', () => {
    const [row] = withProfit([{ quantity: 0, amount: 80 }]);
    expect(row.profit).toBe(-80);
  });

  it('leaves open positions without a profit', () => {
    const [row] = withProfit([{ quantity: 3, amount: 900 }]);
    expect(row.profit).toBeNull();
  });

  it('keeps the other fields of the row', () => {
    const [row] = withProfit([{ quantity: 0, amount: -10, shortName: 'AAA' }]);
    expect(row.shortName).toBe('AAA');
  });
});
