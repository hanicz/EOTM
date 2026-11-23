import { Component, OnInit, OnDestroy, HostListener, ChangeDetectorRef } from '@angular/core';
import { CryptoWatch } from '../model/cryptowatch';
import { Crypto } from '../model/crypto';
import { ForexWatch } from '../model/forexwatch';
import { StockWatch } from '../model/stockwatch';
import { WatchGroup } from '../model/watchgroup';
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
import { PrimeTemplate, MessageService } from 'primeng/api';
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
import { Toast } from 'primeng/toast';
import { TickerLogoComponent } from '../util/ticker-logo.component';
import { ExchangeOptionComponent } from '../util/exchange-option.component';
import { SymbolOptionComponent } from '../util/symbol-option.component';

@Component({
    selector: 'app-watchlist',
    templateUrl: './watchlist.component.html',
    styleUrls: ['./watchlist.component.css'],
    imports: [Bind, Toolbar, PrimeTemplate, ButtonDirective, Ripple, Tooltip, Accordion, AccordionPanel,
        AccordionHeader, AccordionContent, Skeleton, NgClass, Dialog, Tabs, TabList, Tab, TabPanels, TabPanel,
        Select, FormsModule, InputText, Toast, DecimalPipe, CurrencyPipe, UpperCasePipe, TickerLogoComponent, ExchangeOptionComponent, SymbolOptionComponent]
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

  readonly groupNameMaxLength = 64;
  readonly ungroupedId = -1;

  groups: WatchGroup[] = [];
  selectedGroupId: number | null = null;
  manageGroupsDialog: boolean = false;
  newGroupName: string = '';
  editedGroup: WatchGroup | null = null;
  editedGroupName: string = '';
  moveDialog: boolean = false;
  movedStock: StockWatch | null = null;
  movedGroupId: number | null = null;

  private readonly collapsedGroups = new Set<number>();

  private readonly subscriptions = new Subscription();

  constructor(private watchlistService: WatchlistService,
    globals: Globals,
    private router: Router,
    private stockService: StockService,
    private cryptoService: CryptoService,
    private messageService: MessageService,
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
    this.fetchGroups();
  }

  fetchGroups(): void {
    this.watchlistService.getGroups().subscribe({
      next: (data) => {
        this.groups = data;
        this.cdr.markForCheck();
      }
    });
  }

  get stockBuckets(): StockBucket[] {
    const buckets = this.groups.map(group => ({
      id: group.id,
      name: group.name,
      stocks: this.globals.stockWatchList.filter(s => s.groupId === group.id)
    }));

    const ungrouped = this.globals.stockWatchList.filter(s => s.groupId == null);
    if (ungrouped.length) {
      buckets.push({ id: this.ungroupedId, name: 'Ungrouped', stocks: ungrouped });
    }
    return buckets;
  }

  isCollapsed(groupId: number): boolean {
    return this.collapsedGroups.has(groupId);
  }

  toggleGroup(groupId: number): void {
    if (this.collapsedGroups.has(groupId)) {
      this.collapsedGroups.delete(groupId);
    } else {
      this.collapsedGroups.add(groupId);
    }
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
      this.selectedExchange.Code, this.selectedGroupId).subscribe({
        next: () => {
          this.display = false;
          this.selectedGroupId = null;
          this.fetchStockWatchList();
        }
      });
  }

  openManageGroups(): void {
    this.newGroupName = '';
    this.editedGroup = null;
    this.editedGroupName = '';
    this.manageGroupsDialog = true;
  }

  createGroup(): void {
    const name = this.newGroupName.trim();
    if (!name) return;

    this.watchlistService.createGroup(name).subscribe({
      next: () => {
        this.newGroupName = '';
        this.fetchGroups();
        this.cdr.markForCheck();
      },
      error: (error) => this.showError(error, 'Could not create the group')
    });
  }

  startRename(group: WatchGroup): void {
    this.editedGroup = group;
    this.editedGroupName = group.name;
  }

  cancelRename(): void {
    this.editedGroup = null;
    this.editedGroupName = '';
  }

  saveRename(): void {
    const name = this.editedGroupName.trim();
    if (!this.editedGroup || !name) return;

    this.watchlistService.renameGroup(this.editedGroup.id, name).subscribe({
      next: () => {
        this.cancelRename();
        this.fetchGroups();
        this.fetchStockWatchList();
        this.cdr.markForCheck();
      },
      error: (error) => this.showError(error, 'Could not rename the group')
    });
  }

  deleteGroup(group: WatchGroup): void {
    this.watchlistService.deleteGroup(group.id).subscribe({
      next: () => {
        this.fetchGroups();
        this.fetchStockWatchList();
        this.cdr.markForCheck();
      },
      error: (error) => this.showError(error, 'Could not delete the group')
    });
  }

  openMoveDialog(stockWatch: StockWatch, event: Event): void {
    event.stopPropagation();
    this.movedStock = stockWatch;
    this.movedGroupId = stockWatch.groupId;
    this.moveDialog = true;
  }

  saveMove(): void {
    if (!this.movedStock) return;

    this.watchlistService.setStockWatchGroup(this.movedStock.tickerWatchId, this.movedGroupId).subscribe({
      next: () => {
        this.moveDialog = false;
        this.movedStock = null;
        this.fetchStockWatchList();
      },
      error: (error) => this.showError(error, 'Could not move the stock')
    });
  }

  private showError(error: any, summary: string): void {
    this.messageService.add({
      severity: 'error',
      summary: summary,
      detail: error?.error?.error ?? 'Something went wrong, please try again.',
      life: 8000
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

interface StockBucket {
  id: number;
  name: string;
  stocks: StockWatch[];
}
