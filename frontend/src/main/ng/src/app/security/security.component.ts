import { Component, OnInit, ViewChild } from '@angular/core';
import { SecurityTransaction } from '../model/securityTransaction';
import { Interest } from '../model/interest';
import { MenuComponent } from '../menu/menu.component';
import { Bind } from 'primeng/bind';
import { Panel } from 'primeng/panel';
import { Divider } from 'primeng/divider';
import { Tabs, TabList, Tab, TabPanels, TabPanel } from 'primeng/tabs';
import { Ripple } from 'primeng/ripple';
import { ButtonDirective } from 'primeng/button';
import { Tooltip } from 'primeng/tooltip';
import { Select } from 'primeng/select';
import { FormsModule } from '@angular/forms';
import { HoldingComponent } from './holding/holding.component';
import { TransactionComponent } from './transaction/transaction.component';
import { InterestComponent } from './interest/interest.component';
import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { DashboardService } from '../service/dashboard.service';
import { Tag } from 'primeng/tag';
import { ChartComponent, ApexChart, ApexNonAxisChartSeries, ApexLegend } from 'ng-apexcharts';

export type AllocationChartOptions = {
  series: ApexNonAxisChartSeries;
  chart: ApexChart;
  labels: string[];
  legend: ApexLegend;
};

@Component({
    selector: 'app-security',
    templateUrl: './security.component.html',
    styleUrls: ['./security.component.css'],
    imports: [MenuComponent, Bind, Panel, Divider, Tabs, TabList, Ripple, Tab, TabPanels, TabPanel, ButtonDirective, Tooltip, Select, FormsModule, HoldingComponent, TransactionComponent, InterestComponent, CurrencyPipe, DecimalPipe, Tag, ChartComponent]
})
export class SecurityComponent implements OnInit {

  @ViewChild(HoldingComponent) holding!: HoldingComponent;
  @ViewChild(TransactionComponent) transaction!: TransactionComponent;
  @ViewChild(InterestComponent) interest!: InterestComponent;

  transactions: SecurityTransaction[] = [];
  interests: Interest[] = [];
  totalSpent: number = 0;
  totalWorth: number = 0;
  diffy: number = 0;
  percentage: number = 0;

  allocationChartOptions: Partial<AllocationChartOptions> = {
    series: [],
    chart: {
      type: 'pie',
      width: 380,
      height: 500
    },
    labels: [],
    legend: {
      position: 'bottom'
    }
  };

  currencyOptions = [
    { label: 'HUF', value: 'HUF' },
    { label: 'EUR', value: 'EUR' },
    { label: 'USD', value: 'USD' },
    { label: 'GBP', value: 'GBP' }
  ];
  selectedCurrency: string = 'HUF';

  private readonly BASE_CURRENCY = 'EUR';
  private rates: { [currency: string]: number } = { EUR: 1 };

  private ratesRequestInFlight: boolean = false;
  private pendingRecalculation: boolean = false;

  constructor(private dashboardService: DashboardService) { }

  ngOnInit(): void {
  }

  refreshAll(): void {
    this.holding?.refresh();
    this.transaction?.refresh();
    this.interest?.refresh();
  }

  onCurrencyChange(): void {
    this.loadRatesAndCalculate();
  }

  loadData(transactions: SecurityTransaction[]) {
    this.transactions = transactions;
    this.loadRatesAndCalculate();
  }

  loadInterestData(interests: Interest[]) {
    this.interests = interests;
    this.loadRatesAndCalculate();
  }

  private loadRatesAndCalculate(): void {
    const neededCurrencies = new Set<string>([
      this.selectedCurrency,
      ...this.transactions.map(t => t.currencyId),
      ...this.interests.map(i => i.currencyId)
    ]);
    const missing = [...neededCurrencies].filter(currency => !(currency in this.rates));

    if (missing.length === 0) {
      this.calculateTotals();
      return;
    }

    if (this.ratesRequestInFlight) {
      this.pendingRecalculation = true;
      return;
    }

    this.ratesRequestInFlight = true;
    this.dashboardService.getRates(missing).subscribe({
      next: (response) => {
        this.rates = { ...this.rates, ...response.rates };
        this.ratesRequestInFlight = false;
        this.calculateTotals();
        this.recalculateIfPending();
      },
      error: (error) => {
        console.error('Error loading exchange rates:', error);
        this.ratesRequestInFlight = false;
        this.calculateTotals();
        this.recalculateIfPending();
      }
    });
  }

  private recalculateIfPending(): void {
    if (this.pendingRecalculation) {
      this.pendingRecalculation = false;
      this.loadRatesAndCalculate();
    }
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
    this.transactions.forEach(t => {
      this.totalSpent += this.convert(t.amount, t.currencyId);
    });

    let totalInterest = 0;
    this.interests.forEach(i => {
      totalInterest += this.convert(i.amount, i.currencyId);
    });

    this.totalWorth = this.totalSpent + totalInterest;
    this.diffy = this.totalWorth - this.totalSpent;
    this.percentage = this.diffy / this.totalSpent * 100;

    this.allocationChartOptions.series = this.transactions.map(t => this.convert(t.amount, t.currencyId));
    this.allocationChartOptions.labels = this.transactions.map(t => t.securityName);
  }
}
