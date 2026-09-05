import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MessageService } from 'primeng/api';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { SalaryHistoryComponent } from './history.component';
import { Salary } from '../../model/salary';
import { Globals } from '../../util/global';
import { environment } from '../../../environments/environment';

describe('SalaryHistoryComponent', () => {
  let component: SalaryHistoryComponent;
  let fixture: ComponentFixture<SalaryHistoryComponent>;
  let http: HttpTestingController;

  const salaryUrl = `${environment.API_URL}/api/v1/history/salary`;

  const current: Salary = {
    id: 1,
    amount: 600000,
    basis: 'MONTHLY',
    currencyId: 'HUF',
    validFrom: '2024-06-01',
    validTo: null,
    dependents: 2,
    note: 'Sample role',
    grossMonthly: 600000,
    grossAnnual: 7200000,
    netMonthly: 478998,
    netAnnual: 5747976,
    familyAllowanceApplied: true
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SalaryHistoryComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations(), MessageService, Globals]
    })
      .compileComponents();

    fixture = TestBed.createComponent(SalaryHistoryComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  it('loads the salaries on start', () => {
    http.expectOne(salaryUrl).flush([current]);
    fixture.detectChanges();

    expect(component.salaries.length).toBe(1);
    expect(component.salaries[0].netMonthly).toBe(478998);
  });

  it('posts a new salary and drops the empty end date', () => {
    http.expectOne(salaryUrl).flush([]);

    component.openNew();
    component.salary.amount = 900000;
    component.salary.validFrom = '2026-01-01';
    component.salary.note = '  Sample role  ';
    component.saveSalary();

    const request = http.expectOne(salaryUrl);
    expect(request.request.method).toBe('POST');
    expect(JSON.parse(request.request.body)).toEqual({
      amount: 900000,
      basis: 'MONTHLY',
      currencyId: 'HUF',
      validFrom: '2026-01-01',
      validTo: null,
      dependents: 0,
      note: 'Sample role'
    });

    request.flush(current);
    http.expectOne(salaryUrl).flush([current]);
    expect(component.salaryDialog).toBeFalsy();
  });

  it('puts an edited salary to its own url', () => {
    http.expectOne(salaryUrl).flush([current]);

    component.editSalary(current);
    component.salary.dependents = 3;
    component.saveSalary();

    const request = http.expectOne(`${salaryUrl}/1`);
    expect(request.request.method).toBe('PUT');
    expect(JSON.parse(request.request.body).dependents).toBe(3);

    request.flush(current);
    http.expectOne(salaryUrl).flush([current]);
  });

  it('does not save without an amount or a start date', () => {
    http.expectOne(salaryUrl).flush([]);

    component.openNew();
    component.saveSalary();

    http.expectNone(salaryUrl);
  });

  it('deletes the selected salaries in one call', () => {
    http.expectOne(salaryUrl).flush([current]);

    component.selectedSalaries = [current];
    component.deleteClicked();

    const request = http.expectOne(`${salaryUrl}?ids=1`);
    expect(request.request.method).toBe('DELETE');

    request.flush({});
    http.expectOne(salaryUrl).flush([]);
    expect(component.selectedSalaries.length).toBe(0);
  });

  it('works the raise out from the period before it, whatever order the rows arrive in', () => {
    const latest: Salary = { ...current, netMonthly: 600000 };
    const older: Salary = { ...current, id: 2, validFrom: '2021-03-01', netMonthly: 400000 };
    const newer: Salary = { ...current, id: 3, validFrom: '2023-01-01', netMonthly: 500000 };
    http.expectOne(salaryUrl).flush([newer, latest, older]);

    const byId = new Map(component.salaries.map(s => [s.id, s]));
    expect(byId.get(2)!.raiseAmount).toBeNull();
    expect(byId.get(2)!.raisePercent).toBeNull();
    expect(byId.get(3)!.raiseAmount).toBe(100000);
    expect(byId.get(3)!.raisePercent).toBe(25);
    expect(byId.get(1)!.raiseAmount).toBe(100000);
    expect(byId.get(1)!.raisePercent).toBe(20);
  });

  it('reports a pay cut as a negative raise', () => {
    const older: Salary = { ...current, id: 2, validFrom: '2021-03-01', netMonthly: 800000 };
    http.expectOne(salaryUrl).flush([{ ...current, netMonthly: 600000 }, older]);

    const latest = component.salaries.find(s => s.id === 1)!;
    expect(latest.raiseAmount).toBe(-200000);
    expect(latest.raisePercent).toBe(-25);
  });

  it('leaves the raise empty when the currency changed', () => {
    const older: Salary = { ...current, id: 2, validFrom: '2021-03-01', currencyId: 'EUR', netMonthly: 1995 };
    http.expectOne(salaryUrl).flush([current, older]);

    expect(component.salaries.find(s => s.id === 1)!.raiseAmount).toBeNull();
  });

  it('flags a row whose dependants earned no allowance', () => {
    const inEuro: Salary = { ...current, currencyId: 'EUR', familyAllowanceApplied: false };
    http.expectOne(salaryUrl).flush([inEuro]);

    expect(component.allowanceMissed(inEuro)).toBeTruthy();
    expect(component.allowanceMissed(current)).toBeFalsy();
    expect(component.allowanceMissed({ ...current, dependents: 0, familyAllowanceApplied: false })).toBeFalsy();
  });
});
