import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MessageService } from 'primeng/api';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { AlertComponent } from './alert.component';
import { environment } from '../../environments/environment';

describe('AlertComponent', () => {
  let component: AlertComponent;
  let fixture: ComponentFixture<AlertComponent>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AlertComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations(), MessageService]
    })
      .compileComponents();

    fixture = TestBed.createComponent(AlertComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('loads the report subscription only when the report tab is opened', () => {
    http.expectNone(`${environment.API_URL}/api/v1/report/monthly/subscription`);

    component.tabChanged('2');

    const request = http.expectOne(`${environment.API_URL}/api/v1/report/monthly/subscription`);
    request.flush({ enabled: true, currency: 'EUR', recipients: ['partner@test.test'] });

    expect(component.report.enabled).toBeTrue();
    expect(component.report.recipients).toEqual(['partner@test.test']);
    expect(component.reportLoading).toBeFalse();
  });

  it('does not reload the subscription on a second visit to the report tab', () => {
    component.tabChanged('2');
    http.expectOne(`${environment.API_URL}/api/v1/report/monthly/subscription`)
      .flush({ enabled: false, currency: 'HUF', recipients: [] });

    component.tabChanged('2');

    http.expectNone(`${environment.API_URL}/api/v1/report/monthly/subscription`);
  });

  it('adds a recipient and rejects duplicates, bad addresses and overflow', () => {
    component.report = { enabled: true, currency: 'EUR', recipients: [] };

    component.newRecipient = 'not-an-email';
    expect(component.canAddRecipient).toBeFalse();

    component.newRecipient = ' Partner@Test.test ';
    expect(component.canAddRecipient).toBeTrue();
    component.addRecipient();
    expect(component.report.recipients).toEqual(['partner@test.test']);
    expect(component.newRecipient).toBe('');

    component.newRecipient = 'partner@test.test';
    expect(component.canAddRecipient).toBeFalse();

    component.report.recipients = ['a@t.test', 'b@t.test', 'c@t.test', 'd@t.test', 'e@t.test'];
    component.newRecipient = 'f@t.test';
    expect(component.canAddRecipient).toBeFalse();
  });

  it('removes a recipient', () => {
    component.report = { enabled: true, currency: 'EUR', recipients: ['a@t.test', 'b@t.test'] };

    component.removeRecipient('a@t.test');

    expect(component.report.recipients).toEqual(['b@t.test']);
  });

  it('saves the subscription', () => {
    component.report = { enabled: true, currency: 'HUF', recipients: ['partner@test.test'] };

    component.saveReport();

    const request = http.expectOne(`${environment.API_URL}/api/v1/report/monthly/subscription`);
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual(component.report);
    request.flush({ enabled: true, currency: 'HUF', recipients: ['partner@test.test'] });

    expect(component.reportSaving).toBeFalse();
  });

  it('triggers a manual send', () => {
    component.sendReportNow();

    const request = http.expectOne(`${environment.API_URL}/api/v1/report/monthly/send`);
    expect(request.request.method).toBe('POST');
    request.flush({});

    expect(component.reportSending).toBeFalse();
  });
});
