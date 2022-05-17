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

  getPositions(currency: string) {
    let data = { "currency": currency }
    const url = `${this.etfUrl}/position`;
    return this.http.post<ETFInvestment[]>(url, JSON.stringify(data), {
      headers: this.helper.getHeadersWithToken()
    });
  };

  deleteByIds(ids: string) {
    const url = `${this.etfUrl}?ids=${ids}`;
    return this.http.delete(url, {
      headers: this.helper.getHeadersWithToken()
    });
  }

  download() {
    const url = `${this.etfUrl}/csv`;
    return this.http.get(url, {
      headers: this.helper.getHeadersWithToken(),
      responseType: 'blob'
    });
  }

  create(investment: ETFInvestment) {
    const url = `${this.etfUrl}`;
    return this.http.post<ETFInvestment>(url, JSON.stringify(investment), {
      headers: this.helper.getHeadersWithToken(),
    });
  }

  update(investment: ETFInvestment) {
    const url = `${this.etfUrl}`;
    return this.http.put<ETFInvestment>(url, JSON.stringify(investment), {
      headers: this.helper.getHeadersWithToken(),
    });
  }
}
