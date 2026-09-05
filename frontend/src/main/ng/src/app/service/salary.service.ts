import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Salary } from '../model/salary';
import { ResourceHelper } from '../util/servicehelper';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class SalaryService {

  private helper = new ResourceHelper();

  private salaryUrl = `${environment.API_URL}/api/v1/history/salary`;

  constructor(private http: HttpClient) { }

  getSalaries() {
    return this.http.get<Salary[]>(this.salaryUrl, {
      headers: this.helper.getHeadersWithToken()
    });
  }

  create(salary: Salary) {
    return this.http.post<Salary>(this.salaryUrl, JSON.stringify(this.toPayload(salary)), {
      headers: this.helper.getHeadersWithToken()
    });
  }

  update(salary: Salary) {
    const url = `${this.salaryUrl}/${salary.id}`;
    return this.http.put<Salary>(url, JSON.stringify(this.toPayload(salary)), {
      headers: this.helper.getHeadersWithToken()
    });
  }

  deleteByIds(ids: string) {
    const url = `${this.salaryUrl}?ids=${ids}`;
    return this.http.delete(url, {
      headers: this.helper.getHeadersWithToken()
    });
  }

  private toPayload(salary: Salary) {
    return {
      amount: salary.amount,
      basis: salary.basis,
      currencyId: salary.currencyId,
      validFrom: salary.validFrom,
      validTo: salary.validTo ? salary.validTo : null,
      dependents: salary.dependents,
      note: salary.note
    };
  }
}
