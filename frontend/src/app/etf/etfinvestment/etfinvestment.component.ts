import { Component, ElementRef, inject, OnInit, ViewChild, ChangeDetectorRef } from '@angular/core';
import { Input } from '@angular/core';
import { EMPTY_FILTER, PortfolioFilter, filterRows } from '../../util/tablefilter';
import { CsvDropDirective } from '../../util/csv-drop.directive';
import { ETFInvestment } from 'src/app/model/etfinvestment';
import { Account } from 'src/app/model/account';
import { AccountService } from 'src/app/service/account.service';
import { EtfService } from 'src/app/service/etf.service';
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
import { Tooltip } from 'primeng/tooltip';
import { TableModule } from 'primeng/table';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { FormsModule } from '@angular/forms';
import { Dialog } from 'primeng/dialog';
import { CurrencyPipe, DatePipe, DecimalPipe, NgClass } from '@angular/common';
import { TickerIdentityComponent } from '../../util/ticker-identity.component';
import { ExchangeOptionComponent } from '../../util/exchange-option.component';
import { SymbolOptionComponent } from '../../util/symbol-option.component';

@Component({
    selector: 'app-etfinvestment',
    hostDirectives: [CsvDropDirective],
    templateUrl: './etfinvestment.component.html',
    imports: [Bind, Toolbar, PrimeTemplate, ButtonDirective, Ripple, Tooltip, TableModule, InputText, Select, FormsModule, Dialog, CurrencyPipe, DatePipe, DecimalPipe, NgClass, Toast, TickerIdentityComponent, ExchangeOptionComponent, SymbolOptionComponent]
})
export class EtfinvestmentComponent implements OnInit {
  visibleInvestments: ETFInvestment[] = [];
  private activeFilter: PortfolioFilter = EMPTY_FILTER;

  @Input() set filter(value: PortfolioFilter) {
    this.activeFilter = value ?? EMPTY_FILTER;
    this.applyFilter();
  }

  private applyFilter(): void {
    this.visibleInvestments = filterRows(this.investments, this.activeFilter, ['shortName', 'name'], 'accountName');
  }


  investments: ETFInvestment[] = [];
  currencies: any[];
  statuses: any[];
  accounts: Account[] = [];
  selectedInvestments: ETFInvestment[] = [];
  investmentDialog: boolean = false;
  investment: ETFInvestment = {} as ETFInvestment;
  @ViewChild('fileInput') fileInput?: ElementRef<HTMLInputElement>;
  symbols: Symbol[] = [];
  exchanges: Exchange[] = [];
  exchangesLoading: boolean = true;
  etfsLoading: boolean = false;
  selectedETF: Symbol = {} as Symbol;
  selectedExchange: Exchange = {} as Exchange;

  constructor(private etfService: EtfService, globals: Globals, private accountService: AccountService, private stockService: StockService, private cdr: ChangeDetectorRef, private messageService: MessageService) {
    inject(CsvDropDirective).csvDropped.subscribe(event => this.onUpload(event));
    this.currencies = globals.currencies;

    this.statuses = [
      { label: 'BUY', value: 'B' },
      { label: 'SELL', value: 'S' }
    ];

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

    this.accountService.getAccounts().subscribe({
      next: (data) => {
        this.accounts = data;
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
    this.etfService.getInvestments().subscribe({
      next: (data) => {
        this.investments = data;
        this.applyFilter();
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.log(error);
      }
    });
  }

  openNew() {
    this.investment = {} as ETFInvestment;
    this.symbols = [];
    this.selectedETF = {} as Symbol;
    this.selectedExchange = {} as Exchange;
    this.investmentDialog = true;
  }

  hideDialog() {
    this.investmentDialog = false;
  }

  editInvestment(investment: ETFInvestment) {
    this.investment = { ...investment };
    this.selectedExchange = this.exchanges.find(e => e.Code === investment.exchange)
      ?? { Code: investment.exchange, Name: investment.exchange } as Exchange;
    this.selectedETF = { Code: investment.shortName, Name: investment.name } as Symbol;
    this.loadSymbols(investment.shortName);
    this.investmentDialog = true;
  }

  exchangeChanged(event: any) {
    this.selectedETF = {} as Symbol;
    this.loadSymbols();
  }

  private loadSymbols(preselectCode?: string) {
    if (!this.selectedExchange?.Code) {
      this.symbols = [];
      return;
    }
    this.etfsLoading = true;
    this.stockService.getAllSymbols(this.selectedExchange.Code).subscribe({
      next: (data) => {
        this.etfsLoading = false;
        this.symbols = data;
        if (preselectCode) {
          this.selectedETF = data.find(s => s.Code === preselectCode) ?? this.selectedETF;
        }
        this.cdr.markForCheck();
      },
      error: () => {
        this.etfsLoading = false;
        this.symbols = [];
        this.cdr.markForCheck();
      }
    });
  }

  deleteClicked() {
    const ids = this.selectedInvestments.map(i => i.id).join(',');
    this.deleteByIds(ids);
  }

  deleteByIds(ids: string) {
    this.etfService.deleteByIds(ids).subscribe({
      next: () => {
        this.selectedInvestments = [];
        this.fetchData();
      }
    });
  }

  download() {
    this.etfService.download().subscribe({
      next: (data) => {
        let fileName = 'etfinvestments.csv';
        let a = document.createElement('a');
        a.href = window.URL.createObjectURL(data as Blob);
        a.download = fileName;
        a.click();
      }
    });
  }

  saveInvestment() {
    this.investment.name = this.selectedETF.Name;
    this.investment.shortName = this.selectedETF.Code;
    this.investment.exchange = this.selectedExchange.Code;
    if (this.investment.id === undefined) {
      this.etfService.create(this.investment).subscribe({
        next: () => {
          this.fetchData();
          this.investmentDialog = false;
        }
      });
    } else {
      this.etfService.update(this.investment).subscribe({
        next: () => {
          this.fetchData();
          this.investmentDialog = false;
        }
      });
    }
  }

  fileChosen(event: Event) {
    const files = Array.from((event.target as HTMLInputElement).files ?? []);
    if (files.length) this.onUpload({ files });
  }

  private clearFileInput() {
    if (this.fileInput) this.fileInput.nativeElement.value = '';
  }

  onUpload(event: any) {
    for (let file of event.files) {
      this.etfService.uploadCSV(file).subscribe({
        next: () => {
          this.fetchData();
          this.clearFileInput();
          this.messageService.add({ severity: 'success', detail: 'Import finished.' });
        },
        error: (error) => {
          this.clearFileInput();
          this.messageService.add({ severity: 'error', detail: error.error?.error ?? 'Import failed.' });
        }
      });
    }
  }
}
