import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Dividend } from '../model/dividend';
import { ResourceHelper } from '../util/servicehelper';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class DividendService {

  private helper = new ResourceHelper();

  private dividendUrl = `${environment.API_URL}/dividend`;

  constructor(private http: HttpClient) { }

  getAllDividends() {
    const url = `${this.dividendUrl}`;
    return this.http.get<Dividend[]>(url, {
      headers: this.helper.getHeadersWithToken()
    });
  }

  deleteByIds(ids: string) {
    const url = `${this.dividendUrl}?ids=${ids}`;
    return this.http.delete(url, {
      headers: this.helper.getHeadersWithToken()
    });
  }

  download() {
    const url = `${this.dividendUrl}/csv`;
    return this.http.get(url, {
      headers: this.helper.getHeadersWithToken(),
      responseType: 'blob'
    });
  }

  create(dividend: Dividend) {
    const url = `${this.dividendUrl}`;
    return this.http.post<Dividend>(url, JSON.stringify(dividend), {
      headers: this.helper.getHeadersWithToken(),
    });
  }

  update(dividend: Dividend) {
    const url = `${this.dividendUrl}`;
    return this.http.put<Dividend>(url, JSON.stringify(dividend), {
      headers: this.helper.getHeadersWithToken(),
    });
  }

  uploadCSV(file: File) {
    const formData = new FormData();
    formData.append('file', file, 'file.csv')
    const url = `${this.dividendUrl}/process/csv`;
    return this.http.post<any>(url, formData, {
      headers: this.helper.getAuthHeaders(),
    });
  }
}
