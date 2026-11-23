import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MessageService } from 'primeng/api';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { PensionComponent } from './pension.component';
import { Globals } from '../util/global';
import { environment } from '../../environments/environment';

describe('PensionComponent', () => {
  let component: PensionComponent;
  let fixture: ComponentFixture<PensionComponent>;
  let http: HttpTestingController;

  const pensionUrl = `${environment.API_URL}/api/v1/pension`;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PensionComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations(), MessageService, Globals]
    })
      .compileComponents();

    fixture = TestBed.createComponent(PensionComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  it('should create', () => {
    http.expectOne(pensionUrl).flush({ totalContribution: 0, currentValue: 0, currency: 'HUF' });
    fixture.detectChanges();

    expect(component).toBeTruthy();
  });

  it('picks up the values and the currency they are stored in', () => {
    http.expectOne(pensionUrl).flush({ totalContribution: 980000, currentValue: 1250000, currency: 'EUR' });
    fixture.detectChanges();

    expect(component.totalContribution).toBe(980000);
    expect(component.currentValue).toBe(1250000);
    expect(component.currency).toBe('EUR');
    expect(component.loading).toBeFalsy();
  });

  it('derives the gain from the two amounts', () => {
    http.expectOne(pensionUrl).flush({ totalContribution: 800000, currentValue: 1000000, currency: 'HUF' });

    expect(component.gain).toBe(200000);
    expect(component.gainPct).toBe(25);
  });

  it('reports no gain rather than dividing by zero before anything is contributed', () => {
    http.expectOne(pensionUrl).flush({ totalContribution: 0, currentValue: 0, currency: 'HUF' });

    expect(component.gain).toBe(0);
    expect(component.gainPct).toBe(0);
  });

  it('sends both amounts when saving', () => {
    http.expectOne(pensionUrl).flush({ totalContribution: 800000, currentValue: 1000000, currency: 'HUF' });

    component.currentValue = 1100000;
    component.save();

    const request = http.expectOne(pensionUrl);
    expect(request.request.method).toBe('PUT');
    expect(JSON.parse(request.request.body)).toEqual({
      totalContribution: 800000, currentValue: 1100000, currency: 'HUF'
    });

    request.flush({ totalContribution: 800000, currentValue: 1100000, currency: 'HUF' });
    expect(component.currentValue).toBe(1100000);
    expect(component.saving).toBeFalsy();
  });
});
