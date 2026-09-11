import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RSU, StockRSUTaxReport, TaxBreakdown, TaxReport, TaxableEventReport } from '../model/rsu';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class TaxService {

  private taxUrl = `${environment.API_URL}/api/v1/tax`;

  constructor(private http: HttpClient) { }

  calculateForRSUs(rsus: RSU[]) {
    return this.http.post<TaxReport>(`${this.taxUrl}/rsu`, JSON.stringify(rsus));
  }

  downloadRSUCsv(rsus: RSU[]) {
    return this.http.post(`${this.taxUrl}/rsu/csv`, JSON.stringify(rsus), {
      responseType: 'blob'
    });
  }

  calculateForAmount(amount: number) {
    return this.http.post<TaxBreakdown>(`${this.taxUrl}/amount`, JSON.stringify({ amount }));
  }

  getTaxableEvents() {
    return this.http.get<TaxableEventReport>(`${this.taxUrl}/transaction`);
  }

  setTaxPaid(ids: string, paid: boolean) {
    const url = `${this.taxUrl}/transaction/paid?ids=${ids}&paid=${paid}`;
    return this.http.put(url, null);
  }

  downloadTaxableEventsCsv() {
    return this.http.get(`${this.taxUrl}/transaction/csv`, {
      responseType: 'blob'
    });
  }

  getStockRSUEvents() {
    return this.http.get<StockRSUTaxReport>(`${this.taxUrl}/stock`);
  }

  setStockRSUPaid(ids: string, paid: boolean) {
    const url = `${this.taxUrl}/stock/paid?ids=${ids}&paid=${paid}`;
    return this.http.put(url, null);
  }

  downloadStockRSUCsv() {
    return this.http.get(`${this.taxUrl}/stock/csv`, {
      responseType: 'blob'
    });
  }
}
