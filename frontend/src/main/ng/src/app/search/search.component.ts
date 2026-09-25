import { Component, OnInit, AfterViewInit, OnDestroy, ViewChild, ChangeDetectorRef } from '@angular/core';
import { Candle } from '../model/candle';
import { Metric } from '../model/metric';
import { News } from '../model/news';
import { Profile } from '../model/profile';
import { Stock } from '../model/stock';
import { MetricService } from '../service/metric.service';
import { StockService } from '../service/stock.service';
import { Globals } from '../util/global';
import { WatchlistService } from '../service/watchlist.service';
import { Symbol } from '../model/symbol';
import { Exchange } from '../model/exchange';
import { DatePipe, DecimalPipe, CurrencyPipe, NgClass } from '@angular/common';

import {
  ChartComponent,
  ApexAxisChartSeries,
  ApexChart,
  ApexXAxis,
  ApexTitleSubtitle,
  ApexTooltip
} from "ng-apexcharts";
import { Recommendation } from '../model/recommendation';
import { MenuComponent } from '../menu/menu.component';
import { Bind } from 'primeng/bind';
import { Panel } from 'primeng/panel';
import { Select } from 'primeng/select';
import { FormsModule } from '@angular/forms';
import { TickerLogoComponent } from '../util/ticker-logo.component';
import { ExchangeOptionComponent } from '../util/exchange-option.component';
import { SymbolOptionComponent } from '../util/symbol-option.component';
import { ButtonDirective } from 'primeng/button';
import { Ripple } from 'primeng/ripple';
import { Divider } from 'primeng/divider';
import { Skeleton } from 'primeng/skeleton';
import { PrimeTemplate } from 'primeng/api';
import { Tooltip } from 'primeng/tooltip';
import { NewsComponent } from '../news/news.component';
import { SignalResult } from '../model/signal';
import { Subscription } from 'rxjs';

export type ChartOptions = {
  series: ApexAxisChartSeries;
  chart: ApexChart;
  xaxis: ApexXAxis;
  title: ApexTitleSubtitle;
  tooltip: ApexTooltip;
};

@Component({
    selector: 'app-search',
    templateUrl: './search.component.html',
    styleUrls: ['./search.component.css'],
    imports: [MenuComponent, Bind, Panel, PrimeTemplate, Select, FormsModule, ButtonDirective, Ripple, Divider, Skeleton, ChartComponent, NewsComponent, DecimalPipe, CurrencyPipe, DatePipe, NgClass, Tooltip, TickerLogoComponent, ExchangeOptionComponent, SymbolOptionComponent]
})
export class SearchComponent implements OnInit, AfterViewInit, OnDestroy {

  globals: Globals;

  stocks: Stock[] = [];
  symbols: Symbol[] = [];
  exchanges: Exchange[] = [];
  news: News[] = [];
  options: any[];
  recommendations: Recommendation[] = [];

  profile: Profile = {} as Profile;
  metric: Metric = {} as Metric;
  candle: Candle = {} as Candle;
  signalResult: SignalResult | undefined;

  selectedOption = 12;
  startPrice = 0;
  endPrice = 0;
  percentage = 0;
  difference = 0;
  volume = 0;

  displayName = '';
  displayTicker = '';
  displayExchange = '';
  displayCurrency = 'USD';
  displayIsin = '';
  displayType = '';
  hasProfile = false;
  hasMetric = false;
  private metricHasValues = false;
  private usExchange = false;
  private pendingStockRestore = false;
  volumeAxisMax = 0;
  periodHigh = 0;
  periodLow = 0;
  periodHighDate: Date | undefined;
  periodLowDate: Date | undefined;

  newsType = '';

  exchangesLoading: boolean = true;
  stocksLoading: boolean = false;
  profileLoading: boolean = false;
  chartLoading = false;
  chartReady = false;
  private candleRequest?: Subscription;
  private stockSelectedSubscription?: Subscription;
  private selectionId = 0;

  public chartOptions: Partial<ChartOptions> | any;
  @ViewChild("recChart") recChart: ChartComponent | any;
  public recChartOptions: Partial<ChartOptions> | any;

