import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MessageService } from 'primeng/api';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { SalaryRaiseComponent } from './raise.component';
import { SalaryRaise } from '../../model/salary';
import { Globals } from '../../util/global';
import { environment } from '../../../environments/environment';

describe('SalaryRaiseComponent', () => {
  let component: SalaryRaiseComponent;
  let fixture: ComponentFixture<SalaryRaiseComponent>;
  let http: HttpTestingController;

  const raiseUrl = `${environment.API_URL}/api/v1/history/salary/raise`;
  const ratesUrl = `${environment.API_URL}/api/v1/dashboard/rates?currencies=USD&currencies=HUF`;

  const raise: SalaryRaise = {
    current: {
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
    },
    scenarios: [
      { percent: 2, grossMonthly: 612000, grossAnnual: 7344000, netMonthly: 486958, netAnnual: 5843496 },
      { percent: 5, grossMonthly: 630000, grossAnnual: 7560000, netMonthly: 498898, netAnnual: 5986776 },
      { percent: 10, grossMonthly: 660000, grossAnnual: 7920000, netMonthly: 518798, netAnnual: 6225576 },
      { percent: 20, grossMonthly: 720000, grossAnnual: 8640000, netMonthly: 558598, netAnnual: 6703176 },
      { percent: 25, grossMonthly: 750000, grossAnnual: 9000000, netMonthly: 578498, netAnnual: 6941976 }
    ]
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SalaryRaiseComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations(), MessageService, Globals]
    })
      .compileComponents();

    fixture = TestBed.createComponent(SalaryRaiseComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  it('loads the current salary with its scenarios on start', () => {
    http.expectOne(raiseUrl).flush(raise);
    http.expectOne(ratesUrl).flush({ rates: { USD: 1.08, HUF: 395 } });
    fixture.detectChanges();

    expect(component.raise!.current.grossAnnual).toBe(7200000);
    expect(component.raise!.scenarios.length).toBe(5);
    expect(component.raise!.scenarios.map(s => s.percent)).toEqual([2, 5, 10, 20, 25]);
  });

  it('converts the annual gross to dollars and euros', () => {
    http.expectOne(raiseUrl).flush(raise);
    http.expectOne(ratesUrl).flush({ rates: { USD: 1.08, HUF: 400 } });

    expect(component.grossAnnualUsd).toBeCloseTo(19440, 2);
    expect(component.grossAnnualEur).toBeCloseTo(18000, 2);
  });

  it('asks only for the rates it is missing', () => {
    const inEuro: SalaryRaise = { ...raise, current: { ...raise.current, currencyId: 'EUR' } };
    http.expectOne(raiseUrl).flush(inEuro);

    http.expectOne(`${environment.API_URL}/api/v1/dashboard/rates?currencies=USD`)
      .flush({ rates: { USD: 1.08 } });

    expect(component.grossAnnualEur).toBe(7200000);
    expect(component.grossAnnualUsd).toBeCloseTo(7776000, 2);
  });

  it('leaves the converted figures empty when a rate is missing', () => {
    http.expectOne(raiseUrl).flush(raise);
    http.expectOne(ratesUrl).flush({ rates: { USD: 1.08 } });

    expect(component.grossAnnualUsd).toBeNull();
    expect(component.grossAnnualEur).toBeNull();
  });

  it('shows nothing to convert when no salary has been recorded', () => {
    http.expectOne(raiseUrl).flush(null, { status: 204, statusText: 'No Content' });

    expect(component.raise).toBeNull();
    expect(component.loaded).toBe(true);
    http.expectNone(ratesUrl);
  });
});
