import { Component, OnInit } from '@angular/core';
import { StockAlert } from '../model/stockalert';
import { AlertService } from '../service/alert.service';
import { Exchange } from '../model/exchange';
import { Symbol } from '../model/symbol';
import { StockService } from '../service/stock.service';

@Component({
  selector: 'app-alert',
  templateUrl: './alert.component.html',
  styleUrls: ['./alert.component.css']
})
export class AlertComponent implements OnInit {

  stockAlerts: StockAlert[] = [];
  alertsLoading: boolean = true;
  displayDialog: boolean = false;
  symbols: Symbol[] = [];
  exchanges: Exchange[] = [];
  selectedStock: Symbol = {} as Symbol;
  selectedExchange: Exchange = {} as Exchange;
  exchangesLoading: boolean = true;
  stocksLoading: boolean = false;
  valuePoint: number = 0.0;
  types;
  selectedType: string = '';

  constructor(private alertService: AlertService, private stockService: StockService) {
    this.fetchData();

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
    this.alertService.getAlerts().subscribe({
      next: (data) => {
        this.alertsLoading = false;
        this.stockAlerts = data;
      },
      error: (error) => {
        console.log(error);
      }
    });
  }

  deleteAlert(alert: StockAlert) {
    console.log('itten');
    this.alertService.deleteStockAlert(alert.id).subscribe({
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

  createAlert() {
    let data = {shortName: this.selectedStock.Code, exchange: this.selectedExchange.Code, type: this.selectedType, valuePoint: this.valuePoint}

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
