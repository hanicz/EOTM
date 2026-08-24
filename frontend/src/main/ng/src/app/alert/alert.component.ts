import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { StockAlert } from '../model/stockalert';
import { AlertService } from '../service/alert.service';
import { Exchange } from '../model/exchange';
import { Symbol } from '../model/symbol';
import { Globals } from '../util/global';
import { StockService } from '../service/stock.service';
import { CryptoAlert } from '../model/cryptoalert';
import { Crypto } from '../model/crypto';
import { CryptoService } from '../service/crypto.service';
import { environment } from 'src/environments/environment';
import { MenuComponent } from '../menu/menu.component';
import { Bind } from 'primeng/bind';
import { Panel } from 'primeng/panel';
import { Tabs, TabList, Tab, TabPanels, TabPanel } from 'primeng/tabs';
import { Ripple } from 'primeng/ripple';
import { ButtonDirective } from 'primeng/button';
import { Tooltip } from 'primeng/tooltip';
import { TableModule } from 'primeng/table';
import { MessageService, PrimeTemplate } from 'primeng/api';
import { Skeleton } from 'primeng/skeleton';
import { NgClass, UpperCasePipe } from '@angular/common';
import { Tag } from 'primeng/tag';
import { Toast } from 'primeng/toast';
import { TickerLogoComponent } from '../util/ticker-logo.component';
import { ExchangeOptionComponent } from '../util/exchange-option.component';
import { SymbolOptionComponent } from '../util/symbol-option.component';
import { Dialog } from 'primeng/dialog';
import { Select } from 'primeng/select';
import { FormsModule } from '@angular/forms';
import { InputNumber } from 'primeng/inputnumber';
import { AlertTypePipe } from '../util/pipe';

@Component({
    selector: 'app-alert',
    templateUrl: './alert.component.html',
    styleUrls: ['./alert.component.css'],
    imports: [MenuComponent, Bind, Panel, Tabs, TabList, Ripple, Tab, TabPanels, TabPanel, ButtonDirective, Tooltip,
        TableModule, PrimeTemplate, Skeleton, NgClass, Tag, Toast, Dialog, Select, FormsModule, InputNumber,
        UpperCasePipe, AlertTypePipe, TickerLogoComponent, ExchangeOptionComponent, SymbolOptionComponent]
})
export class AlertComponent implements OnInit {

  stockAlerts: StockAlert[] = [];
  cryptoAlerts: CryptoAlert[] = [];
  alertsLoading: boolean = true;
  cryptoAlertsLoading: boolean = true;
  alertsError: boolean = false;
  cryptoAlertsError: boolean = false;
  displayDialog: boolean = false;
  displayCryptoDialog: boolean = false;
  symbols: Symbol[] = [];
  exchanges: Exchange[] = [];
  selectedStock: Symbol = {} as Symbol;
  selectedExchange: Exchange = {} as Exchange;
  exchangesLoading: boolean = true;
  stocksLoading: boolean = false;
  cryptos: Crypto[] = [];
  cryptosLoading: boolean = true;
  selectedCrypto: Crypto = {} as Crypto;
  globals: Globals;
  valuePoint: number = 0.0;
  cryptoValuePoint: number = 0.0;
  types;
  selectedType: string = '';
  selectedCryptoType: string = '';
  creatingStockAlert: boolean = false;
  creatingCryptoAlert: boolean = false;
  assetUrl: string;

  private readonly deleting = new Set<string>();

  constructor(private alertService: AlertService, private stockService: StockService,
    private cryptoService: CryptoService, globals: Globals, private cdr: ChangeDetectorRef,
    private messageService: MessageService) {
    this.fetchData();
    this.globals = globals;
    this.assetUrl = environment.assets_url;
    this.stockService.getAllExchanges().subscribe({
      next: (data) => {
        this.exchangesLoading = false;
        this.exchanges = data;
        this.cdr.markForCheck();
      }
    });

    this.types = [
      { name: 'Percent over', code: 'PERCENT_OVER' },
      { name: 'Percent under', code: 'PERCENT_UNDER' },
      { name: 'Price over', code: 'PRICE_OVER' },
      { name: 'Price under', code: 'PRICE_UNDER' }
  ];

    this.cryptoService.getAllCrypto().subscribe({
      next: (data) => {
        this.cryptosLoading = false;
        this.cryptos = data;
        this.cdr.markForCheck();
      }
    });
  }

  ngOnInit(): void {
  }

  refresh(): void {
    this.alertsLoading = true;
    this.cryptoAlertsLoading = true;
    this.fetchData();
  }

  fetchData(): void {
    this.alertsError = false;
    this.cryptoAlertsError = false;

    this.alertService.getStockAlerts().subscribe({
      next: (data) => {
        this.alertsLoading = false;
        this.stockAlerts = data;
        this.cdr.markForCheck();
      },
      error: () => {
        this.alertsLoading = false;
        this.alertsError = true;
        this.cdr.markForCheck();
      }
    });

    this.alertService.getCryptoAlerts().subscribe({
      next: (data) => {
        this.cryptoAlertsLoading = false;
        this.cryptoAlerts = data;
        this.cdr.markForCheck();
      },
      error: () => {
        this.cryptoAlertsLoading = false;
        this.cryptoAlertsError = true;
        this.cdr.markForCheck();
      }
    });
  }