  constructor(private stockService: StockService, globals: Globals,
    private metricService: MetricService, private watchlistService: WatchlistService,
    private datepipe: DatePipe, private cdr: ChangeDetectorRef) {

    this.globals = globals;
    this.options = [
      { label: '1M', value: 1 },
      { label: '6M', value: 6 },
      { label: '1Y', value: 12 },
      { label: '2Y', value: 24 },
      { label: '5Y', value: 60 },
      { label: 'All', value: 100 },
    ];

    this.stockService.getAllStocks().subscribe({
      next: (data) => {
        this.stocks = data;
        this.cdr.markForCheck();
      }
    });

    this.stockService.getAllExchanges().subscribe({
      next: (data) => {
        this.exchangesLoading = false;
        this.exchanges = data;
        this.updateDisplayInfo();
        if (this.pendingStockRestore) {
          this.pendingStockRestore = false;
          this.stockChanged(undefined);
        }
        this.cdr.markForCheck();
      }
    });

    if (this.globals.selectedExchange != '') {
      this.loadSymbols();
    }

    this.chartOptions = {
      chart: {
        type: 'candlestick',
        toolbar: {
          show: true,
          tools: {
            download: false,
            selection: false,
            zoom: false,
            zoomin: false,
            zoomout: false,
            pan: false,
            reset: false,
          },
        },
        selection: {
          enabled: false
        },
        animations: {
          enabled: false
        },
        height: 360,
        background: 'transparent',
        fontFamily: 'inherit',
        id: 'candles'
      },
      series: [],
      legend: { show: false },
      dataLabels: { enabled: false },
      colors: [this.getColor],
      stroke: { width: [2, 0] },
      title: {
        text: 'Candlestick chart',
        align: 'left',
        style: {
          fontSize: '13px',
          fontWeight: 700,
          color: '#888780',
        }
      },
      grid: {
        borderColor: '#ece9df',
        strokeDashArray: 3,
      },
      xaxis: {
        type: 'category',
        labels: {
          show: false
        },
        axisBorder: {
          show: false
        },
        axisTicks: {
          show: false
        }
      },
      noData: {
        text: 'Waiting...'
      },
      tooltip: {
        enabled: true,
        theme: 'dark',
        shared: true,
        intersect: false,
        custom: this.getTooltip
      }
    };

    this.recChartOptions = {
      series: [],
      noData: {
        text: 'No analyst recommendations for this ticker'
      },
      chart: {
        type: 'bar',
        height: 220,
        stacked: true,
        toolbar: {
          show: true,
          tools: {
            download: false,
            selection: false,
            zoom: false,
            zoomin: false,
            zoomout: false,
            pan: false,
            reset: false,
          },
        }
      },
      yaxis: {
        labels: {
          show: false
        }
      },
      plotOptions: {
        bar: {
          horizontal: false,
          borderRadius: 10
        },
      },
      dataLabels: {
        style: {
          // Text color per series (Strong Sell, Sell, Hold, Buy, Strong Buy) chosen for
          // contrast against each bar color - the bright Hold/Buy colors are unreadable with white text.
          colors: ['#ffffff', '#ffffff', '#000000', '#000000', '#ffffff']
        }
      }
    };
  }

  ngAfterViewInit(): void {
    if (this.globals.selectedStock == '') {
      return;
    }
    if (this.exchanges.length > 0) {
      this.stockChanged(undefined);
    } else {
      this.pendingStockRestore = true;
    }
  }

  ngOnInit(): void {
    this.stockSelectedSubscription = this.globals.stockSelectedEvent.subscribe(() => {
      this.stockChanged(undefined);
    });
  }

  ngOnDestroy(): void {
    ++this.selectionId;
    this.candleRequest?.unsubscribe();
    this.stockSelectedSubscription?.unsubscribe();
  }

