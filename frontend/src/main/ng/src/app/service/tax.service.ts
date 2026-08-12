import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RSU, TaxBreakdown, TaxReport } from '../model/rsu';
import { ResourceHelper } from '../util/servicehelper';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class TaxService {

  private helper = new ResourceHelper();

  private taxUrl = `${environment.API_URL}/api/v1/tax`;

  constructor(private http: HttpClient) { }

  calculateForRSUs(rsus: RSU[]) {
    return this.http.post<TaxReport>(`${this.taxUrl}/rsu`, JSON.stringify(rsus), {
      headers: this.helper.getHeadersWithToken()
    });
  }

  downloadRSUCsv(rsus: RSU[]) {
    return this.http.post(`${this.taxUrl}/rsu/csv`, JSON.stringify(rsus), {
      headers: this.helper.getHeadersWithToken(),
      responseType: 'blob'
    });
  }

  calculateForAmount(amount: number) {
    return this.http.post<TaxBreakdown>(`${this.taxUrl}/amount`, JSON.stringify({ amount }), {
      headers: this.helper.getHeadersWithToken()
    });
  }
}
