import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ETFInvestment } from '../model/etfinvestment';
import { ResourceHelper } from '../util/servicehelper';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class EtfService {

  private helper = new ResourceHelper();

  private etfUrl = `${environment.API_URL}/etf`;

  constructor(private http: HttpClient) { }

  getInvestments() {
    const url = `${this.etfUrl}`;
    return this.http.get<ETFInvestment[]>(url, {
      headers: this.helper.getHeadersWithToken()
    });
  };

  getHolding(currency: string) {
    let data = { "currency": currency }
    const url = `${this.etfUrl}/holding`;
    return this.http.post<ETFInvestment[]>(url, JSON.stringify(data), {
      headers: this.helper.getHeadersWithToken()
    });
  };
}
