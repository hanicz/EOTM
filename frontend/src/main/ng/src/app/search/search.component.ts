import { Component, OnInit, ViewChild } from '@angular/core';
import { Candle } from '../model/candle';
import { Metric } from '../model/metric';
import { News } from '../model/news';
import { Profile } from '../model/profile';
import { Stock } from '../model/stock';
import { MetricService } from '../service/metric.service';
import { NewsService } from '../service/news.service';
import { StockService } from '../service/stock.service';

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
  selectedTicker: any;

  @ViewChild("chart") chart: ChartComponent | any;
  public chartOptions: Partial<ChartOptions> | any;

  constructor(private stockService: StockService,
    private metricService: MetricService,
    private newsService: NewsService) {

    this.options = [
      {label: '1 month', value: 1},
      {label: '3 months', value: 3},
      {label: '6 months', value: 6},
      {label: '9 months', value: 9},
      {label: '12 months', value: 12},
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
          show: false
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
      yaxis: {
        labels: {
          show: true
        }
      },
      tooltip: {
        enabled: true,
        theme: 'dark',
        shared: true,
        intersect: false
      }
    };
  }

  ngOnInit(): void {
  }

  stockChanged(event: any) {
    this.metricService.getMetrics(this.selectedTicker).subscribe({
      next: (data) => {
        this.metric = data;
      }
    });

    this.metricService.getProfile(this.selectedTicker).subscribe({
      next: (data) => {
        this.profile = data;
      }
    });

    this.newsService.getCompanyNews(this.selectedTicker).subscribe({
      next: (data) => {
        this.news = data;
      }
    });
    this.getCandleData();
  }

  getCandleData() {
    this.stockService.getCandleData(this.selectedTicker, this.selectedOption).subscribe({
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
      volumeChartData.push({ x: new Date(this.candle.t[i] * 1000).toLocaleDateString("en-US"), y: this.candle.v[i] / 10000000 })
    }
    this.chart.updateSeries([{ name: 'Price', data: chartData, type: 'candlestick' }, { name: 'Volume', data: volumeChartData, type: 'column' }], false);
  }
}
