import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Transaction } from '../model/transaction';
import { ResourceHelper } from '../util/servicehelper';
import { environment } from '../../environments/environment';


@Injectable({
  providedIn: 'root'
})
export class CryptoService {

  private helper = new ResourceHelper();

  private transactionUrl = `${environment.API_URL}/transaction`;

  constructor(private http: HttpClient) { }

  getAllCrypto() {
    const url = `${environment.API_URL}/coin`;
    return this.http.get<Crypto[]>(url, {
      headers: this.helper.getHeadersWithToken()
    });
  };

  getTransactions() {
    const url = `${this.transactionUrl}`;
    return this.http.get<Transaction[]>(url, {
      headers: this.helper.getHeadersWithToken()
    });
  };

  getPositions() {
    const url = `${this.transactionUrl}/position`;
    return this.http.post<Transaction[]>(url, {
      headers: this.helper.getHeadersWithToken()
    });
  };

  getHoldings(currency: string) {
    var data = { "currency": currency }
    const url = `${this.transactionUrl}/holding`;
    return this.http.post<Transaction[]>(url, JSON.stringify(data), {
      headers: this.helper.getHeadersWithToken()
    });
  };

  deleteByIds(ids: string) {
    const url = `${this.transactionUrl}?ids=${ids}`;
    return this.http.delete(url, {
      headers: this.helper.getHeadersWithToken()
    });
  }

  download() {
    const url = `${this.transactionUrl}/csv`;
    return this.http.get(url, {
      headers: this.helper.getHeadersWithToken(),
      responseType: 'blob'
    });
  }

  create(transaction: Transaction) {
    const url = `${this.transactionUrl}`;
    return this.http.post<Transaction>(url, JSON.stringify(transaction), {
      headers: this.helper.getHeadersWithToken(),
    });
  }

  update(transaction: Transaction) {
    const url = `${this.transactionUrl}`;
    return this.http.put<Transaction>(url, JSON.stringify(transaction), {
      headers: this.helper.getHeadersWithToken(),
    });
  }

  uploadCSV(file: File) {
    const formData = new FormData();
    formData.append('file', file, 'file.csv')
    const url = `${this.transactionUrl}/process/csv`;
    return this.http.post<any>(url, formData, {
      headers: this.helper.getAuthHeaders(),
    });
  }
}
