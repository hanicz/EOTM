import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { FireSummaryComponent } from './fire-summary.component';
import { FireSnapshot } from '../../model/fire';
import { environment } from '../../../environments/environment';

describe('FireSummaryComponent', () => {
  let component: FireSummaryComponent;
  let fixture: ComponentFixture<FireSummaryComponent>;
  let http: HttpTestingController;

  const userUrl = `${environment.API_URL}/api/v1/user/me`;
  const snapshotUrl = `${environment.API_URL}/api/v1/fire/snapshot?currency=HUF`;

  const snapshot = (overrides: Partial<FireSnapshot> = {}): FireSnapshot => ({
    currency: 'HUF',
    netWorth: 90000000,
    fireNumber: 450000000,
    targetSource: 'FIXED',
    progressPct: 20,
    yearsToFire: 25,
    fiReached: false,
    monthlyIncome: 2000000,
    monthlySpending: 1200000,
    monthlySavings: 800000,
    savingsRatePct: 40,
    withdrawalRate: 3,
    annualReturn: 5,
    annualContributionIncrease: 2.5,
    inflation: 3,
    horizonYears: 90,
    hasCashFlow: true,
    monthsCounted: 3,
    windowStart: '2026-01',
    windowEnd: '2026-03',
    ignoredCurrencies: [],
    unconvertedCurrencies: [],
    ...overrides
  });

  const load = (data: Partial<FireSnapshot> = {}) => {
    fixture.detectChanges();
    http.expectOne(userUrl).flush({ preferredCurrency: 'HUF' });
    http.expectOne(snapshotUrl).flush(snapshot(data));
    fixture.detectChanges();
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FireSummaryComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations()]
    })
      .compileComponents();

    fixture = TestBed.createComponent(FireSummaryComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('reads the snapshot in the preferred currency', () => {
    load();

    expect(component.loading()).toBeFalsy();
    expect(component.currency()).toBe('HUF');
    expect(component.snapshot()?.fireNumber).toBe(450000000);
    expect(component.yearsLabel()).toBe('25 yrs');
  });

  it('clamps the progress bar when the target is already passed', () => {
    load({ progressPct: 140, fiReached: true, yearsToFire: 0 });

    expect(component.barWidth()).toBe(100);
    expect(component.yearsLabel()).toBe('Reached');
  });

  it('keeps the bar off the scale when progress is negative', () => {
    load({ progressPct: -5 });

    expect(component.barWidth()).toBe(0);
  });

  it('says so when the target is out of reach', () => {
    load({ yearsToFire: null, monthlySavings: -500000, savingsRatePct: -25 });

    expect(component.yearsLabel()).toBe('Not on track');
    expect(component.notes()).toContain('Spending more than you earn');
  });

  it('prompts for an import when there is no cash flow', () => {
    load({ hasCashFlow: false, monthsCounted: 0, yearsToFire: null, monthlySavings: 0 });

    expect(component.yearsLabel()).toBe('—');
    expect(component.snapshot()?.fireNumber).toBe(450000000);
    expect(fixture.nativeElement.textContent).toContain('Import bank transactions');
  });

  it('notes a partial window', () => {
    load({ monthsCounted: 2 });

    expect(component.notes()).toContain('Based on 2 months');
  });

  it('notes a target that was derived rather than fixed', () => {
    load({ targetSource: 'DERIVED' });

    expect(component.notes()).toContain('Target derived from spending');
  });

  it('names the currencies left out of the figures', () => {
    load({ ignoredCurrencies: ['EUR'], unconvertedCurrencies: ['GBP'] });

    expect(component.notes()).toContain('Excludes EUR, GBP');
  });

  it('shows no label when there is no target to measure against', () => {
    load({ fireNumber: null, progressPct: null, targetSource: null, yearsToFire: null });

    expect(component.yearsLabel()).toBe('—');
  });

  it('stops loading when the snapshot fails', () => {
    fixture.detectChanges();
    http.expectOne(userUrl).flush({ preferredCurrency: 'HUF' });
    http.expectOne(snapshotUrl).flush('nope', { status: 500, statusText: 'Server Error' });

    expect(component.loading()).toBeFalsy();
    expect(component.snapshot()).toBeNull();
  });
});
