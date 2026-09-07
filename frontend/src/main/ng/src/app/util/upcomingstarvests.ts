import { STARGrant } from '../model/equity';
import { daysUntil } from './upcomingpayments';

export interface UpcomingStarVest {
  grantId: number;
  name: string;
  vestDate: string;
  quantity: number;
  cliff: boolean;
  daysUntil: number;
}

export function buildUpcomingStarVests(grants: STARGrant[], limit?: number): UpcomingStarVest[] {
  const vests = grants
    .map(grant => ({ grant, next: grant.vests?.find(vest => !vest.vested) }))
    .filter(pair => pair.next != null)
    .map(pair => ({
      grantId: pair.grant.id!,
      name: pair.grant.name,
      vestDate: pair.next!.vestDate,
      quantity: pair.next!.quantity,
      cliff: pair.next!.cliff,
      daysUntil: daysUntil(pair.next!.vestDate)
    }))
    .sort((a, b) => a.daysUntil - b.daysUntil);

  return limit == null ? vests : vests.slice(0, limit);
}
