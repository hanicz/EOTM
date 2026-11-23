import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ForexTransaction } from '../model/forextransaction';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ForexService {

  private forexUrl = `${environment.API_URL}/api/v1/forex`;

  constructor(private http: HttpClient) { }

  getTransactions() {
    const url = `${this.forexUrl}`;
    return this.http.get<ForexTransaction[]>(url);
  };

  getHolding(refresh = false) {
    const url = `${this.forexUrl}/holding${refresh ? '?refresh=true' : ''}`;
    return this.http.get<ForexTransaction[]>(url);
  };

  deleteByIds(ids: string) {
    const url = `${this.forexUrl}?ids=${ids}`;
    return this.http.delete(url);
  }

  download() {
    const url = `${this.forexUrl}/csv`;
    return this.http.get(url, {
      responseType: 'blob'
    });
  }

  create(forexTransaction: ForexTransaction) {
    const url = `${this.forexUrl}`;
    return this.http.post<ForexTransaction>(url, JSON.stringify(forexTransaction));
  }

  update(forexTransaction: ForexTransaction) {
    const url = `${this.forexUrl}`;
    return this.http.put<ForexTransaction>(url, JSON.stringify(forexTransaction));
  }

  uploadCSV(file: File) {
    const formData = new FormData();
    formData.append('file', file, 'file.csv')
    const url = `${this.forexUrl}/process/csv`;
    return this.http.post<any>(url, formData);
  }
}
