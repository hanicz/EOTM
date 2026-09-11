import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BankTransaction, ExclusionRule, MonthlyCashFlow, MonthlyIncome } from '../model/bankTransaction';
import { ImportResult } from '../model/importResult';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class FinancialService {

  private transactionUrl = `${environment.API_URL}/api/v1/financial/transaction`;
  private ruleUrl = `${environment.API_URL}/api/v1/financial/rule`;

  constructor(private http: HttpClient) { }

  getTransactions() {
    return this.http.get<BankTransaction[]>(this.transactionUrl);
  }

  getMonthlyCashFlow() {
    const url = `${this.transactionUrl}/report/monthly`;
    return this.http.get<MonthlyCashFlow[]>(url);
  }

  downloadMonthlyCashFlow() {
    const url = `${this.transactionUrl}/report/monthly/csv`;
    return this.http.get(url, {
      responseType: 'blob'
    });
  }

  getMonthlyIncome() {
    const url = `${this.transactionUrl}/report/income`;
    return this.http.get<MonthlyIncome[]>(url);
  }

  downloadMonthlyIncome() {
    const url = `${this.transactionUrl}/report/income/csv`;
    return this.http.get(url, {
      responseType: 'blob'
    });
  }

  setExcluded(ids: string, excluded: boolean) {
    const url = `${this.transactionUrl}/exclusion?ids=${ids}&excluded=${excluded}`;
    return this.http.put(url, null);
  }

  setTaxable(ids: string, taxable: boolean) {
    const url = `${this.transactionUrl}/taxable?ids=${ids}&taxable=${taxable}`;
    return this.http.put(url, null);
  }

  updateTransaction(id: number, bookingDate: string, memo: string) {
    const url = `${this.transactionUrl}/${id}`;
    return this.http.put(url, { bookingDate, memo });
  }

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

  uploadCSV(file: File) {
    const formData = new FormData();
    formData.append('file', file, 'file.csv');
    const url = `${this.transactionUrl}/process/csv`;
    return this.http.post<ImportResult>(url, formData);
  }

  getRules() {
    return this.http.get<ExclusionRule[]>(this.ruleUrl);
  }

  createRule(rule: ExclusionRule) {
    return this.http.post<ExclusionRule>(this.ruleUrl, JSON.stringify(this.toRulePayload(rule)));
  }

  updateRule(rule: ExclusionRule) {
    const url = `${this.ruleUrl}/${rule.id}`;
    return this.http.put<ExclusionRule>(url, JSON.stringify(this.toRulePayload(rule)));
  }

  private toRulePayload(rule: ExclusionRule) {
    return {
      name: rule.name,
      accountNumber: rule.accountNumber,
      side: rule.side,
      active: rule.active
    };
  }

  deleteRulesByIds(ids: string) {
    const url = `${this.ruleUrl}?ids=${ids}`;
    return this.http.delete(url);
  }
}
