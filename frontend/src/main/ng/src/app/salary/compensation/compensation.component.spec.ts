import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MessageService } from 'primeng/api';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { SalaryCompensationComponent } from './compensation.component';
import { CompensationItem, CompensationPackage } from '../../model/compensation';
import { Globals } from '../../util/global';
import { environment } from '../../../environments/environment';

describe('SalaryCompensationComponent', () => {
  let component: SalaryCompensationComponent;
  let fixture: ComponentFixture<SalaryCompensationComponent>;
  let http: HttpTestingController;

  const compensationUrl = `${environment.API_URL}/api/v1/history/compensation`;

  const bonus: CompensationItem = {
    id: 11,
    name: 'Annual bonus',
    amountMode: 'PERCENT_OF_ANNUAL',
    monthlyAmount: null,
    percent: 15,
    taxTreatment: 'TAXED_AS_SALARY',
    note: null,
    currencyId: 'HUF',
    grossMonthly: 90000,
    grossAnnual: 1080000,
    netMonthly: 59850,
    netAnnual: 718200
  };

  const szepCard: CompensationItem = {
    id: 12,
    name: 'SZEP card',
    amountMode: 'MONTHLY_AMOUNT',
    monthlyAmount: 37500,
    percent: null,
    taxTreatment: 'RECEIVED_NET',
    note: null,
    currencyId: 'HUF',
    grossMonthly: null,
    grossAnnual: null,
    netMonthly: 37500,
    netAnnual: 450000
  };

  const current: CompensationPackage = {
    label: 'Sample role',
    currencyId: 'HUF',
    basis: 'MONTHLY',
    dependents: 0,
    validFrom: '2024-06-01',
    validTo: null,
    baseGrossMonthly: 600000,
    baseGrossAnnual: 7200000,
    baseNetMonthly: 399000,
    baseNetAnnual: 4788000,
    items: [bonus, szepCard],
    totalGrossMonthly: 690000,
    totalGrossAnnual: 8280000,
    totalNetMonthly: 496350,
    totalNetAnnual: 5956200,
  };

  const offer: CompensationPackage = {
    ...current,
    label: 'The offer',
    validFrom: null,
    baseGrossMonthly: 800000,
    baseGrossAnnual: 9600000,
    baseNetMonthly: 532000,
    baseNetAnnual: 6384000,
    items: [],
    totalGrossMonthly: 800000,
    totalGrossAnnual: 9600000,
    totalNetMonthly: 532000,
    totalNetAnnual: 6384000,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SalaryCompensationComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations(), MessageService, Globals]
    })
      .compileComponents();

    fixture = TestBed.createComponent(SalaryCompensationComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  it('loads the package on start', () => {
    http.expectOne(compensationUrl).flush(current);
    fixture.detectChanges();

    expect(component.current?.items.length).toBe(2);
    expect(component.current?.totalNetMonthly).toBe(496350);
  });

  it('puts the base salary in front of the items as its own row', () => {
    http.expectOne(compensationUrl).flush(current);

    const rows = component.rowsOf(current);

    expect(rows.length).toBe(3);
    expect(rows[0].base).toBeTruthy();
    expect(rows[0].grossMonthly).toBe(600000);
    expect(rows[1].id).toBe(11);
  });

  it('posts a new item and sends only the figure its mode uses', () => {
    http.expectOne(compensationUrl).flush(current);

    component.openNew();
    component.item.name = '  Annual bonus  ';
    component.item.amountMode = 'PERCENT_OF_ANNUAL';
    component.item.percent = 15;
    component.saveItem();

    const request = http.expectOne(compensationUrl);
    expect(request.request.method).toBe('POST');
    expect(JSON.parse(request.request.body)).toEqual({
      name: 'Annual bonus',
      amountMode: 'PERCENT_OF_ANNUAL',
      monthlyAmount: null,
      percent: 15,
      taxTreatment: 'TAXED_AS_SALARY',
      note: null
    });

    request.flush(bonus);
    http.expectOne(compensationUrl).flush(current);
    expect(component.itemDialog).toBeFalsy();
  });

  it('puts an edited item to its own url', () => {
    http.expectOne(compensationUrl).flush(current);

    component.editItem(szepCard);
    component.item.monthlyAmount = 40000;
    component.saveItem();

    const request = http.expectOne(`${compensationUrl}/12`);
    expect(request.request.method).toBe('PUT');
    expect(JSON.parse(request.request.body).monthlyAmount).toBe(40000);

    request.flush(szepCard);
    http.expectOne(compensationUrl).flush(current);
  });

  it('clears the figure the other mode used when the mode changes', () => {
    http.expectOne(compensationUrl).flush(current);

    component.editItem(szepCard);
    component.item.amountMode = 'PERCENT_OF_ANNUAL';
    component.modeChanged(component.item);

    expect(component.item.monthlyAmount).toBeNull();

    component.item.percent = 15;
    component.item.amountMode = 'MONTHLY_AMOUNT';
    component.modeChanged(component.item);

    expect(component.item.percent).toBeNull();
  });

  it('does not save an item without a name or the figure its mode needs', () => {
    http.expectOne(compensationUrl).flush(current);

    component.openNew();
    component.item.monthlyAmount = 8000;
    component.saveItem();

    http.expectNone(compensationUrl);

    component.item.name = '   ';
    expect(component.incomplete(component.item)).toBeTruthy();

    component.item.name = 'Phone';
    expect(component.incomplete(component.item)).toBeFalsy();
  });

  it('deletes the selected items in one call', () => {
    http.expectOne(compensationUrl).flush(current);

    component.selectedItems = [bonus, szepCard];
    component.deleteClicked();

    const request = http.expectOne(`${compensationUrl}?ids=11,12`);
    expect(request.request.method).toBe('DELETE');

    request.flush({});
    http.expectOne(compensationUrl).flush(current);
    expect(component.selectedItems.length).toBe(0);
  });

  it('copies the current package into a draft, keeping a bonus as a percentage', () => {
    http.expectOne(compensationUrl).flush(current);

    component.copyFromCurrent();

    expect(component.draft?.baseAmount).toBe(7200000);
    expect(component.draft?.baseBasis).toBe('ANNUAL');
    expect(component.draft?.items.length).toBe(2);
    expect(component.draft?.items[0].name).toBe('Annual bonus');
    expect(component.draft?.items[0].amountMode).toBe('PERCENT_OF_ANNUAL');
    expect(component.draft?.items[0].percent).toBe(15);
  });

  it('posts the draft to be priced and never saves it', () => {
    http.expectOne(compensationUrl).flush(current);

    component.startDraft();
    component.draft!.label = 'The offer';
    component.draft!.baseAmount = 9600000;
    component.compare();

    const request = http.expectOne(`${compensationUrl}/compare`);
    expect(request.request.method).toBe('POST');
    expect(JSON.parse(request.request.body)).toEqual({
      label: 'The offer',
      baseAmount: 9600000,
      baseBasis: 'ANNUAL',
      currencyId: 'HUF',
      dependents: 0,
      items: []
    });

    request.flush(offer);
    http.verify();
    expect(component.compared?.totalNetMonthly).toBe(532000);
  });

  it('works out the difference between the two packages', () => {
    http.expectOne(compensationUrl).flush(current);
    component.compared = offer;

    const rows = component.comparisonRows();
    const totalNetAnnual = rows.find(row => row.label === 'Total net annual')!;

    expect(component.comparable()).toBeTruthy();
    expect(component.difference(totalNetAnnual)).toBe(427800);
    expect(component.differencePercent(totalNetAnnual)).toBeCloseTo(7.182, 3);
  });

  it('refuses to subtract packages kept in different currencies', () => {
    http.expectOne(compensationUrl).flush(current);
    component.compared = { ...offer, currencyId: 'EUR' };

    expect(component.comparable()).toBeFalsy();
  });

  it('will not compare a draft whose items are missing their figures', () => {
    http.expectOne(compensationUrl).flush(current);

    component.startDraft();
    component.draft!.baseAmount = 9600000;
    component.addDraftItem();

    expect(component.draftReady()).toBeFalsy();

    component.draft!.items[0].monthlyAmount = 8000;

    expect(component.draftReady()).toBeFalsy();

    component.draft!.items[0].name = 'Phone';

    expect(component.draftReady()).toBeTruthy();
  });

  it('drops the draft and its result when cleared', () => {
    http.expectOne(compensationUrl).flush(current);

    component.startDraft();
    component.compared = offer;
    component.clearDraft();

    expect(component.draft).toBeNull();
    expect(component.compared).toBeNull();
  });
});
