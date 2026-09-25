import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { PerformanceSummaryComponent } from './performance-summary.component';
import { NetWorthHistory, NetWorthPoint } from '../../model/networth';
import { environment } from '../../../environments/environment';

describe('PerformanceSummaryComponent', () => {
  let component: PerformanceSummaryComponent;
  let fixture: ComponentFixture<PerformanceSummaryComponent>;
  let http: HttpTestingController;

  const historyUrl = `${environment.API_URL}/api/v1/networth/history`;

  const point = (date: string, totalWorth: number): NetWorthPoint => ({
    date, totalWorth, totalSpent: totalWorth, assetWorth: {}
  });

  const history = (points: NetWorthPoint[]): NetWorthHistory => ({ currency: 'HUF', points, months: [] });

  const load = (data: NetWorthHistory) => {
    component.ngOnInit();
    http.expectOne(historyUrl).flush(data);
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PerformanceSummaryComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations()]
    })
      .compileComponents();

    fixture = TestBed.createComponent(PerformanceSummaryComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('measures the change over the last 90 days', () => {
    load(history([point('2026-01-01', 500), point('2026-06-01', 1000), point('2026-08-30', 1200)]));

    expect(component.currency()).toBe('HUF');
    expect(component.points().map(p => p.date)).toEqual(['2026-06-01', '2026-08-30']);
    expect(component.latest()?.totalWorth).toBe(1200);
    expect(component.change()).toBe(200);
    expect(component.changePct()).toBe(20);
  });

  it('leaves the percent empty when the window started at zero', () => {
    load(history([point('2026-08-01', 0), point('2026-08-02', 300)]));

    expect(component.change()).toBe(300);
    expect(component.changePct()).toBeNull();
  });

  it('explains the missing trend when there is only one snapshot', () => {
    fixture.detectChanges();
    http.expectOne(historyUrl).flush(history([point('2026-08-01', 1000)]));
    fixture.detectChanges();

    expect(component.change()).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('two days of snapshots');
  });

  it('asks the backend to refresh today when the dashboard was refreshed', () => {
    fixture.componentRef.setInput('refresh', true);
    component.ngOnInit();
    http.expectOne(`${historyUrl}?refresh=true`).flush(history([point('2026-08-01', 1000), point('2026-08-02', 1100)]));

    expect(component.loading()).toBe(false);
    expect(component.latest()?.totalWorth).toBe(1100);
  });

  it('stops loading when the history fails', () => {
    component.ngOnInit();
    http.expectOne(historyUrl).flush('nope', { status: 500, statusText: 'Server Error' });

    expect(component.loading()).toBeFalsy();
    expect(component.history()).toBeNull();
  });
});
