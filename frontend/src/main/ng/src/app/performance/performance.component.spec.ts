import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { PerformanceComponent, rangeStart } from './performance.component';
import { MonthlyPerformance, NetWorthHistory, NetWorthPoint } from '../model/networth';
import { Globals } from '../util/global';
import { environment } from '../../environments/environment';

describe('PerformanceComponent', () => {
  let component: PerformanceComponent;
  let fixture: ComponentFixture<PerformanceComponent>;
  let http: HttpTestingController;

  const historyUrl = `${environment.API_URL}/api/v1/networth/history`;

  const point = (date: string, totalWorth: number, totalSpent: number,
                 assetWorth: { [assetClass: string]: number } = {}): NetWorthPoint => ({
    date, totalWorth, totalSpent, assetWorth
  });

  const history = (points: NetWorthPoint[], months: MonthlyPerformance[] = []): NetWorthHistory => ({
    currency: 'HUF', points, months
  });

  const load = (data: NetWorthHistory) => {
    component.ngOnInit();
    http.expectOne(historyUrl).flush(data);
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PerformanceComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations(), provideRouter([]),
        Globals]
    })
      .compileComponents();

    fixture = TestBed.createComponent(PerformanceComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('works out where each range starts', () => {
    expect(rangeStart('ALL', '2026-08-15')).toBeNull();
    expect(rangeStart('YTD', '2026-08-15')).toBe('2026-01-01');
    expect(rangeStart('1Y', '2026-08-15')).toBe('2025-08-15');
    expect(rangeStart('3M', '2026-02-10')).toBe('2025-11-10');
    expect(rangeStart('1M', '2026-03-31')).toBe('2026-02-28');
  });

  it('filters the points to the range, counted back from the latest snapshot', () => {
    load(history([
      point('2026-01-10', 900, 900),
      point('2026-07-20', 1000, 950),
      point('2026-08-10', 1100, 1000),
      point('2026-08-20', 1150, 1000)
    ]));

    expect(component.points().length).toBe(4);

    component.setRange('1M');

    expect(component.points().map(p => p.date)).toEqual(['2026-07-20', '2026-08-10', '2026-08-20']);
  });

  it('splits the change over the range into invested money and market gain', () => {
    load(history([
      point('2026-06-01', 1000, 900),
      point('2026-06-15', 1100, 1000),
      point('2026-06-30', 1300, 1100)
    ]));

    const summary = component.summary()!;

    expect(summary.endWorth).toBe(1300);
    expect(summary.change).toBe(300);
    expect(summary.changePct).toBe(30);
    expect(summary.invested).toBe(200);
    expect(summary.marketGain).toBe(100);
  });

  it('leaves the change percent empty when the range started at zero', () => {
    load(history([point('2026-06-01', 0, 0), point('2026-06-02', 500, 500)]));

    expect(component.summary()!.changePct).toBeNull();
  });

  it('orders the asset classes like the dashboard and drops the ones that stay at zero', () => {
    load(history([
      point('2026-06-01', 305, 300, { Cash: 100, Stock: 200, Gold: 5, Crypto: 0 }),
      point('2026-06-02', 310, 300, { Cash: 100, Stock: 205, Gold: 5, Crypto: 0 })
    ]));

    expect(component.assetClasses()).toEqual(['Stock', 'Cash', 'Gold']);
    expect(component.assetChart().colors).toEqual(['#ef9f27', '#c9a227', '#b4b2a9']);
  });

  it('explains that the chart fills in when there is only one snapshot', () => {
    fixture.detectChanges();
    http.match(req => req.url.includes('/api/v1/user')).forEach(req => req.flush({ preferredCurrency: 'HUF' }));
    http.expectOne(historyUrl).flush(history([point('2026-06-01', 1000, 900)]));
    fixture.detectChanges();

    expect(component.hasHistory()).toBeFalsy();
    expect(fixture.nativeElement.textContent).toContain('the chart fills in over the next few days');
  });

  it('stops loading when the history fails', () => {
    component.ngOnInit();
    http.expectOne(historyUrl).flush('nope', { status: 500, statusText: 'Server Error' });

    expect(component.loading()).toBeFalsy();
    expect(component.history()).toBeNull();
  });
});
