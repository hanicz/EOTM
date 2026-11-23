import { RSUGrant } from '../model/equity';
import { daysUntil } from './upcomingpayments';

export interface UpcomingVest {
  grantId: number;
  shortName: string;
  exchange: string;
  vestDate: string;
  quantity: number;
  netInHuf: number | null;
  daysUntil: number;
}

export function buildUpcomingVests(grants: RSUGrant[], limit?: number): UpcomingVest[] {
  const vests = grants
    .map(grant => ({ grant, next: grant.vests?.find(vest => !vest.vested) }))
    .filter(pair => pair.next != null)
    .map(pair => ({
      grantId: pair.grant.id!,
      shortName: pair.grant.shortName,
      exchange: pair.grant.exchange,
      vestDate: pair.next!.vestDate,
      quantity: pair.next!.quantity,
      netInHuf: pair.grant.error ? null : pair.next!.netInHuf,
      daysUntil: daysUntil(pair.next!.vestDate)
    }))
    .sort((a, b) => a.daysUntil - b.daysUntil);

  return limit == null ? vests : vests.slice(0, limit);
}
