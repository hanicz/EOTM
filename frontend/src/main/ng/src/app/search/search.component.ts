import { Component, OnInit, ViewChild } from '@angular/core';
import { Candle } from '../model/candle';
import { Metric } from '../model/metric';
import { News } from '../model/news';
import { Profile } from '../model/profile';
import { Stock } from '../model/stock';
import { MetricService } from '../service/metric.service';
import { StockService } from '../service/stock.service';
import { Globals } from '../util/global';

import {
  ChartComponent,
  ApexAxisChartSeries,
  ApexChart,
  ApexXAxis,
  ApexTitleSubtitle,
  ApexTooltip
} from "ng-apexcharts";

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
  styleUrls: ['./search.component.css']
})
export class SearchComponent implements OnInit {

  stocks: Stock[] = [];
  profile: Profile = {} as Profile;
  metric: Metric = {} as Metric;
  news: News[] = [];
  candle: Candle = {} as Candle;
  options: any[];
  selectedOption = 3;
  newsType = '';
  startPrice = 0;
  endPrice = 0;
  percentage = 0;
  difference = 0;
  volume = 0;
  globals: Globals;

  @ViewChild("chart") chart: ChartComponent | any;
  public chartOptions: Partial<ChartOptions> | any;

  constructor(private stockService: StockService, globals: Globals,
    private metricService: MetricService) {

    this.globals = globals;
    this.options = [
      { label: '1 month', value: 1 },
      { label: '3 months', value: 3 },
      { label: '6 months', value: 6 },
      { label: '9 months', value: 9 },
      { label: '12 months', value: 12 },
    ];

    this.stockService.getAllStocks().subscribe({
      next: (data) => {
        this.stocks = data;
      }
    });

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
        height: 350,
        background: 'white',
        id: 'candles'
      },
      series: [],
      title: {
        text: 'Ticker Chart',
        align: 'left'
      },
      xaxis: {
        type: "category",
        labels: {
          show: false
        },
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

    if (globals.selectedStock != '') {
      this.stockChanged(undefined);
    }
  }

  ngOnInit(): void {
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

  stockChanged(event: any) {
    console.log('changed');
    this.metricService.getMetrics(this.globals.selectedStock).subscribe({
      next: (data) => {
        this.metric = data;
      }
    });

    this.metricService.getProfile(this.globals.selectedStock).subscribe({
      next: (data) => {
        this.profile = data;
      }
    });

    this.newsType = `company/${this.globals.selectedStock}`;

    this.getCandleData();
  }

  getCandleData() {
    this.stockService.getCandleData(this.globals.selectedStock, this.selectedOption).subscribe({
      next: (data) => {
        this.candle = data;
        this.createChart();
      }
    });
  }

  createChart() {
    var chartData = [];
    var volumeChartData = [];
    for (let i = 0; i < this.candle.c.length; i++) {
      let xy = { x: new Date(this.candle.t[i] * 1000).toLocaleDateString("en-US"), y: [this.candle.o[i], this.candle.h[i], this.candle.l[i], this.candle.c[i]] }
      chartData.push(xy);
      volumeChartData.push({ x: new Date(this.candle.t[i] * 1000).toLocaleDateString("en-US"), y: this.candle.v[i] / 1000000 })
    }
    this.chart.updateOptions({
      legend: {
        show: false
      },
      dataLabels: {
        enabled: false
      },
      colors: ["#999999"],
      yaxis: [{
        labels: {
          show: true,
          formatter: function (value: any) {
            return value + ' $';
          }
        }
      },
      {
        max: this.candle.v[this.candle.c.length - 2] / 100000,
        labels: {
          show: false,
        }
      }
      ]
    });
    this.chart.updateSeries([{ name: 'Price', data: chartData, type: 'candlestick' }, { name: 'Volume', data: volumeChartData, type: 'column' }], false);

    this.startPrice = this.candle.c[0];
    this.endPrice = this.candle.c[this.candle.c.length - 1];
    this.difference = this.endPrice - this.startPrice;
    this.percentage = this.difference / this.startPrice * 100;
    this.volume = this.candle.v[this.candle.c.length - 1] / 1000000;
  }
}
