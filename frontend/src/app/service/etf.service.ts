import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ETFInvestment } from '../model/etfinvestment';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class EtfService {

  private etfUrl = `${environment.API_URL}/api/v1/etf`;

  constructor(private http: HttpClient) { }

  getInvestments() {
    const url = `${this.etfUrl}`;
    return this.http.get<ETFInvestment[]>(url);
  };

  getHolding(refresh = false) {
    const url = `${this.etfUrl}/holding${refresh ? '?refresh=true' : ''}`;
    return this.http.get<ETFInvestment[]>(url);
  };

  getPositions() {
    const url = `${this.etfUrl}/position`;
    return this.http.get<ETFInvestment[]>(url);
  };

  deleteByIds(ids: string) {
    const url = `${this.etfUrl}?ids=${ids}`;
    return this.http.delete(url);
  }

  download() {
    const url = `${this.etfUrl}/csv`;
    return this.http.get(url, {
      responseType: 'blob'
    });
  }

  create(investment: ETFInvestment) {
    const url = `${this.etfUrl}`;
    return this.http.post<ETFInvestment>(url, JSON.stringify(investment));
  }

  update(investment: ETFInvestment) {
    const url = `${this.etfUrl}`;
    return this.http.put<ETFInvestment>(url, JSON.stringify(investment));
  }

  uploadCSV(file: File) {
    const formData = new FormData();
    formData.append('file', file, 'file.csv')
    const url = `${this.etfUrl}/process/csv`;
    return this.http.post<any>(url, formData);
  }
}
