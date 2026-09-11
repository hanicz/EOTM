import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Dividend } from '../model/dividend';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class DividendService {

  private dividendUrl = `${environment.API_URL}/api/v1/dividend`;

  constructor(private http: HttpClient) { }

  getAllDividends() {
    const url = `${this.dividendUrl}`;
    return this.http.get<Dividend[]>(url);
  }

  deleteByIds(ids: string) {
    const url = `${this.dividendUrl}?ids=${ids}`;
    return this.http.delete(url);
  }

  download() {
    const url = `${this.dividendUrl}/csv`;
    return this.http.get(url, {
      responseType: 'blob'
    });
  }

  create(dividend: Dividend) {
    const url = `${this.dividendUrl}`;
    return this.http.post<Dividend>(url, JSON.stringify(dividend));
  }

  update(dividend: Dividend) {
    const url = `${this.dividendUrl}`;
    return this.http.put<Dividend>(url, JSON.stringify(dividend));
  }

  uploadCSV(file: File) {
    const formData = new FormData();
    formData.append('file', file, 'file.csv')
    const url = `${this.dividendUrl}/process/csv`;
    return this.http.post<any>(url, formData);
  }
}
