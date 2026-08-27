import { Component, OnInit, ViewChild, ChangeDetectorRef } from '@angular/core';
import { Dividend } from 'src/app/model/dividend';
import { DividendService } from 'src/app/service/dividend.service';
import { StockService } from 'src/app/service/stock.service';
import { Exchange } from 'src/app/model/exchange';
import { Symbol } from 'src/app/model/symbol';
import { Globals } from '../../util/global';
import { Bind } from 'primeng/bind';
import { Toolbar } from 'primeng/toolbar';
import { MessageService, PrimeTemplate } from 'primeng/api';
import { Toast } from 'primeng/toast';
import { ButtonDirective } from 'primeng/button';
import { Ripple } from 'primeng/ripple';
import { FileUpload } from 'primeng/fileupload';
import { TableModule } from 'primeng/table';
import { InputText } from 'primeng/inputtext';
import { Dialog } from 'primeng/dialog';
import { FormsModule } from '@angular/forms';
import { Select } from 'primeng/select';
import { TickerIdentityComponent } from '../../util/ticker-identity.component';
import { ExchangeOptionComponent } from '../../util/exchange-option.component';
import { SymbolOptionComponent } from '../../util/symbol-option.component';
import { CurrencyPipe, DatePipe } from '@angular/common';

@Component({
    selector: 'app-dividend',
    templateUrl: './dividend.component.html',
    styleUrls: ['./dividend.component.css'],
    imports: [Bind, Toolbar, PrimeTemplate, ButtonDirective, Ripple, FileUpload, TableModule, InputText, Dialog, FormsModule, Select, CurrencyPipe, DatePipe, Toast, TickerIdentityComponent, ExchangeOptionComponent, SymbolOptionComponent]
})
export class DividendComponent implements OnInit {

  dividends: Dividend[] = [];
  currencies: any[];
  selectedDividends: Dividend[] = [];
  dividendDialog: boolean = false;
  dividend: Dividend = {} as Dividend;
  @ViewChild('fileUpload') fileUpload: any;
  symbols: Symbol[] = [];
  exchanges: Exchange[] = [];
  exchangesLoading: boolean = true;
  stocksLoading: boolean = false;
  selectedStock: Symbol = {} as Symbol;
  selectedExchange: Exchange = {} as Exchange;
  globals: Globals;

  constructor(private dividendService: DividendService, globals: Globals, private stockService: StockService, private cdr: ChangeDetectorRef, private messageService: MessageService) {
    this.globals = globals;
    this.currencies = globals.currencies;

    this.stockService.getAllExchanges().subscribe({
      next: (data) => {
        this.exchangesLoading = false;
        this.exchanges = data;
        this.cdr.markForCheck();
      },
      error: () => {
        this.exchangesLoading = false;
        this.cdr.markForCheck();
      }
    });

    this.fetchData();
  }

  ngOnInit(): void {
  }

  refresh(): void {
    this.fetchData();
  }

  private fetchData(): void {
    this.dividendService.getAllDividends().subscribe({
      next: (data) => {
        this.dividends = data;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.log(error);
      }
    });
  }

  openNew() {
    this.dividend = {} as Dividend;
    this.symbols = [];
    this.selectedStock = {} as Symbol;
    this.selectedExchange = {} as Exchange;
    this.dividendDialog = true;
  }

  hideDialog() {
    this.dividendDialog = false;
  }

  editDividend(dividend: Dividend) {
    this.dividend = { ...dividend };
    this.selectedExchange = this.exchanges.find(e => e.Code === dividend.exchange)
      ?? { Code: dividend.exchange, Name: dividend.exchange } as Exchange;
    this.selectedStock = { Code: dividend.shortName, Name: dividend.name } as Symbol;
    this.loadSymbols(dividend.shortName);
    this.dividendDialog = true;
  }

  exchangeChanged(event: any) {
    this.selectedStock = {} as Symbol;
    this.loadSymbols();
  }

  private loadSymbols(preselectCode?: string) {
    if (!this.selectedExchange?.Code) {
      this.symbols = [];
      return;
    }
    this.stocksLoading = true;
    this.stockService.getAllSymbols(this.selectedExchange.Code).subscribe({
      next: (data) => {
        this.stocksLoading = false;
        this.symbols = data;
        if (preselectCode) {
          this.selectedStock = data.find(s => s.Code === preselectCode) ?? this.selectedStock;
        }
        this.cdr.markForCheck();
      },
      error: () => {
        this.stocksLoading = false;
        this.symbols = [];
        this.cdr.markForCheck();
      }
    });
  }

  deleteClicked() {
    let ids = '';
    this.selectedDividends.forEach(d => {
      ids += d.dividendId + ',';
    });
    this.deleteByIds(ids);
  }

  deleteByIds(ids: string) {
    this.dividendService.deleteByIds(ids).subscribe({
      next: () => {
        this.selectedDividends = [];
        this.fetchData();
      }
    });
  }

  download() {
    this.dividendService.download().subscribe({
      next: (data) => {
        let fileName = 'dividends.csv';
        let a = document.createElement('a');
        a.href = window.URL.createObjectURL(data as Blob);
        a.download = fileName;
        a.click();
      }
    });
  }

  saveDividend() {
    this.dividend.name = this.selectedStock.Name;
    this.dividend.shortName = this.selectedStock.Code;
    this.dividend.exchange = this.selectedExchange.Code;
    if (this.dividend.dividendId === undefined) {
      this.dividendService.create(this.dividend).subscribe({
        next: () => {
          this.fetchData();
          this.dividendDialog = false;
        }
      });
    } else {
      this.dividendService.update(this.dividend).subscribe({
        next: () => {
          this.fetchData();
          this.dividendDialog = false;
        }
      });
    }
  }

  onUpload(event: any) {
    for (let file of event.files) {
      this.dividendService.uploadCSV(file).subscribe({
        next: () => {
          this.fetchData();
          this.fileUpload.clear();
          this.messageService.add({ severity: 'success', detail: 'Import finished.' });
        },
        error: (error) => {
          this.fileUpload.clear();
          this.messageService.add({ severity: 'error', detail: error.error?.error ?? 'Import failed.' });
        }
      });
    }
  }

}
