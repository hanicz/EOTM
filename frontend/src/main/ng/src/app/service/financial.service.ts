import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BankTransaction, ImportResult, MonthlyCashFlow, MonthlyIncome } from '../model/bankTransaction';
import { ResourceHelper } from '../util/servicehelper';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class FinancialService {

  private helper = new ResourceHelper();

  private transactionUrl = `${environment.API_URL}/api/v1/financial/transaction`;

  constructor(private http: HttpClient) { }

  getTransactions() {
    return this.http.get<BankTransaction[]>(this.transactionUrl, {
      headers: this.helper.getHeadersWithToken()
    });
  }

  getMonthlyCashFlow() {
    const url = `${this.transactionUrl}/report/monthly`;
    return this.http.get<MonthlyCashFlow[]>(url, {
      headers: this.helper.getHeadersWithToken()
    });
  }

  downloadMonthlyCashFlow() {
    const url = `${this.transactionUrl}/report/monthly/csv`;
    return this.http.get(url, {
      headers: this.helper.getHeadersWithToken(),
      responseType: 'blob'
    });
  }

  getMonthlyIncome() {
    const url = `${this.transactionUrl}/report/income`;
    return this.http.get<MonthlyIncome[]>(url, {
      headers: this.helper.getHeadersWithToken()
    });
  }

  downloadMonthlyIncome() {
    const url = `${this.transactionUrl}/report/income/csv`;
    return this.http.get(url, {
      headers: this.helper.getHeadersWithToken(),
      responseType: 'blob'
    });
  }

  setExcluded(ids: string, excluded: boolean) {
    const url = `${this.transactionUrl}/exclusion?ids=${ids}&excluded=${excluded}`;
    return this.http.put(url, null, {
      headers: this.helper.getHeadersWithToken()
    });
  }

  setTaxable(ids: string, taxable: boolean) {
    const url = `${this.transactionUrl}/taxable?ids=${ids}&taxable=${taxable}`;
    return this.http.put(url, null, {
      headers: this.helper.getHeadersWithToken()
    });
  }

  updateMemo(id: number, memo: string) {
    const url = `${this.transactionUrl}/${id}/memo`;
    return this.http.put(url, { memo }, {
      headers: this.helper.getHeadersWithToken()
    });
  }

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

  uploadCSV(file: File) {
    const formData = new FormData();
    formData.append('file', file, 'file.csv');
    const url = `${this.transactionUrl}/process/csv`;
    return this.http.post<ImportResult>(url, formData, {
      headers: this.helper.getAuthHeaders(),
    });
  }
}
