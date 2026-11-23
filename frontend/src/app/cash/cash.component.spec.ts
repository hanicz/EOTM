import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MessageService } from 'primeng/api';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { CashComponent } from './cash.component';
import { Globals } from '../util/global';
import { environment } from '../../environments/environment';

describe('CashComponent', () => {
  let component: CashComponent;
  let fixture: ComponentFixture<CashComponent>;
  let http: HttpTestingController;

  const cashUrl = `${environment.API_URL}/api/v1/cash`;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CashComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations(), MessageService, Globals]
    })
      .compileComponents();

    fixture = TestBed.createComponent(CashComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  it('should create', () => {
    http.expectOne(cashUrl).flush({ amount: 0, currency: 'HUF' });
    fixture.detectChanges();

    expect(component).toBeTruthy();
  });

  it('picks up the currency the balance is stored in', () => {
    http.expectOne(cashUrl).flush({ amount: 1500, currency: 'EUR' });
    fixture.detectChanges();

    expect(component.amount).toBe(1500);
    expect(component.currency).toBe('EUR');
    expect(component.loading).toBeFalsy();
  });

  it('saves the amount unchanged when the currency is switched', () => {
    http.expectOne(cashUrl).flush({ amount: 1500, currency: 'HUF' });

    component.currency = 'USD';
    component.save();

    const request = http.expectOne(cashUrl);
    expect(request.request.method).toBe('PUT');
    expect(JSON.parse(request.request.body)).toEqual({ amount: 1500, currency: 'USD' });

    request.flush({ amount: 1500, currency: 'USD' });
    expect(component.currency).toBe('USD');
    expect(component.saving).toBeFalsy();
  });
});
