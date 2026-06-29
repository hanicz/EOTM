import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { MenuComponent } from '../menu/menu.component';
import { Bind } from 'primeng/bind';
import { Panel } from 'primeng/panel';
import { ButtonDirective } from 'primeng/button';
import { Ripple } from 'primeng/ripple';
import { Tooltip } from 'primeng/tooltip';
import { PrimeTemplate } from 'primeng/api';
import { Skeleton } from 'primeng/skeleton';
import { Select } from 'primeng/select';
import { DecimalPipe, CurrencyPipe } from '@angular/common';
import { StockService } from '../service/stock.service';
import { CryptoService } from '../service/crypto.service';
import { EtfService } from '../service/etf.service';
import { ForexService } from '../service/forex.service';
import { DashboardService } from '../service/dashboard.service';
import { AlertService } from '../service/alert.service';
import { Investment } from '../model/investment';
import { Transaction } from '../model/transaction';
import { ETFInvestment } from '../model/etfinvestment';
import { ForexTransaction } from '../model/forextransaction';
import { StockAlert } from '../model/stockalert';
import { CryptoAlert } from '../model/cryptoalert';
import { AlertTypePipe } from '../util/pipe';

interface AssetSlice {
  label: string;
  value: number;
  percentage: number;
  color: string;
}

interface SpentWorth {
  spent: number;
  worth: number;
}

const BASE_CURRENCY = 'EUR';
const DEFAULT_CURRENCY = 'HUF';

@Component({
    selector: 'app-dashboard',
    templateUrl: './dashboard.component.html',
    styleUrls: ['./dashboard.component.css'],
    imports: [MenuComponent, Bind, Panel, ButtonDirective, Ripple, Tooltip, PrimeTemplate, Skeleton, Select, FormsModule, DecimalPipe, CurrencyPipe, AlertTypePipe]
})
export class DashboardComponent implements OnInit {

  loading: boolean = true;

  currencyOptions: string[] = [BASE_CURRENCY];
  selectedCurrency: string = DEFAULT_CURRENCY;

  stockTotal: number = 0;
  stockChangePct: number = 0;
  cryptoTotal: number = 0;
  cryptoChangePct: number = 0;
  etfTotal: number = 0;
  etfChangePct: number = 0;
  forexTotal: number = 0;
  forexChangePct: number = 0;

  netWorth: number = 0;
  netWorthChangePct: number = 0;

  slices: AssetSlice[] = [];
  donutSegments: { color: string; dashArray: string; dashOffset: string }[] = [];

  stockAlerts: StockAlert[] = [];
  cryptoAlerts: CryptoAlert[] = [];

  private stockItems: Investment[] = [];
  private cryptoItems: Transaction[] = [];
  private etfItems: ETFInvestment[] = [];
  private forexItems: ForexTransaction[] = [];

  constructor(
    private stockService: StockService,
    private cryptoService: CryptoService,
    private etfService: EtfService,
    private forexService: ForexService,
    private dashboardService: DashboardService,
    private alertService: AlertService,
    private cdr: ChangeDetectorRef,
    private router: Router,
  ) {
    this.loadData();
  }

  ngOnInit(): void {
  }

  navigateTo(path: string): void {
    this.router.navigate([path]);
  }

  refresh(): void {
    this.loading = true;
    this.loadData();
  }

  onCurrencyChange(): void {
    this.loading = true;
    this.fetchRatesAndCompute();
  }

