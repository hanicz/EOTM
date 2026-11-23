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
import { EtfProfile } from '../model/etf-profile';
import { Symbol } from '../model/symbol';

describe('SearchComponent', () => {
  let component: SearchComponent;
  let fixture: ComponentFixture<SearchComponent>;
  let globals: Globals;
  let requests: Subject<Candle>[];
  let etfRequests: string[];
  let symbolRequests: string[];
  const symbolsByExchange: Record<string, Symbol[]> = {
    EXA: [{ Code: 'CORP', Name: 'Sample Corp', Type: 'Common Stock', Isin: 'XX0000000025' }],
    EXB: [{ Code: 'FUND', Name: 'Sample Fund', Type: 'ETF', Isin: 'XX0000000017' }]
  };

  const etfProfile: EtfProfile = {
    isin: 'XX0000000017', provider: 'Sample Funds', indexName: 'Sample Index', investmentFocus: null,
    fundSize: 'EUR 100 m', ter: '0.10% p.a.', replication: 'Physical', distributionPolicy: 'Accumulating',
    fundCurrency: 'USD', currencyHedge: null, inceptionDate: '1 March 2020', volatility: null, totalHoldings: 250,
    holdings: [{ name: 'Alpha Corp', isin: 'XX1111111111', weight: 8 }, { name: 'Beta Inc', isin: 'XX2222222222', weight: 2 }],
    countries: [{ name: 'Atlantis', weight: 100 }],
    sectors: []
  };

  const candles = (close: number): Candle => ({
    o: [close - 1], h: [close + 1], l: [close - 2], c: [close],
    t: [Date.UTC(2026, 0, 1)], v: [1000000]
  });

  beforeEach(async () => {
    requests = [];
    etfRequests = [];
    symbolRequests = [];
    await TestBed.configureTestingModule({
      imports: [SearchComponent],
      providers: [
        provideNoopAnimations(),
        DatePipe,
        Globals,
        { provide: StockService, useValue: {
          getAllStocks: () => of([]),
          getAllExchanges: () => of([]),
          getAllSymbols: (exchange: string) => {
            symbolRequests.push(exchange);
            return of(symbolsByExchange[exchange] ?? []);
          },
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
          getRecommendations: () => of([]),
          getEtfProfile: (isin: string) => {
            etfRequests.push(isin);
            return of(etfProfile);
          }
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

  it('loads the fund profile once for an ETF and skips it for stocks', () => {
    component.symbols = [
      { Code: 'FUND', Name: 'Sample Fund', Type: 'ETF', Isin: 'XX0000000017' },
      { Code: 'CORP', Name: 'Sample Corp', Type: 'Common Stock', Isin: 'XX0000000025' }
    ];

    globals.selectedStock = 'FUND';
    component.stockChanged(undefined);
    component.updateDisplayInfo();

    expect(etfRequests).toEqual(['XX0000000017']);
    expect(component.isEtf).toBe(true);
    expect(component.etfLoading).toBe(false);
    expect(component.hasEtfData).toBe(true);
    expect(component.fundInfoRows.map(row => row.label)).toEqual(
      ['Provider', 'Index', 'Fund Size', 'TER', 'Replication', 'Distribution', 'Currency', 'Inception', 'Holdings']);

    globals.selectedStock = 'CORP';
    component.stockChanged(undefined);

    expect(etfRequests).toEqual(['XX0000000017']);
    expect(component.isEtf).toBe(false);
    expect(component.etfProfile).toBeUndefined();
  });

  it('reloads the tickers when the watchlist picks a stock on another exchange', () => {
    globals.selectedExchange = 'EXA';
    component.ngOnInit();
    component.loadSymbols();

    globals.selectedExchange = 'EXB';
    globals.selectedStock = 'FUND';
    globals.stockSelectedEvent.emit();

    expect(symbolRequests).toEqual(['EXA', 'EXB']);
    expect(component.symbols.map(s => s.Code)).toEqual(['FUND']);
    expect(component.isEtf).toBe(true);
    expect(etfRequests).toEqual(['XX0000000017']);

    globals.selectedStock = 'FUND';
    globals.stockSelectedEvent.emit();
    expect(symbolRequests).toEqual(['EXA', 'EXB']);
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
