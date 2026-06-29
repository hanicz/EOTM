import { Component, OnInit, ViewChild } from '@angular/core';
import { ETFInvestment } from '../model/etfinvestment';
import { MenuComponent } from '../menu/menu.component';
import { Bind } from 'primeng/bind';
import { Panel } from 'primeng/panel';
import { Tag } from 'primeng/tag';
import { Divider } from 'primeng/divider';
import { Tabs, TabList, Tab, TabPanels, TabPanel } from 'primeng/tabs';
import { Ripple } from 'primeng/ripple';
import { ButtonDirective } from 'primeng/button';
import { Tooltip } from 'primeng/tooltip';
import { Select } from 'primeng/select';
import { FormsModule } from '@angular/forms';
import { EtfholdingComponent } from './etfholding/etfholding.component';
import { EtfpositionComponent } from './etfposition/etfposition.component';
import { EtfinvestmentComponent } from './etfinvestment/etfinvestment.component';
import { EtfdividendComponent } from './etfdividend/etfdividend.component';
import { DecimalPipe, CurrencyPipe } from '@angular/common';
import { ChartComponent, ApexChart, ApexNonAxisChartSeries, ApexLegend } from 'ng-apexcharts';
import { DashboardService } from '../service/dashboard.service';

export type AllocationChartOptions = {
  series: ApexNonAxisChartSeries;
  chart: ApexChart;
  labels: string[];
  legend: ApexLegend;
};

@Component({
    selector: 'app-etf',
    templateUrl: './etf.component.html',
    styleUrls: ['./etf.component.css'],
    imports: [MenuComponent, Bind, Panel, Tag, Divider, Tabs, TabList, Ripple, Tab, TabPanels, TabPanel, ButtonDirective, Tooltip, Select, FormsModule, EtfholdingComponent, EtfpositionComponent, EtfinvestmentComponent, EtfdividendComponent, DecimalPipe, CurrencyPipe, ChartComponent]
})
export class EtfComponent implements OnInit {

  @ViewChild(EtfholdingComponent) etfholding!: EtfholdingComponent;
  @ViewChild(EtfpositionComponent) etfposition!: EtfpositionComponent;
  @ViewChild(EtfinvestmentComponent) etfinvestment!: EtfinvestmentComponent;
  @ViewChild(EtfdividendComponent) etfdividend!: EtfdividendComponent;

  investments: ETFInvestment[] = [];
  totalSpent: number = 0;
  totalWorth: number = 0;
  diffy: number = 0;
  percentage: number = 0;

  currencyOptions = [
    { label: 'HUF', value: 'HUF' },
    { label: 'EUR', value: 'EUR' },
    { label: 'USD', value: 'USD' },
    { label: 'GBP', value: 'GBP' }
  ];
  selectedCurrency: string = 'HUF';

  // Rates are EUR -> currency (e.g. rates['USD'] = how many USD per 1 EUR). EUR itself is always 1.
  private readonly BASE_CURRENCY = 'EUR';
  private rates: { [currency: string]: number } = { EUR: 1 };

  constructor(private dashboardService: DashboardService) { }

  allocationChartOptions: Partial<AllocationChartOptions> = {
    series: [],
    chart: {
      type: 'pie',
      width: 380
    },
    labels: [],
    legend: {
      position: 'bottom'
    }
  };

  ngOnInit(): void {
  }

  refreshAll(): void {
    this.etfholding?.refresh();
    this.etfposition?.refresh();
    this.etfinvestment?.refresh();
    this.etfdividend?.refresh();
  }

  onCurrencyChange(): void {
    this.loadRatesAndCalculate();
  }

  loadData(investments: ETFInvestment[]) {
    this.investments = investments;
    this.loadRatesAndCalculate();
  }

  private loadRatesAndCalculate(): void {
    const neededCurrencies = new Set<string>([this.selectedCurrency, ...this.investments.map(i => i.currencyId)]);
    const missing = [...neededCurrencies].filter(currency => !(currency in this.rates));

    if (missing.length === 0) {
      this.calculateTotals();
      return;
    }

    this.dashboardService.getRates(missing).subscribe({
      next: (response) => {
        this.rates = { ...this.rates, ...response.rates };
        this.calculateTotals();
      },
      error: (error) => {
        console.error('Error loading exchange rates:', error);
        this.calculateTotals();
      }
    });
  }

  private convert(amount: number, fromCurrency: string): number {
    const fromRate = fromCurrency === this.BASE_CURRENCY ? 1 : this.rates[fromCurrency];
    const toRate = this.selectedCurrency === this.BASE_CURRENCY ? 1 : this.rates[this.selectedCurrency];

    if (!fromRate || !toRate) {
      return amount;
    }

    return (amount / fromRate) * toRate;
  }

  private calculateTotals(): void {
    this.totalSpent = 0;
    this.totalWorth = 0;
    this.diffy = 0;
    this.percentage = 0;

    this.investments.forEach(i => {
      this.totalSpent += this.convert(i.amount, i.currencyId);
      const worth = i.liveValue != undefined ? i.liveValue : i.amount;
      this.totalWorth += this.convert(worth, i.currencyId);
    });
    this.diffy = this.totalWorth - this.totalSpent;
    this.percentage = this.diffy / this.totalSpent * 100;

    this.allocationChartOptions.series = this.investments.map(i => this.convert(i.liveValue ?? i.amount, i.currencyId));
    this.allocationChartOptions.labels = this.investments.map(i => `${i.shortName}.${i.exchange}`);
  }
}
