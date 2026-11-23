import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Salary, SalaryRaise } from '../model/salary';
import { PensionProjection, PensionProjectionInput } from '../model/pension-projection';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class SalaryService {

  private salaryUrl = `${environment.API_URL}/api/v1/history/salary`;

  constructor(private http: HttpClient) { }

  getSalaries() {
    return this.http.get<Salary[]>(this.salaryUrl);
  }

  getRaise() {
    return this.http.get<SalaryRaise>(`${this.salaryUrl}/raise`);
  }

  create(salary: Salary) {
    return this.http.post<Salary>(this.salaryUrl, JSON.stringify(this.toPayload(salary)));
  }

  update(salary: Salary) {
    const url = `${this.salaryUrl}/${salary.id}`;
    return this.http.put<Salary>(url, JSON.stringify(this.toPayload(salary)));
  }

  projectPension(input: PensionProjectionInput) {
    return this.http.post<PensionProjection>(`${this.salaryUrl}/pension`, JSON.stringify(input));
  }

  downloadPensionCsv(input: PensionProjectionInput) {
    return this.http.post(`${this.salaryUrl}/pension/csv`, JSON.stringify(input), {
      responseType: 'blob'
    });
  }

  deleteByIds(ids: string) {
    const url = `${this.salaryUrl}?ids=${ids}`;
    return this.http.delete(url);
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
