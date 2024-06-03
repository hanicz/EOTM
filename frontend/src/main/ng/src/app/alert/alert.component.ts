import { Component, OnInit } from '@angular/core';
import { StockAlert } from '../model/stockalert';
import { AlertService } from '../service/alert.service';
import { Exchange } from '../model/exchange';
import { Symbol } from '../model/symbol';
import { Globals } from '../util/global';
import { StockService } from '../service/stock.service';
import { CryptoAlert } from '../model/cryptoalert';
import { environment } from 'src/environments/environment';

@Component({
  selector: 'app-alert',
  templateUrl: './alert.component.html',
  styleUrls: ['./alert.component.css']
})
export class AlertComponent implements OnInit {

  stockAlerts: StockAlert[] = [];
  cryptoAlerts: CryptoAlert[] = [];
  alertsLoading: boolean = true;
  cryptoAlertsLoading: boolean = true;
  displayDialog: boolean = false;
  symbols: Symbol[] = [];
  exchanges: Exchange[] = [];
  selectedStock: Symbol = {} as Symbol;
  selectedExchange: Exchange = {} as Exchange;
  exchangesLoading: boolean = true;
  stocksLoading: boolean = false;
  globals: Globals;
  valuePoint: number = 0.0;
  types;
  selectedType: string = '';
  assetUrl: string;

  constructor(private alertService: AlertService, private stockService: StockService,
    globals: Globals) {
    this.fetchData();
    this.globals = globals;
    this.assetUrl = environment.assets_url;
    this.stockService.getAllExchanges().subscribe({
      next: (data) => {
        this.exchangesLoading = false;
        this.exchanges = data;
      }
    });

    this.types = [
      { name: 'Percent over', code: 'PERCENT_OVER' },
      { name: 'Percent under', code: 'PERCENT_UNDER' },
      { name: 'Price over', code: 'PRICE_OVER' },
      { name: 'Price under', code: 'PRICE_UNDER' }
  ];
  }

  ngOnInit(): void {
  }

  fetchData(): void {
    this.alertService.getStockAlerts().subscribe({
      next: (data) => {
        this.alertsLoading = false;
        this.stockAlerts = data;
      },
      error: (error) => {
        console.log(error);
      }
    });

    this.alertService.getCryptoAlerts().subscribe({
      next: (data) => {
        this.cryptoAlertsLoading = false;
        this.cryptoAlerts = data;
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

  createCryptoAlert() {
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
}
