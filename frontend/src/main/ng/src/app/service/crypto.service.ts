import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Transaction } from '../model/transaction';
import { Crypto } from '../model/crypto';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class CryptoService {

  private transactionUrl = `${environment.API_URL}/api/v1/transaction`;

  constructor(private http: HttpClient) { }

  getAllCrypto() {
    const url = `${environment.API_URL}/api/v1/coin`;
    return this.http.get<Crypto[]>(url);
  };

  getTransactions() {
    const url = `${this.transactionUrl}`;
    return this.http.get<Transaction[]>(url);
  };

  getPositions() {
    const url = `${this.transactionUrl}/position`;
    return this.http.get<Transaction[]>(url);
  };

  getHoldings(refresh = false) {
    var data = { "currency": "EUR" }
    const url = `${this.transactionUrl}/holding${refresh ? '?refresh=true' : ''}`;
    return this.http.post<Transaction[]>(url, JSON.stringify(data));
  };

  deleteByIds(ids: string) {
    const url = `${this.transactionUrl}?ids=${ids}`;
    return this.http.delete(url);
  }

  download() {
    const url = `${this.transactionUrl}/csv`;
    return this.http.get(url, {
      responseType: 'blob'
    });
  }

  create(transaction: Transaction) {
    const url = `${this.transactionUrl}`;
    return this.http.post<Transaction>(url, JSON.stringify(transaction));
  }

  update(transaction: Transaction) {
    const url = `${this.transactionUrl}`;
    return this.http.put<Transaction>(url, JSON.stringify(transaction));
  }

  uploadCSV(file: File) {
    const formData = new FormData();
    formData.append('file', file, 'file.csv')
    const url = `${this.transactionUrl}/process/csv`;
    return this.http.post<any>(url, formData);
  }
}
