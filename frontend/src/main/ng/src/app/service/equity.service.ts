import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RSUGrant, RSUGrantReport, STARGrant, STARGrantReport } from '../model/equity';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class EquityService {

  private rsuUrl = `${environment.API_URL}/api/v1/equity/rsu`;
  private starUrl = `${environment.API_URL}/api/v1/equity/star`;

  constructor(private http: HttpClient) { }

  getGrants() {
    return this.http.get<RSUGrantReport>(this.rsuUrl);
  }

  create(grant: RSUGrant) {
    return this.http.post<RSUGrant>(this.rsuUrl, JSON.stringify(this.toPayload(grant)));
  }

  update(grant: RSUGrant) {
    return this.http.put<RSUGrant>(`${this.rsuUrl}/${grant.id}`, JSON.stringify(this.toPayload(grant)));
  }

  deleteByIds(ids: string) {
    return this.http.delete(`${this.rsuUrl}?ids=${ids}`);
  }

  downloadCsv() {
    return this.http.get(`${this.rsuUrl}/csv`, {
      responseType: 'blob'
    });
  }

  getStarGrants() {
    return this.http.get<STARGrantReport>(this.starUrl);
  }

  createStar(grant: STARGrant) {
    return this.http.post<STARGrant>(this.starUrl, JSON.stringify(this.toStarPayload(grant)));
  }

  updateStar(grant: STARGrant) {
    return this.http.put<STARGrant>(`${this.starUrl}/${grant.id}`, JSON.stringify(this.toStarPayload(grant)));
  }

  deleteStarByIds(ids: string) {
    return this.http.delete(`${this.starUrl}?ids=${ids}`);
  }

  private toPayload(grant: RSUGrant) {
    return {
      shortName: grant.shortName,
      exchange: grant.exchange,
      currency: grant.currency,
      grantDate: grant.grantDate,
      quantity: grant.quantity,
      vestingYears: grant.vestingYears,
      vestingFrequency: grant.vestingFrequency,
      note: grant.note
    };
  }

  private toStarPayload(grant: STARGrant) {
    return {
      name: grant.name,
      currency: grant.currency,
      commencementDate: grant.commencementDate,
      quantity: grant.quantity,
      baseValue: grant.baseValue,
      currentValue: grant.currentValue,
      vestingYears: grant.vestingYears,
      note: grant.note,
      applyValueToAll: grant.applyValueToAll ?? false
    };
  }
}
