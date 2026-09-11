import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { finalize, shareReplay } from 'rxjs/operators';
import { SecurityTransaction } from '../model/securityTransaction';
import { environment } from '../../environments/environment';
import { Security } from '../model/security';
import { ImportResult } from '../model/importResult';

@Injectable({
  providedIn: 'root'
})
export class SecurityService {

  private securityUrl = `${environment.API_URL}/api/v1/security`;
  private transactionUrl = `${this.securityUrl}/transaction`;

  private allSecuritiesRequest$: Observable<Security[]> | null = null;

  constructor(private http: HttpClient) { }

  getAllSecurities() {
    if (!this.allSecuritiesRequest$) {
      this.allSecuritiesRequest$ = this.http.get<Security[]>(this.securityUrl).pipe(
        shareReplay(1),
        finalize(() => this.allSecuritiesRequest$ = null)
      );
    }
    return this.allSecuritiesRequest$;
  };

  getHolding() {
    const url = `${this.transactionUrl}/holding`;
    return this.http.get<SecurityTransaction[]>(url);
  };

  refreshRates() {
    const url = `${this.securityUrl}/rate/refresh`;
    return this.http.post(url, null);
  };

  getTransactions() {
    return this.http.get<SecurityTransaction[]>(this.transactionUrl);
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

  create(transaction: SecurityTransaction) {
    return this.http.post<SecurityTransaction>(this.transactionUrl, JSON.stringify(transaction));
  }

  update(transaction: SecurityTransaction) {
    return this.http.put<SecurityTransaction>(this.transactionUrl, JSON.stringify(transaction));
  }

  uploadCSV(file: File) {
    const formData = new FormData();
    formData.append('file', file, 'file.csv')
    const url = `${this.transactionUrl}/process/csv`;
    return this.http.post<any>(url, formData);
  }

  uploadXls(file: File) {
    const formData = new FormData();
    formData.append('file', file, file.name);
    const url = `${this.securityUrl}/process/xls`;
    return this.http.post<ImportResult>(url, formData);
  }
}