  getTooltip({ series, seriesIndex, dataPointIndex, w }: any) {
    const o = w.globals.seriesCandleO[0][dataPointIndex]
    const h = w.globals.seriesCandleH[0][dataPointIndex]
    const l = w.globals.seriesCandleL[0][dataPointIndex]
    const c = w.globals.seriesCandleC[0][dataPointIndex]
    const v = series[1][dataPointIndex];
    return (
      '<div class="card p-2">' +
      '<div>Open: <span class="font-bold">' + o.toFixed(2) + '</span></div>' +
      '<div>High: <span class="font-bold">' + h.toFixed(2) + '</span></div>' +
      '<div>Low: <span class="font-bold">' + l.toFixed(2) + '</span></div>' +
      '<div>Close: <span class="font-bold">' + c.toFixed(2) + '</span></div>' +
      '<div>Volume: <span class="font-bold">' + v.toFixed(1) + ' M' + '</span></div>' +
      '</div>'
    )
  }

  getColor({ series, seriesIndex, dataPointIndex, w }: any) {
    if (w.globals.seriesCandleO[0][dataPointIndex] > w.globals.seriesCandleC[0][dataPointIndex]) {
      return "#ffc0c0";
    }
    return "#a8e0a8";
  }

  stockChanged(event: any) {
    const selectionId = ++this.selectionId;
    this.getCandleData();
    this.profile = {} as Profile;
    this.metric = {} as Metric;
    this.signalResult = undefined;
    this.recommendations = [];
    this.metricHasValues = false;
    this.updateDisplayInfo();
    this.profileLoading = this.usExchange;

    this.newsType = `company/${this.globals.selectedStock}`;
    if (!this.globals.selectedStock) return;
    this.getSignal(selectionId);

    if (!this.usExchange) {
      return;
    }

    this.metricService.getMetrics(this.globals.selectedStock).subscribe({
      next: (data) => {
        if (selectionId !== this.selectionId) return;
        this.metric = data ?? {} as Metric;
        this.metricHasValues = this.metric.peInclExtraTTM != null || this.metric.yearHigh != null
          || this.metric.tenDayAverageTradingVolume != null;
        this.updateDisplayInfo();
        this.cdr.markForCheck();
      },
      error: () => {
        if (selectionId !== this.selectionId) return;
        this.metric = {} as Metric;
        this.metricHasValues = false;
        this.updateDisplayInfo();
        this.cdr.markForCheck();
      }
    });

    this.metricService.getProfile(this.globals.selectedStock).subscribe({
      next: (data) => {
        if (selectionId !== this.selectionId) return;
        this.profileLoading = false;
        this.profile = data ?? {} as Profile;
        this.updateDisplayInfo();
        this.cdr.markForCheck();
      },
      error: () => {
        if (selectionId !== this.selectionId) return;
        this.profileLoading = false;
        this.profile = {} as Profile;
        this.updateDisplayInfo();
        this.cdr.markForCheck();
      }
    });

    this.metricService.getRecommendations(this.globals.selectedStock).subscribe({
      next: (data) => {
        if (selectionId !== this.selectionId) return;
        this.recommendations = data ?? [];
        this.createRecChart();
        this.cdr.markForCheck();
      },
      error: () => {
        if (selectionId !== this.selectionId) return;
        this.recommendations = [];
        this.createRecChart();
        this.cdr.markForCheck();
      }
    });
  }

  updateDisplayInfo() {
    const symbol = this.symbols.find(s => s.Code === this.globals.selectedStock);
    const exchange = this.exchanges.find(e => e.Code === this.globals.selectedExchange);

    this.usExchange = exchange?.CountryISO2 === 'US' || this.globals.selectedExchange === 'US';
    this.hasProfile = this.usExchange && !!this.profile.name;
    this.hasMetric = this.usExchange && this.metricHasValues;

    this.displayName = (this.hasProfile ? this.profile.name : '')
      || symbol?.Name
      || this.stocks.find(s => s.shortName === this.globals.selectedStock)?.name
      || this.globals.selectedStock;
    this.displayTicker = (this.hasProfile ? this.profile.ticker : '') || this.globals.selectedStock;
    this.displayExchange = (this.hasProfile ? this.profile.exchange : '')
      || exchange?.Name || this.globals.selectedExchange;
    this.displayCurrency = exchange?.Currency
      || (this.hasProfile ? this.profile.currency : '') || 'USD';
    this.displayIsin = symbol?.Isin ?? '';
    this.displayType = symbol?.Type ?? '';

    this.applyChartCurrency();
    this.createRecChart();
  }

