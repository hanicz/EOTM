import { Component, OnInit, OnDestroy, HostListener, ChangeDetectorRef } from '@angular/core';
import { CryptoWatch } from '../model/cryptowatch';
import { Crypto } from '../model/crypto';
import { ForexWatch } from '../model/forexwatch';
import { StockWatch } from '../model/stockwatch';
import { WatchlistService } from '../service/watchlist.service';
import { filter, interval, Subscription } from 'rxjs';
import { Globals } from '../util/global';
import { Router } from '@angular/router';
import { StockService } from '../service/stock.service';
import { Stock } from '../model/stock';
import { CryptoService } from '../service/crypto.service';
import { environment } from '../../environments/environment';
import { Symbol } from '../model/symbol';
import { Exchange } from '../model/exchange';
import { Bind } from 'primeng/bind';
import { Toolbar } from 'primeng/toolbar';
import { PrimeTemplate } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { Ripple } from 'primeng/ripple';
import { Tooltip } from 'primeng/tooltip';
import { Accordion, AccordionPanel, AccordionHeader, AccordionContent } from 'primeng/accordion';
import { Skeleton } from 'primeng/skeleton';
import { NgClass, DecimalPipe, CurrencyPipe, UpperCasePipe } from '@angular/common';
import { Dialog } from 'primeng/dialog';
import { Tabs, TabList, Tab, TabPanels, TabPanel } from 'primeng/tabs';
import { Select } from 'primeng/select';
import { FormsModule } from '@angular/forms';
import { InputText } from 'primeng/inputtext';
import { TickerLogoComponent } from '../util/ticker-logo.component';
import { ExchangeOptionComponent } from '../util/exchange-option.component';
import { SymbolOptionComponent } from '../util/symbol-option.component';

@Component({
    selector: 'app-watchlist',
    templateUrl: './watchlist.component.html',
    styleUrls: ['./watchlist.component.css'],
    imports: [Bind, Toolbar, PrimeTemplate, ButtonDirective, Ripple, Tooltip, Accordion, AccordionPanel,
        AccordionHeader, AccordionContent, Skeleton, NgClass, Dialog, Tabs, TabList, Tab, TabPanels, TabPanel,
        Select, FormsModule, InputText, DecimalPipe, CurrencyPipe, UpperCasePipe, TickerLogoComponent, ExchangeOptionComponent, SymbolOptionComponent]
})
export class WatchlistComponent implements OnInit, OnDestroy {

  readonly skeletonRows = [0, 1, 2];
  readonly cryptoCurrency = 'EUR';

  forexWatchList: ForexWatch[] = [];
  cryptoWatchList: CryptoWatch[] = [];
  stocks: Stock[] = [];
  cryptos: Crypto[] = [];
  symbols: Symbol[] = [];
  exchanges: Exchange[] = [];
  globals: Globals;
  display: boolean = false;
  assetUrl: string;
  fromForex: string = '';
  toForex: string = '';
  selectedStock: Symbol = {} as Symbol;
  selectedExchange: Exchange = {} as Exchange;

  forexLoading: boolean = false;
  stockLoading: boolean = false;
  cryptoLoading: boolean = false;
  forexLoaded: boolean = false;
  stockLoaded: boolean = false;
  cryptoLoaded: boolean = false;
  forexError: boolean = false;
  stockError: boolean = false;
  cryptoError: boolean = false;
  exchangesLoading: boolean = true;
  stocksLoading: boolean = false;

  private readonly subscriptions = new Subscription();

  constructor(private watchlistService: WatchlistService,
    globals: Globals,
    private router: Router,
    private stockService: StockService,
    private cryptoService: CryptoService,
    private cdr: ChangeDetectorRef) {

    this.assetUrl = environment.assets_url;
    this.globals = globals;
    this.fetchData();

    this.stockService.getAllExchanges().subscribe({
      next: (data) => {
        this.exchangesLoading = false;
        this.exchanges = data;
        this.cdr.markForCheck();
      }
    });

    this.subscriptions.add(interval(60000)
      .pipe(filter(() => !document.hidden))
      .subscribe(() => this.fetchData()));
  }

