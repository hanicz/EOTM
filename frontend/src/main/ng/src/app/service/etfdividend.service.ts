import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ETFDividend } from '../model/etfdividend';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class EtfdividendService {

  private etfDividendUrl = `${environment.API_URL}/api/v1/etfdividend`;

  constructor(private http: HttpClient) { }

  getAllDividends() {
    const url = `${this.etfDividendUrl}`;
    return this.http.get<ETFDividend[]>(url);
  }

  deleteByIds(ids: string) {
    const url = `${this.etfDividendUrl}?ids=${ids}`;
    return this.http.delete(url);
  }

  download() {
    const url = `${this.etfDividendUrl}/csv`;
    return this.http.get(url, {
      responseType: 'blob'
    });
  }

  create(dividend: ETFDividend) {
    const url = `${this.etfDividendUrl}`;
    return this.http.post<ETFDividend>(url, JSON.stringify(dividend));
  }

  update(dividend: ETFDividend) {
    const url = `${this.etfDividendUrl}`;
    return this.http.put<ETFDividend>(url, JSON.stringify(dividend));
  }

  uploadCSV(file: File) {
    const formData = new FormData();
    formData.append('file', file, 'file.csv')
    const url = `${this.etfDividendUrl}/process/csv`;
    return this.http.post<any>(url, formData);
  }
}
