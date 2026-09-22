export type WithProfit<T> = T & { profit: number | null };

export function withProfit<T extends { quantity: number; amount: number }>(rows: T[]): WithProfit<T>[] {
  return rows.map(row => ({ ...row, profit: row.quantity === 0 ? -row.amount : null }));
}
