import { SecurityTransaction } from '../model/securityTransaction';

const MILLISECONDS_PER_DAY = 86400000;

export const SOON_IN_DAYS = 30;

export interface UpcomingPayment {
  securityName: string;
  currencyId: string;
  nextPaymentDate: Date;
  nextPaymentAmount: number;
  daysUntil: number;
}

export function daysUntil(date: Date | string): number {
  const target = typeof date === 'string'
    ? new Date(+date.slice(0, 4), +date.slice(5, 7) - 1, +date.slice(8, 10))
    : new Date(date.getFullYear(), date.getMonth(), date.getDate());
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  return Math.round((target.getTime() - today.getTime()) / MILLISECONDS_PER_DAY);
}

export function dueInLabel(days: number): string {
  if (days === 0) return 'today';
  if (days === 1) return 'tomorrow';
  return days + ' days';
}

export function buildUpcomingPayments(transactions: SecurityTransaction[], limit?: number): UpcomingPayment[] {
  const payments = transactions
    .filter(t => t.nextPaymentDate != null && t.nextPaymentAmount != null)
    .map(t => ({
      securityName: t.securityName,
      currencyId: t.currencyId,
      nextPaymentDate: t.nextPaymentDate!,
      nextPaymentAmount: t.nextPaymentAmount!,
      daysUntil: daysUntil(t.nextPaymentDate!)
    }))
    .sort((a, b) => a.daysUntil - b.daysUntil);

  return limit == null ? payments : payments.slice(0, limit);
}