  private loadData(): void {
    forkJoin({
      stock: this.stockService.getHolding(),
      crypto: this.cryptoService.getHoldings(),
      etf: this.etfService.getHolding(),
      forex: this.forexService.getHolding(),
      stockAlerts: this.alertService.getStockAlerts(),
      cryptoAlerts: this.alertService.getCryptoAlerts(),
    }).subscribe({
      next: ({ stock, crypto, etf, forex, stockAlerts, cryptoAlerts }) => {
        this.stockItems = stock as Investment[];
        this.cryptoItems = crypto as Transaction[];
        this.etfItems = etf as ETFInvestment[];
        this.forexItems = forex as ForexTransaction[];
        this.stockAlerts = stockAlerts;
        this.cryptoAlerts = cryptoAlerts;

        const currencies = new Set<string>([BASE_CURRENCY, DEFAULT_CURRENCY]);
        this.stockItems.forEach(i => currencies.add(i.currencyId));
        this.cryptoItems.forEach(i => currencies.add(i.currencyId));
        this.etfItems.forEach(i => currencies.add(i.currencyId));
        this.forexItems.forEach(i => { currencies.add(i.fromCurrencyId); currencies.add(i.toCurrencyId); });
        this.currencyOptions = Array.from(currencies).sort();

        this.fetchRatesAndCompute();
      },
      error: (error) => {
        console.log(error);
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  private fetchRatesAndCompute(): void {
    const neededCurrencies = Array.from(new Set([...this.currencyOptions, this.selectedCurrency]));
    this.dashboardService.getRates(neededCurrencies).subscribe({
      next: (data) => {
        this.compute(data.rates || {});
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.log(error);
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  private convert(amount: number, fromCurrency: string, rates: { [currency: string]: number }): number {
    const fromRate = fromCurrency === BASE_CURRENCY ? 1 : rates[fromCurrency];
    const toRate = this.selectedCurrency === BASE_CURRENCY ? 1 : rates[this.selectedCurrency];
    if (!fromRate || !toRate) return 0;
    const amountInBase = amount / fromRate;
    return amountInBase * toRate;
  }

  private sumSpentWorth(items: { amount: number; liveValue?: number; currencyId: string }[], rates: { [currency: string]: number }): SpentWorth {
    let spent = 0;
    let worth = 0;
    items.forEach(i => {
      spent += this.convert(i.amount, i.currencyId, rates);
      const worthAmount = (i.liveValue != undefined) ? i.liveValue : i.amount;
      worth += this.convert(worthAmount, i.currencyId, rates);
    });
    return { spent, worth };
  }

  private sumForexSpentWorth(items: ForexTransaction[], rates: { [currency: string]: number }): SpentWorth {
    let spent = 0;
    let worth = 0;
    items.forEach(i => {
      spent += this.convert(i.fromAmount, i.fromCurrencyId, rates);
      if (i.liveValue != undefined) {
        worth += this.convert(i.liveValue, i.fromCurrencyId, rates);
      }
    });
    return { spent, worth };
  }

  private percentChange(spent: number, worth: number): number {
    if (!spent) return 0;
    return (worth - spent) / spent * 100;
  }

  private compute(rates: { [currency: string]: number }): void {
    const stockSpentWorth = this.sumSpentWorth(this.stockItems, rates);
    const cryptoSpentWorth = this.sumSpentWorth(this.cryptoItems, rates);
    const etfSpentWorth = this.sumSpentWorth(this.etfItems, rates);
    const forexSpentWorth = this.sumForexSpentWorth(this.forexItems, rates);

    this.stockTotal = stockSpentWorth.worth;
    this.stockChangePct = this.percentChange(stockSpentWorth.spent, stockSpentWorth.worth);
    this.cryptoTotal = cryptoSpentWorth.worth;
    this.cryptoChangePct = this.percentChange(cryptoSpentWorth.spent, cryptoSpentWorth.worth);
    this.etfTotal = etfSpentWorth.worth;
    this.etfChangePct = this.percentChange(etfSpentWorth.spent, etfSpentWorth.worth);
    this.forexTotal = forexSpentWorth.worth;
    this.forexChangePct = this.percentChange(forexSpentWorth.spent, forexSpentWorth.worth);

    this.netWorth = this.stockTotal + this.cryptoTotal + this.etfTotal + this.forexTotal;
    const totalSpent = stockSpentWorth.spent + cryptoSpentWorth.spent + etfSpentWorth.spent + forexSpentWorth.spent;
    this.netWorthChangePct = this.percentChange(totalSpent, this.netWorth);

    this.buildAllocation();
  }

  private buildAllocation(): void {
    const total = this.stockTotal + this.cryptoTotal + this.etfTotal + this.forexTotal;
    const raw: AssetSlice[] = [
      { label: 'Stock', value: this.stockTotal, percentage: 0, color: '#ef9f27' },
      { label: 'Crypto', value: this.cryptoTotal, percentage: 0, color: '#5f5e5a' },
      { label: 'ETF', value: this.etfTotal, percentage: 0, color: '#1b1b1b' },
      { label: 'Forex', value: this.forexTotal, percentage: 0, color: '#b4b2a9' },
    ];

    if (total <= 0) {
      this.slices = raw;
      this.donutSegments = [];
      return;
    }

    raw.forEach(slice => slice.percentage = (slice.value / total) * 100);
    this.slices = raw;

    const circumference = 264;
    let offset = 0;
    this.donutSegments = raw.filter(s => s.value > 0).map(slice => {
      const length = (slice.percentage / 100) * circumference;
      const segment = {
        color: slice.color,
        dashArray: `${length} ${circumference - length}`,
        dashOffset: `${-offset}`,
      };
      offset += length;
      return segment;
    });
  }
}
