import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BankTransaction, CategorizeResult, CategoryRule, ExclusionRule, MonthlyCashFlow, MonthlyCategorySpending, MonthlyIncome, SpendingCategory, YearlyCashFlow } from '../model/bankTransaction';
import { ImportResult } from '../model/importResult';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class FinancialService {

  private transactionUrl = `${environment.API_URL}/api/v1/financial/transaction`;
  private ruleUrl = `${environment.API_URL}/api/v1/financial/rule`;
  private categoryUrl = `${environment.API_URL}/api/v1/financial/category`;

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

  getCategories() {
    return this.http.get<SpendingCategory[]>(this.categoryUrl);
  }

  createCategory(category: SpendingCategory) {
    return this.http.post<SpendingCategory>(this.categoryUrl, JSON.stringify(this.toCategoryPayload(category)));
  }

  updateCategory(category: SpendingCategory) {
    const url = `${this.categoryUrl}/${category.id}`;
    return this.http.put<SpendingCategory>(url, JSON.stringify(this.toCategoryPayload(category)));
  }

  createStarterCategories() {
    const url = `${this.categoryUrl}/starter`;
    return this.http.post<SpendingCategory[]>(url, null);
  }

  deleteCategoriesByIds(ids: string) {
    const url = `${this.categoryUrl}?ids=${ids}`;
    return this.http.delete(url);
  }

  private toCategoryPayload(category: SpendingCategory) {
    return {
      name: category.name,
      color: category.color,
      position: category.position
    };
  }

  getCategoryRules() {
    const url = `${this.ruleUrl}/category`;
    return this.http.get<CategoryRule[]>(url);
  }

  createCategoryRule(rule: CategoryRule) {
    const url = `${this.ruleUrl}/category`;
    return this.http.post<CategoryRule>(url, JSON.stringify(this.toCategoryRulePayload(rule)));
  }

  updateCategoryRule(rule: CategoryRule) {
    const url = `${this.ruleUrl}/category/${rule.id}`;
    return this.http.put<CategoryRule>(url, JSON.stringify(this.toCategoryRulePayload(rule)));
  }

  deleteCategoryRulesByIds(ids: string) {
    const url = `${this.ruleUrl}/category?ids=${ids}`;
    return this.http.delete(url);
  }

  applyCategoryRules() {
    const url = `${this.ruleUrl}/category/apply`;
    return this.http.post<CategorizeResult>(url, null);
  }

  private toCategoryRulePayload(rule: CategoryRule) {
    return {
      name: rule.name,
      pattern: rule.pattern,
      categoryId: rule.categoryId,
      priority: rule.priority,
      active: rule.active
    };
  }

  setCategory(ids: string, categoryId: number | null) {
    const suffix = categoryId === null ? '' : `&categoryId=${categoryId}`;
    const url = `${this.transactionUrl}/category?ids=${ids}${suffix}`;
    return this.http.put(url, null);
  }

  getMonthlyCategorySpending() {
    const url = `${this.transactionUrl}/report/category`;
    return this.http.get<MonthlyCategorySpending[]>(url);
  }

  downloadMonthlyCategorySpending() {
    const url = `${this.transactionUrl}/report/category/csv`;
    return this.http.get(url, {
      responseType: 'blob'
    });
  }

  getYearlyCashFlow() {
    const url = `${this.transactionUrl}/report/yearly`;
    return this.http.get<YearlyCashFlow[]>(url);
  }

  downloadYearlyCashFlow() {
    const url = `${this.transactionUrl}/report/yearly/csv`;
    return this.http.get(url, {
      responseType: 'blob'
    });
  }
}