  applyChartCurrency() {
    if (!this.chartReady) {
      return;
    }
    this.chartOptions = { ...this.chartOptions, yaxis: this.buildYAxis() };
  }

  buildYAxis() {
    const currency = this.displayCurrency;
    return [{
      labels: {
        show: true,
        formatter: (value: any) => value + ' ' + currency
      }
    },
    {
      seriesName: 'Volume',
      opposite: true,
      min: 0,
      max: this.volumeAxisMax,
      labels: {
        show: false,
      }
    }];
  }

  getSignal(selectionId: number) {
    this.stockService.getSignal(this.globals.selectedStock, this.globals.selectedExchange).subscribe({
      next: (data) => {
        if (selectionId !== this.selectionId) return;
        this.signalResult = data;
        this.cdr.markForCheck();
      },
      error: (error) => {
        if (selectionId !== this.selectionId) return;
        console.log(error);
        this.signalResult = undefined;
        this.cdr.markForCheck();
      }
    });
  }

  exchangeChanged(event: any) {
    ++this.selectionId;
    this.globals.selectedStock = '';
    this.symbols = [];
    this.resetCandleChart();
    this.loadSymbols();
  }

  loadSymbols() {
    this.stocksLoading = true;
    this.stockService.getAllSymbols(this.globals.selectedExchange).subscribe({
      next: (data) => {
        this.stocksLoading = false;
        this.symbols = data;
        this.updateDisplayInfo();
        this.cdr.markForCheck();
      },
      error: () => {
        this.stocksLoading = false;
        this.symbols = [];
        this.cdr.markForCheck();
      }
    });
  }

  selectPeriod(months: number) {
    if (months === this.selectedOption) return;
    this.selectedOption = months;
    this.getCandleData();
  }

  getCandleData() {
    this.resetCandleChart();
    if (!this.globals.selectedStock) return;
    const selectionId = this.selectionId;
    this.chartLoading = true;
    this.candleRequest = this.stockService.getCandleData(this.globals.selectedStock, this.globals.selectedExchange, this.selectedOption).subscribe({
      next: (data) => {
        if (selectionId !== this.selectionId) return;
        this.candle = data;
        if (data?.c?.length) {
          this.createChart();
          this.chartReady = true;
        }
        this.chartLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        if (selectionId !== this.selectionId) return;
        this.chartLoading = false;
        this.cdr.markForCheck();
      }
    });
  }

  private resetCandleChart() {
    this.candleRequest?.unsubscribe();
    this.candle = {} as Candle;
    this.chartReady = false;
    this.chartLoading = false;
    this.chartOptions = { ...this.chartOptions, series: [] };
    this.startPrice = 0;
    this.endPrice = 0;
    this.difference = 0;
    this.percentage = 0;
    this.volume = 0;
    this.calculatePeriodExtremes();
    this.cdr.markForCheck();
  }

  createChart() {
    const chartData = [];
    const volumeChartData = [];
    // Bound SVG work on long histories. Buckets retain open, high, low,
    // close and total volume, so price extremes remain visible.
    const maxCandles = typeof window !== 'undefined' && window.innerWidth < 768 ? 150 : 450;
    const bucketSize = Math.ceil(this.candle.c.length / maxCandles);
    let maxVolume = 0;
    for (let start = 0; start < this.candle.c.length; start += bucketSize) {
      const end = Math.min(start + bucketSize, this.candle.c.length) - 1;
      let high = this.candle.h[start];
      let low = this.candle.l[start];
      let volume = 0;
      for (let i = start; i <= end; i++) {
        high = Math.max(high, this.candle.h[i]);
        low = Math.min(low, this.candle.l[i]);
        volume += this.candle.v[i];
      }
      const x = new Date(this.candle.t[end]).toLocaleDateString('en-US');
      chartData.push({ x, y: [this.candle.o[start], high, low, this.candle.c[end]] });
      const volumeMillions = volume / 1000000;
      volumeChartData.push({ x, y: volumeMillions });
      maxVolume = Math.max(maxVolume, volumeMillions);
    }
    this.volumeAxisMax = maxVolume * 4;
    this.chartOptions = {
      ...this.chartOptions,
      series: [{ name: 'Price', data: chartData, type: 'candlestick' }, { name: 'Volume', data: volumeChartData, type: 'column' }],
      title: {
        ...this.chartOptions.title,
        text: bucketSize > 1 ? 'Candlestick chart (' + bucketSize + '-session candles)' : 'Candlestick chart'
      },
      yaxis: this.buildYAxis()
    };

    this.startPrice = this.candle.c[0];
    this.endPrice = this.candle.c[this.candle.c.length - 1];
    this.difference = this.endPrice - this.startPrice;
    this.percentage = this.difference / this.startPrice * 100;
    this.volume = this.candle.v[this.candle.c.length - 1] / 1000000;
    this.calculatePeriodExtremes();
  }

