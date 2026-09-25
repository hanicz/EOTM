import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { DatePipe } from '@angular/common';
import { Subject, of } from 'rxjs';

import { SearchComponent } from './search.component';
import { StockService } from '../service/stock.service';
import { MetricService } from '../service/metric.service';
import { WatchlistService } from '../service/watchlist.service';
import { Globals } from '../util/global';
import { Candle } from '../model/candle';

describe('SearchComponent', () => {
  let component: SearchComponent;
  let fixture: ComponentFixture<SearchComponent>;
  let globals: Globals;
  let requests: Subject<Candle>[];

  const candles = (close: number): Candle => ({
    o: [close - 1], h: [close + 1], l: [close - 2], c: [close],
    t: [Date.UTC(2026, 0, 1)], v: [1000000]
  });

  beforeEach(async () => {
    requests = [];
    await TestBed.configureTestingModule({
      imports: [SearchComponent],
      providers: [
        provideNoopAnimations(),
        DatePipe,
        Globals,
        { provide: StockService, useValue: {
          getAllStocks: () => of([]),
          getAllExchanges: () => of([]),
          getAllSymbols: () => of([]),
          getSignal: () => of(undefined),
          getCandleData: () => {
            const request = new Subject<Candle>();
            requests.push(request);
            return request;
          }
        } },
        { provide: MetricService, useValue: {
          getMetrics: () => of({}),
          getProfile: () => of({}),
          getRecommendations: () => of([])
        } },
        { provide: WatchlistService, useValue: {} }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SearchComponent);
    component = fixture.componentInstance;
    globals = TestBed.inject(Globals);
    globals.selectedExchange = 'X';
  });

  it('clears the previous chart immediately and ignores an older response', () => {
    globals.selectedStock = 'AAA';
    component.stockChanged(undefined);
    requests[0].next(candles(10));
    expect(component.chartReady).toBe(true);

    globals.selectedStock = 'BBB';
    component.stockChanged(undefined);
    expect(component.chartReady).toBe(false);
    expect(component.chartLoading).toBe(true);
    expect(component.chartOptions.series).toEqual([]);
    expect(component.endPrice).toBe(0);
    expect(requests[0].observed).toBe(false);

    requests[0].next(candles(99));
    expect(component.chartReady).toBe(false);
    requests[1].next(candles(20));
    expect(component.chartReady).toBe(true);
    expect(component.endPrice).toBe(20);
  });

  it('limits mobile chart points while retaining price extremes from the full history', () => {
    const originalWidth = Object.getOwnPropertyDescriptor(window, 'innerWidth');
    Object.defineProperty(window, 'innerWidth', { configurable: true, value: 390 });
    try {
      const length = 2000;
      component.candle = {
        o: Array(length).fill(10),
        h: Array(length).fill(12),
        l: Array(length).fill(8),
        c: Array(length).fill(11),
        t: Array.from({ length }, (_, i) => Date.UTC(2018, 0, 1 + i)),
        v: Array(length).fill(1000000)
      };
      component.candle.h[100] = 50;
      component.candle.l[1500] = 1;
      component.createChart();

      expect(component.chartOptions.series[0].data.length).toBeLessThanOrEqual(150);
      expect(component.chartOptions.series[0].data.some((point: { y: number[] }) => point.y[1] === 50)).toBe(true);
      expect(component.periodHigh).toBe(50);
      expect(component.periodLow).toBe(1);
      expect(component.endPrice).toBe(11);
    } finally {
      if (originalWidth) Object.defineProperty(window, 'innerWidth', originalWidth);
    }
  });
});
