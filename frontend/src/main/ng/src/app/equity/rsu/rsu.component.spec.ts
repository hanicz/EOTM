import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MessageService } from 'primeng/api';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { EquityRsuComponent } from './rsu.component';
import { RSUGrant, RSUGrantReport, RSUVest } from '../../model/equity';
import { environment } from '../../../environments/environment';

describe('EquityRsuComponent', () => {
  let component: EquityRsuComponent;
  let fixture: ComponentFixture<EquityRsuComponent>;
  let http: HttpTestingController;

  const rsuUrl = `${environment.API_URL}/api/v1/equity/rsu`;
  const exchangesUrl = `${environment.API_URL}/api/v1/stock/exchanges`;

  const vest = (sequence: number, vestDate: string, vested: boolean): RSUVest => ({
    grantId: 1,
    shortName: 'ACME',
    exchange: 'US',
    grantDate: '2021-03-01',
    sequence,
    vestDate,
    quantity: 100,
    currency: 'USD',
    price: 100,
    priceDate: vestDate,
    amount: 10000,
    rate: 350,
    rateDate: vestDate,
    amountInHuf: 3500000,
    netInHuf: 2555000,
    vested,
    projected: !vested,
    tax: { amount: 3500000, taxBase: 3115000, szocho: 404950, szja: 467250, total: 872200 }
  });

  const grant: RSUGrant = {
    id: 1,
    shortName: 'ACME',
    exchange: 'US',
    currency: null,
    grantDate: '2021-03-01',
    quantity: 400,
    vestingYears: 4,
    vestingFrequency: 'ANNUAL',
    note: 'Joining grant',
    vests: [vest(1, '2022-03-01', true), vest(2, '2023-03-01', true),
      vest(3, '2024-03-01', true), vest(4, '2025-03-01', false)],
    totalAmountInHuf: 14000000,
    totalTax: { amount: 14000000, taxBase: 12460000, szocho: 1619800, szja: 1869000, total: 3488800 },
    totalNetInHuf: 10220000,
    vestedNetInHuf: 7665000,
    upcomingNetInHuf: 2555000,
    error: null
  };

  const report: RSUGrantReport = {
    items: [grant],
    totalAmountInHuf: 14000000,
    totalTax: grant.totalTax!,
    totalNetInHuf: 10220000
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EquityRsuComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations(), MessageService]
    })
      .compileComponents();

    fixture = TestBed.createComponent(EquityRsuComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  const settle = (body: RSUGrantReport) => {
    http.expectOne(rsuUrl).flush(body);
    http.expectOne(exchangesUrl).flush([]);
  };

  it('loads the grants and their vesting schedule on start', () => {
    settle(report);
    fixture.detectChanges();

    expect(component.report!.items.length).toBe(1);
    expect(component.report!.items[0].vests!.length).toBe(4);
    expect(component.report!.items[0].vests![3].projected).toBeTruthy();
    expect(component.loading).toBeFalsy();
  });

  it('describes the schedule in words', () => {
    settle(report);

    expect(component.scheduleLabel(grant)).toBe('4y × annual');
    expect(component.scheduleLabel({ ...grant, vestingYears: 2, vestingFrequency: 'QUARTERLY' }))
      .toBe('2y × quarterly');
  });

  it('posts a new grant and trims away the empty note', () => {
    settle({ ...report, items: [] });

    component.openNew();
    component.grant.shortName = 'WIDGET';
    component.grant.grantDate = '2026-01-01';
    component.grant.quantity = 401;
    component.grant.note = '   ';
    component.saveGrant();

    const request = http.expectOne(rsuUrl);
    expect(request.request.method).toBe('POST');
    expect(JSON.parse(request.request.body)).toEqual({
      shortName: 'WIDGET',
      exchange: 'US',
      currency: null,
      grantDate: '2026-01-01',
      quantity: 401,
      vestingYears: 4,
      vestingFrequency: 'ANNUAL',
      note: null
    });

    request.flush(grant);
    http.expectOne(rsuUrl).flush(report);
    expect(component.grantDialog).toBeFalsy();
  });

  it('puts an edited grant to its own url', () => {
    settle(report);

    component.editGrant(grant);
    component.grant.vestingYears = 2;
    component.grant.vestingFrequency = 'QUARTERLY';
    component.saveGrant();

    const request = http.expectOne(`${rsuUrl}/1`);
    expect(request.request.method).toBe('PUT');
    expect(JSON.parse(request.request.body).vestingFrequency).toBe('QUARTERLY');

    request.flush(grant);
    http.expectOne(rsuUrl).flush(report);
  });

  it('does not save without a ticker, a grant date or a share count', () => {
    settle({ ...report, items: [] });

    component.openNew();
    component.saveGrant();

    http.expectNone(rsuUrl);
  });

  it('deletes the selected grants in one call and reloads', () => {
    settle(report);

    component.selectedGrants = [grant];
    component.deleteClicked();

    const request = http.expectOne(`${rsuUrl}?ids=1`);
    expect(request.request.method).toBe('DELETE');

    request.flush({});
    http.expectOne(rsuUrl).flush({ ...report, items: [] });
    expect(component.selectedGrants.length).toBe(0);
  });

  it('keeps the page usable when a grant could not be priced', () => {
    settle({ ...report, items: [{ ...grant, error: 'Could not value NOPE.US' }] });

    expect(component.report!.items[0].error).toBe('Could not value NOPE.US');
    expect(component.loading).toBeFalsy();
  });
});
