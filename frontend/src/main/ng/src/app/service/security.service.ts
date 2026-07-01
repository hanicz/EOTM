import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { finalize, shareReplay } from 'rxjs/operators';
import { SecurityTransaction } from '../model/securityTransaction';
import { ResourceHelper } from '../util/servicehelper';
import { environment } from '../../environments/environment';
import { Security } from '../model/security';

@Injectable({
  providedIn: 'root'
})
export class SecurityService {

  private helper = new ResourceHelper();

  private securityUrl = `${environment.API_URL}/api/v1/security`;
  private transactionUrl = `${this.securityUrl}/transaction`;

  private allSecuritiesRequest$: Observable<Security[]> | null = null;

  constructor(private http: HttpClient) { }

  getAllSecurities() {
    if (!this.allSecuritiesRequest$) {
      this.allSecuritiesRequest$ = this.http.get<Security[]>(this.securityUrl, {
        headers: this.helper.getHeadersWithToken()
      }).pipe(
        shareReplay(1),
        finalize(() => this.allSecuritiesRequest$ = null)
      );
    }
    return this.allSecuritiesRequest$;
  };

  getHolding() {
    const url = `${this.transactionUrl}/holding`;
    return this.http.get<SecurityTransaction[]>(url, {
      headers: this.helper.getHeadersWithToken()
    });
  };

  getTransactions() {
    return this.http.get<SecurityTransaction[]>(this.transactionUrl, {
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

  create(transaction: SecurityTransaction) {
    return this.http.post<SecurityTransaction>(this.transactionUrl, JSON.stringify(transaction), {
      headers: this.helper.getHeadersWithToken(),
    });
  }

  update(transaction: SecurityTransaction) {
    return this.http.put<SecurityTransaction>(this.transactionUrl, JSON.stringify(transaction), {
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
