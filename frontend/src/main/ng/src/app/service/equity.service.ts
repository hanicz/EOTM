import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RSUGrant, RSUGrantReport, STARGrant, STARGrantReport } from '../model/equity';
import { ResourceHelper } from '../util/servicehelper';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class EquityService {

  private helper = new ResourceHelper();

  private rsuUrl = `${environment.API_URL}/api/v1/equity/rsu`;
  private starUrl = `${environment.API_URL}/api/v1/equity/star`;

  constructor(private http: HttpClient) { }

  getGrants() {
    return this.http.get<RSUGrantReport>(this.rsuUrl, { headers: this.helper.getHeadersWithToken() });
  }

  create(grant: RSUGrant) {
    return this.http.post<RSUGrant>(this.rsuUrl, JSON.stringify(this.toPayload(grant)), {
      headers: this.helper.getHeadersWithToken()
    });
  }

  update(grant: RSUGrant) {
    return this.http.put<RSUGrant>(`${this.rsuUrl}/${grant.id}`, JSON.stringify(this.toPayload(grant)), {
      headers: this.helper.getHeadersWithToken()
    });
  }

  deleteByIds(ids: string) {
    return this.http.delete(`${this.rsuUrl}?ids=${ids}`, { headers: this.helper.getHeadersWithToken() });
  }

  downloadCsv() {
    return this.http.get(`${this.rsuUrl}/csv`, {
      headers: this.helper.getHeadersWithToken(),
      responseType: 'blob'
    });
  }

  getStarGrants() {
    return this.http.get<STARGrantReport>(this.starUrl, { headers: this.helper.getHeadersWithToken() });
  }

  createStar(grant: STARGrant) {
    return this.http.post<STARGrant>(this.starUrl, JSON.stringify(this.toStarPayload(grant)), {
      headers: this.helper.getHeadersWithToken()
    });
  }

  updateStar(grant: STARGrant) {
    return this.http.put<STARGrant>(`${this.starUrl}/${grant.id}`, JSON.stringify(this.toStarPayload(grant)), {
      headers: this.helper.getHeadersWithToken()
    });
  }

  deleteStarByIds(ids: string) {
    return this.http.delete(`${this.starUrl}?ids=${ids}`, { headers: this.helper.getHeadersWithToken() });
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