  ngOnInit(): void {
    this.subscriptions.add(this.globals.stockWatchEvent.subscribe(() => this.fetchStockWatchList()));
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  /* Polling is skipped while the tab is in the background, so the lists are refreshed on the way back
     instead of showing whatever was left over from before. */
  @HostListener('document:visibilitychange')
  onVisibilityChange(): void {
    if (!document.hidden) {
      this.fetchData();
    }
  }

  private fetchData = () => {
    this.fetchCryptoWatchList();
    this.fetchStockWatchList();
    this.fetchForexWatchList();
  }

  fetchStockWatchList(): void {
    this.stockLoading = true;
    this.stockError = false;
    this.watchlistService.getStockWatchList().subscribe({
      next: (data) => {
        this.stockLoading = false;
        this.stockLoaded = true;
        this.globals.stockWatchList = data;
        this.cdr.markForCheck();
      },
      error: () => {
        this.stockLoading = false;
        this.stockLoaded = true;
        this.stockError = true;
        this.cdr.markForCheck();
      }
    });
  }

  fetchCryptoWatchList(): void {
    this.cryptoLoading = true;
    this.cryptoError = false;
    this.watchlistService.getCryptoWatchList(this.cryptoCurrency).subscribe({
      next: (data) => {
        this.cryptoLoading = false;
        this.cryptoLoaded = true;
        this.cryptoWatchList = data;
        this.cdr.markForCheck();
      },
      error: () => {
        this.cryptoLoading = false;
        this.cryptoLoaded = true;
        this.cryptoError = true;
        this.cdr.markForCheck();
      }
    });
  }

  fetchForexWatchList(): void {
    this.forexLoading = true;
    this.forexError = false;
    this.watchlistService.getForexWatchList().subscribe({
      next: (data) => {
        this.forexLoading = false;
        this.forexLoaded = true;
        this.forexWatchList = data;
        this.cdr.markForCheck();
      },
      error: () => {
        this.forexLoading = false;
        this.forexLoaded = true;
        this.forexError = true;
        this.cdr.markForCheck();
      }
    });
  }

  deltaClass(change: number): string {
    if (change > 0) return 'delta-up';
    return (change < 0) ? 'delta-down' : 'delta-flat';
  }

  caretClass(change: number): string {
    if (change > 0) return 'pi-caret-up';
    return (change < 0) ? 'pi-caret-down' : 'pi-minus';
  }

  signed(value: number): string {
    const formatted = (value ?? 0).toFixed(2);
    return (value > 0) ? '+' + formatted : formatted;
  }

  stockSelected(stock: StockWatch): void {
    this.globals.selectedExchange = stock.stockExchange;
    this.globals.selectedStock = stock.stockShortName;
    this.globals.stockSelectedEvent.emit();
    this.router.navigate(['./search']);
  }

  showDialog(): void {
    this.display = true;

    this.stockService.getAllStocks().subscribe({
      next: (data) => {
        this.stocks = data;
        this.cdr.markForCheck();
      }
    });

    this.cryptoService.getAllCrypto().subscribe({
      next: (data) => {
        this.cryptos = data;
        this.cdr.markForCheck();
      }
    });
  }

  get stockAlreadyWatched(): boolean {
    return this.globals.stockWatchList.some(s => s.stockShortName === this.selectedStock.Code
      && s.stockExchange === this.selectedExchange.Code);
  }

  get forexPairTouched(): boolean {
    return !!this.fromForex || !!this.toForex;
  }

  get forexPairValid(): boolean {
    const from = this.fromForex.trim().toUpperCase();
    const to = this.toForex.trim().toUpperCase();
    return /^[A-Z]{3}$/.test(from) && /^[A-Z]{3}$/.test(to) && from !== to;
  }

  get forexAlreadyWatched(): boolean {
    const from = this.fromForex.trim().toUpperCase();
    const to = this.toForex.trim().toUpperCase();
    return this.forexWatchList.some(f => f.fromCurrencyId === from && f.toCurrencyId === to);
  }

  isCryptoWatched(name: string): boolean {
    return this.cryptoWatchList.some(c => c.name === name);
  }

  createStockWatch(): void {
    this.watchlistService.createNewStockWatch(this.selectedStock.Code, this.selectedStock.Name,
      this.selectedExchange.Code).subscribe({
        next: () => {
          this.display = false;
          this.fetchStockWatchList();
        }
      });
  }

  createCryptoWatch(id: string): void {
    this.watchlistService.createWatch('/crypto/' + id).subscribe({
      next: () => this.fetchCryptoWatchList()
    });
  }

  createForexWatch(): void {
    this.watchlistService.createNewForexWatch(this.fromForex.trim().toUpperCase(),
      this.toForex.trim().toUpperCase()).subscribe({
        next: () => {
          this.display = false;
          this.fromForex = '';
          this.toForex = '';
          this.fetchForexWatchList();
        }
      });
  }

  removeStockWatch(stockWatch: StockWatch, event: Event): void {
    event.stopPropagation();
    this.watchlistService.deleteWatch('/stock/' + stockWatch.tickerWatchId).subscribe({
      next: () => this.fetchStockWatchList()
    });
  }

  removeCryptoWatch(cryptoWatch: CryptoWatch, event: Event): void {
    event.stopPropagation();
    this.watchlistService.deleteWatch('/crypto/' + cryptoWatch.cryptoWatchId).subscribe({
      next: () => this.fetchCryptoWatchList()
    });
  }

  removeForexWatch(forexWatch: ForexWatch, event: Event): void {
    event.stopPropagation();
    this.watchlistService.deleteWatch('/forex/' + forexWatch.forexWatchID).subscribe({
      next: () => this.fetchForexWatchList()
    });
  }

  exchangeChanged(event: any): void {
    this.selectedStock = {} as Symbol;
    this.stocksLoading = true;
    this.stockService.getAllSymbols(this.selectedExchange.Code).subscribe({
      next: (data) => {
        this.stocksLoading = false;
        this.symbols = data;
        this.cdr.markForCheck();
      }
    });
  }
}
