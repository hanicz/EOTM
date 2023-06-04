import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ForexTransaction } from '../model/forextransaction';
import { ResourceHelper } from '../util/servicehelper';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ForexService {

  private helper = new ResourceHelper();

  private forexUrl = `${environment.API_URL}/forex`;

  constructor(private http: HttpClient) { }

  getTransactions() {
    const url = `${this.forexUrl}`;
    return this.http.get<ForexTransaction[]>(url, {
      headers: this.helper.getHeadersWithToken()
    });
  };

  getHolding() {
    const url = `${this.forexUrl}/holding`;
    return this.http.get<ForexTransaction[]>(url, {
      headers: this.helper.getHeadersWithToken()
    });
  };

  deleteByIds(ids: string) {
    const url = `${this.forexUrl}?ids=${ids}`;
    return this.http.delete(url, {
      headers: this.helper.getHeadersWithToken()
    });
  }

  download() {
    const url = `${this.forexUrl}/csv`;
    return this.http.get(url, {
      headers: this.helper.getHeadersWithToken(),
      responseType: 'blob'
    });
  }

  create(forexTransaction: ForexTransaction) {
    const url = `${this.forexUrl}`;
    return this.http.post<ForexTransaction>(url, JSON.stringify(forexTransaction), {
      headers: this.helper.getHeadersWithToken(),
    });
  }

  update(forexTransaction: ForexTransaction) {
    const url = `${this.forexUrl}`;
    return this.http.put<ForexTransaction>(url, JSON.stringify(forexTransaction), {
      headers: this.helper.getHeadersWithToken(),
    });
  }
}