  isPercent(type: string): boolean {
    return type?.startsWith('PERCENT') ?? false;
  }

  isOver(type: string): boolean {
    return type?.endsWith('OVER') ?? false;
  }

  directionClass(type: string): string {
    return this.isOver(type) ? 'alert-over' : 'alert-under';
  }

  directionIcon(type: string): string {
    return this.isOver(type) ? 'pi-arrow-up' : 'pi-arrow-down';
  }

  formatValuePoint(type: string, valuePoint: number): string {
    return valuePoint + (this.isPercent(type) ? '%' : '');
  }

  alertSummary(name: string, type: string, valuePoint: number): string {
    if (!name || !type || !valuePoint) {
      return 'Notifies you once the threshold is crossed.';
    }
    const direction = this.isOver(type) ? 'rises above' : 'drops below';
    const threshold = this.isPercent(type) ? valuePoint + '% of your average' : valuePoint;
    return `Notifies you when ${name} ${direction} ${threshold}.`;
  }

  isDeleting(kind: string, id: number): boolean {
    return this.deleting.has(kind + id);
  }

  get stockAlertValid(): boolean {
    return !!this.selectedExchange.Code && !!this.selectedStock.Code && !!this.selectedType
      && this.valuePoint > 0;
  }

  get cryptoAlertValid(): boolean {
    return !!this.selectedCrypto.symbol && !!this.selectedCryptoType && this.cryptoValuePoint > 0;
  }

  deleteStockAlert(alert: StockAlert) {
    const key = 'stock' + alert.id;
    if (this.deleting.has(key)) {
      return;
    }
    this.deleting.add(key);

    this.alertService.deleteStockAlert(alert.id).subscribe({
      next: () => {
        this.deleting.delete(key);
        this.stockAlerts = this.stockAlerts.filter(a => a.id !== alert.id);
        this.cdr.markForCheck();
        this.messageService.add({ severity: 'success', detail: `Alert for ${alert.shortName} deleted.` });
      },
      error: (error) => {
        this.deleting.delete(key);
        this.cdr.markForCheck();
        this.messageService.add({ severity: 'error', detail: error.error?.error ?? 'Could not delete the alert.' });
      }
    });
  }

  deleteCryptoAlert(alert: CryptoAlert) {
    const key = 'crypto' + alert.id;
    if (this.deleting.has(key)) {
      return;
    }
    this.deleting.add(key);

    this.alertService.deleteCryptoAlert(alert.id).subscribe({
      next: () => {
        this.deleting.delete(key);
        this.cryptoAlerts = this.cryptoAlerts.filter(a => a.id !== alert.id);
        this.cdr.markForCheck();
        this.messageService.add({ severity: 'success', detail: `Alert for ${alert.symbol} deleted.` });
      },
      error: (error) => {
        this.deleting.delete(key);
        this.cdr.markForCheck();
        this.messageService.add({ severity: 'error', detail: error.error?.error ?? 'Could not delete the alert.' });
      }
    });
  }

  exchangeChanged(event: any) {
    this.selectedStock = {} as Symbol;
    this.symbols = [];
    this.stocksLoading = true;
    this.stockService.getAllSymbols(this.selectedExchange.Code).subscribe({
      next: (data) => {
        this.stocksLoading = false;
        this.symbols = data;
        this.cdr.markForCheck();
      }
    });
  }

  createStockAlert() {
    this.creatingStockAlert = true;
    let data = {shortName: this.selectedStock.Code, exchange: this.selectedExchange.Code, type: this.selectedType, valuePoint: this.valuePoint, name: this.selectedStock.Name}

    this.alertService.createNewStockAlert(data).subscribe({
      next: () => {
        this.creatingStockAlert = false;
        this.displayDialog = false;
        this.fetchData();
        this.messageService.add({ severity: 'success', detail: `Alert for ${data.shortName} created.` });
      },
      error: (error) => {
        this.creatingStockAlert = false;
        this.cdr.markForCheck();
        this.messageService.add({ severity: 'error', detail: error.error?.error ?? 'Could not create the alert.' });
      }
    });
  }

  showDialog() {
    this.selectedExchange = {} as Exchange;
    this.selectedStock = {} as Symbol;
    this.symbols = [];
    this.selectedType = '';
    this.valuePoint = 0.0;
    this.displayDialog = true;
  }

  createCryptoAlert() {
    this.creatingCryptoAlert = true;
    let data = {symbol: this.selectedCrypto.symbol, type: this.selectedCryptoType, valuePoint: this.cryptoValuePoint, name: this.selectedCrypto.name}

    this.alertService.createNewCryptoAlert(data).subscribe({
      next: () => {
        this.creatingCryptoAlert = false;
        this.displayCryptoDialog = false;
        this.fetchData();
        this.messageService.add({ severity: 'success', detail: `Alert for ${data.symbol} created.` });
      },
      error: (error) => {
        this.creatingCryptoAlert = false;
        this.cdr.markForCheck();
        this.messageService.add({ severity: 'error', detail: error.error?.error ?? 'Could not create the alert.' });
      }
    });
  }

  showCryptoDialog() {
    this.selectedCrypto = {} as Crypto;
    this.selectedCryptoType = '';
    this.cryptoValuePoint = 0.0;
    this.displayCryptoDialog = true;
  }
}
