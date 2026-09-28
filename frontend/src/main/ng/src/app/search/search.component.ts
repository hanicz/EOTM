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
  private bucketSize = 1;
  private chartBuckets: number[][] = [];
  chartType: 'candlestick' | 'line' = 'candlestick';
  chartTypes = [
    { label: 'Candles', value: 'candlestick' as const, icon: 'pi pi-chart-bar' },
    { label: 'Line', value: 'line' as const, icon: 'pi pi-chart-line' }
  ];
  private priceAxisMin = 0;
  private priceAxisMax = 0;
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
          show: false
        },
        parentHeightOffset: 0,
        selection: {
          enabled: false
        },
        zoom: {
          enabled: false,
          allowMouseWheelZoom: false
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
        text: '',
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
        padding: {
          left: -6,
          right: 4,
          top: 0,
          bottom: 0
        }
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
        height: 240,
        stacked: true,
        background: 'transparent',
        fontFamily: 'inherit',
        parentHeightOffset: 0,
        toolbar: {
          show: false
        },
        zoom: {
          enabled: false
        },
        animations: {
          enabled: false
        }
      },
      title: {
        text: 'Analyst ratings',
        align: 'left',
        style: {
          fontSize: '13px',
          fontWeight: 700,
          color: '#888780'
        }
      },
      colors: ['#a32d2d', '#d9736a', '#dedbd2', '#7cb152', '#3b6d11'],
      stroke: {
        show: true,
        width: 2,
        colors: ['#fbfaf7']
      },
      states: {
        hover: {
          filter: { type: 'darken', value: 0.9 }
        },
        active: {
          filter: { type: 'none' }
        }
      },
      legend: {
        show: true,
        position: 'bottom',
        horizontalAlign: 'center',
        fontSize: '11px',
        labels: {
          colors: '#5f5e5a'
        },
        markers: {
          size: 5,
          shape: 'circle'
        },
        itemMargin: {
          horizontal: 6,
          vertical: 0
        }
      },
      grid: {
        show: false,
        padding: {
          left: 0,
          right: 0,
          top: -10,
          bottom: 0
        }
      },
      xaxis: {
        type: 'category',
        categories: [],
        axisBorder: {
          show: false
        },
        axisTicks: {
          show: false
        },
        labels: {
          style: {
            colors: '#888780',
            fontSize: '11px'
          }
        }
      },
      yaxis: {
        show: false
      },
      plotOptions: {
        bar: {
          horizontal: false,
          columnWidth: '58%',
          borderRadius: 4,
          borderRadiusApplication: 'end',
          borderRadiusWhenStacked: 'last'
        }
      },
      dataLabels: {
        enabled: true,
        formatter: (value: number, { dataPointIndex, w }: any) => {
          const total = w.globals.stackedSeriesTotals[dataPointIndex] || 0;
          return value > 0 && value / total >= 0.08 ? value : '';
        },
        style: {
          fontSize: '10px',
          fontWeight: 600,
          colors: ['#ffffff', '#1b1b1b', '#1b1b1b', '#1b1b1b', '#ffffff']
        },
        dropShadow: {
          enabled: false
        }
      },
      tooltip: {
        theme: 'dark',
        shared: true,
        intersect: false,
        inverseOrder: true,
        y: {
          formatter: (value: number) => value + (value === 1 ? ' analyst' : ' analysts')
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

  getTooltip = ({ dataPointIndex }: any) => {
    const bucket = this.chartBuckets[dataPointIndex];
    if (!bucket) {
      return '';
    }
    const [o, h, l, c, v] = bucket;
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

  getColor = ({ dataPointIndex }: any) => {
    const bucket = this.chartBuckets[dataPointIndex];
    if (bucket && bucket[0] > bucket[3]) {
      return "#ffc0c0";
    }
    return "#a8e0a8";
  }

  selectChartType(type: 'candlestick' | 'line') {
    if (type === this.chartType) return;
    this.chartType = type;
    if (this.chartReady) {
      this.createChart();
    }
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
    this.chartOptions = {
      ...this.chartOptions,
      title: { ...this.chartOptions.title, text: this.buildChartTitle() },
      yaxis: this.buildYAxis()
    };
  }

  buildChartTitle() {
    return this.bucketSize > 1 && this.chartType === 'candlestick' ? this.displayCurrency + ' · ' + this.bucketSize + '-session candles' : this.displayCurrency;
  }

  buildPriceScale() {
    const range = this.priceAxisMax - this.priceAxisMin;
    if (!(this.priceAxisMax > 0) || !(range >= 0)) {
      return {};
    }
    const rawStep = (range || this.priceAxisMax * 0.1) / 5;
    const magnitude = Math.pow(10, Math.floor(Math.log10(rawStep)));
    const step = [1, 2, 2.5, 5, 10].map(m => m * magnitude).find(s => s >= rawStep) ?? 10 * magnitude;
    const min = Math.max(0, Math.floor((this.priceAxisMin - range * 0.3) / step) * step);
    const max = Math.ceil(this.priceAxisMax / step) * step;
    return { min, max, tickAmount: Math.max(1, Math.round((max - min) / step)) };
  }

  buildYAxis() {
    return [{
      ...this.buildPriceScale(),
      labels: {
        show: true,
        maxWidth: 44,
        offsetX: -6,
        style: {
          colors: '#888780',
          fontSize: '11px'
        },
        formatter: (value: number) => this.formatAxisPrice(value)
      }
    },
    {
      seriesName: 'Volume',
      opposite: true,
      show: false,
      min: 0,
      max: this.volumeAxisMax
    }];
  }

  formatAxisPrice(value: number) {
    if (value == null || isNaN(value)) {
      return '';
    }
    const abs = Math.abs(value);
    if (abs >= 10000) {
      return new Intl.NumberFormat('en-US', { notation: 'compact', maximumFractionDigits: 1 }).format(value);
    }
    const digits = abs >= 100 || abs === 0 ? 0 : abs >= 1 ? 2 : 4;
    return value.toFixed(digits);
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
    this.bucketSize = bucketSize;
    let maxVolume = 0;
    this.chartBuckets = [];
    this.priceAxisMin = Math.min(...this.candle.l);
    this.priceAxisMax = Math.max(...this.candle.h);
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
      const volumeMillions = volume / 1000000;
      this.chartBuckets.push([this.candle.o[start], high, low, this.candle.c[end], volumeMillions]);
      chartData.push(this.chartType === 'line'
        ? { x, y: this.candle.c[end] }
        : { x, y: [this.candle.o[start], high, low, this.candle.c[end]] });
      volumeChartData.push({ x, y: volumeMillions });
      maxVolume = Math.max(maxVolume, volumeMillions);
    }
    this.volumeAxisMax = maxVolume * 4;
    const isLine = this.chartType === 'line';
    const rising = this.candle.c[this.candle.c.length - 1] >= this.candle.c[0];
    this.chartOptions = {
      ...this.chartOptions,
      chart: { ...this.chartOptions.chart, type: isLine ? 'line' : 'candlestick' },
      colors: isLine ? [rising ? '#00b746' : '#ef403c', this.getColor] : [this.getColor],
      series: [{ name: 'Price', data: chartData, type: isLine ? 'line' : 'candlestick' }, { name: 'Volume', data: volumeChartData, type: 'column' }],
      title: {
        ...this.chartOptions.title,
        text: this.buildChartTitle()
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
    let categories: string[] = [];
    const ordered = [...this.recommendations]
      .sort((a, b) => new Date(a.period).getTime() - new Date(b.period).getTime());
    ordered.forEach((recommendation) => {
      sellArray.push(recommendation.sell);
      strongSellArray.push(recommendation.strongSell);
      holdArray.push(recommendation.hold);
      buyArray.push(recommendation.buy);
      strongBuyArray.push(recommendation.strongBuy);
      categories.push(this.datepipe.transform(recommendation.period, 'MMM yy') ?? '');
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
      xaxis: {
        ...this.recChartOptions.xaxis,
        categories: categories
      }
    });
    this.recChart.updateSeries(chartData, false);
  }
}