  calculatePeriodExtremes() {
    if (!this.candle.h?.length || !this.candle.l?.length) {
      this.periodHigh = 0;
      this.periodLow = 0;
      this.periodHighDate = undefined;
      this.periodLowDate = undefined;
      return;
    }
    let highIndex = 0;
    let lowIndex = 0;
    for (let i = 1; i < this.candle.h.length; i++) {
      if (this.candle.h[i] > this.candle.h[highIndex]) {
        highIndex = i;
      }
      if (this.candle.l[i] < this.candle.l[lowIndex]) {
        lowIndex = i;
      }
    }
    this.periodHigh = this.candle.h[highIndex];
    this.periodLow = this.candle.l[lowIndex];
    this.periodHighDate = new Date(this.candle.t[highIndex]);
    this.periodLowDate = new Date(this.candle.t[lowIndex]);
  }

  get periodLabel(): string {
    return this.options.find(o => o.value === this.selectedOption)?.label ?? '';
  }

  checkStockContain() {
    return this.globals.stockWatchList.some(s => s.stockShortName === this.globals.selectedStock
      && s.stockExchange === this.globals.selectedExchange);
  }

  hostName(url: string): string {
    if (!url) {
      return '';
    }
    return url.replace(/^https?:\/\//, '').replace(/^www\./, '').replace(/\/$/, '');
  }

  addToWatchList() {
    this.watchlistService.createNewStockWatch(this.globals.selectedStock, this.displayName, this.globals.selectedExchange).subscribe({
      next: () => {
        this.globals.stockWatchEvent.emit();
      }
    });
  }

  removeFromWatchList() {
    let id = this.globals.stockWatchList.find(s => s.stockShortName === this.globals.selectedStock);
    this.watchlistService.deleteWatch(`/stock/${id?.tickerWatchId}`).subscribe({
      next: () => {
        this.globals.stockWatchEvent.emit();
      }
    });
  }

  createRecChart() {
    if (!this.recChart) {
      return;
    }
    if (!this.usExchange || this.recommendations.length === 0) {
      this.recChart.updateSeries([], false);
      return;
    }
    let sellArray: number[] = [];
    let strongSellArray: number[] = [];
    let holdArray: number[] = [];
    let buyArray: number[] = [];
    let strongBuyArray: number[] = [];
    let categories: Date[] = [];
    this.recommendations.forEach((recommendation) => {
      sellArray.push(recommendation.sell);
      strongSellArray.push(recommendation.strongSell);
      holdArray.push(recommendation.hold);
      buyArray.push(recommendation.buy);
      strongBuyArray.push(recommendation.strongBuy);
      categories.push(recommendation.period);
    });

    let chartData = [
      {
        name: 'Strong Sell',
        data: strongSellArray
      },
      {
        name: 'Sell',
        data: sellArray
      },
      {
        name: 'Hold',
        data: holdArray
      },
      {
        name: 'Buy',
        data: buyArray
      },
      {
        name: 'Strong Buy',
        data: strongBuyArray
      }
    ];

    this.recChart.updateOptions({
      colors: ["#c11f01", "#ff2700", "#f0ff00", "#36ff00", "#2ac600"],
      xaxis: {
        type: 'category',
        categories: categories,
        labels: {
          formatter: (value: number) => this.datepipe.transform(value, 'MMM y'),
        }
      },
    });
    this.recChart.updateSeries(chartData, false);
  }
}
