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
import { PrimeTemplate } from 'primeng/api';
import { Skeleton } from 'primeng/skeleton';
import { NgClass, NgStyle } from '@angular/common';
import { Tag } from 'primeng/tag';
import { Image } from 'primeng/image';
import { Dialog } from 'primeng/dialog';
import { Select } from 'primeng/select';
import { FormsModule } from '@angular/forms';
import { InputNumber } from 'primeng/inputnumber';
import { AlertTypePipe } from '../util/pipe';

@Component({
    selector: 'app-alert',
    templateUrl: './alert.component.html',
    styleUrls: ['./alert.component.css'],
    imports: [MenuComponent, Bind, Panel, Tabs, TabList, Ripple, Tab, TabPanels, TabPanel, ButtonDirective, Tooltip, TableModule, PrimeTemplate, Skeleton, NgClass, Tag, Image, NgStyle, Dialog, Select, FormsModule, InputNumber, AlertTypePipe]
})
export class AlertComponent implements OnInit {

  stockAlerts: StockAlert[] = [];
  cryptoAlerts: CryptoAlert[] = [];
  alertsLoading: boolean = true;
  cryptoAlertsLoading: boolean = true;
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
  assetUrl: string;

  constructor(private alertService: AlertService, private stockService: StockService,
    private cryptoService: CryptoService, globals: Globals, private cdr: ChangeDetectorRef) {
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

  fetchData(): void {
    this.alertService.getStockAlerts().subscribe({
      next: (data) => {
        this.alertsLoading = false;
        this.stockAlerts = data;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.log(error);
      }
    });

    this.alertService.getCryptoAlerts().subscribe({
      next: (data) => {
        this.cryptoAlertsLoading = false;
        this.cryptoAlerts = data;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.log(error);
      }
    });
  }

  deleteStockAlert(alert: StockAlert) {
    this.alertService.deleteStockAlert(alert.id).subscribe({
      next: () => {
        this.fetchData();
      },
      error: (error) => {
        console.log(error);
      }
    });
  }

  deleteCryptoAlert(alert: CryptoAlert) {
    this.alertService.deleteCryptoAlert(alert.id).subscribe({
      next: () => {
        this.fetchData();
      },
      error: (error) => {
        console.log(error);
      }
    });
  }

  exchangeChanged(event: any) {
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
    let data = {shortName: this.selectedStock.Code, exchange: this.selectedExchange.Code, type: this.selectedType, valuePoint: this.valuePoint, name: this.selectedStock.Name}

    this.alertService.createNewStockAlert(data).subscribe({
      next: (data) => {
        this.fetchData();
        this.displayDialog = false;
      }
    });
  }

  showDialog() {
    this.displayDialog = true;
  }

  createCryptoAlert() {
    let data = {symbol: this.selectedCrypto.symbol, type: this.selectedCryptoType, valuePoint: this.cryptoValuePoint, name: this.selectedCrypto.name}

    this.alertService.createNewCryptoAlert(data).subscribe({
      next: (data) => {
        this.fetchData();
        this.displayCryptoDialog = false;
      }
    });
  }

  showCryptoDialog() {
    this.displayCryptoDialog = true;
  }
}
