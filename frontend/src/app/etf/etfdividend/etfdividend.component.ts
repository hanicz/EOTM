import { Component, ElementRef, inject, OnInit, ViewChild, ChangeDetectorRef } from '@angular/core';
import { Input } from '@angular/core';
import { EMPTY_FILTER, PortfolioFilter, filterRows } from '../../util/tablefilter';
import { CsvDropDirective } from '../../util/csv-drop.directive';
import { ETFDividend } from 'src/app/model/etfdividend';
import { EtfdividendService } from 'src/app/service/etfdividend.service';
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
import { Dialog } from 'primeng/dialog';
import { FormsModule } from '@angular/forms';
import { Select } from 'primeng/select';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { TickerIdentityComponent } from '../../util/ticker-identity.component';
import { ExchangeOptionComponent } from '../../util/exchange-option.component';
import { SymbolOptionComponent } from '../../util/symbol-option.component';

@Component({
    selector: 'app-etfdividend',
    hostDirectives: [CsvDropDirective],
    templateUrl: './etfdividend.component.html',
    imports: [Bind, Toolbar, PrimeTemplate, ButtonDirective, Ripple, Tooltip, TableModule, InputText, Dialog, FormsModule, Select, CurrencyPipe, DatePipe, Toast, TickerIdentityComponent, ExchangeOptionComponent, SymbolOptionComponent]
})
export class EtfdividendComponent implements OnInit {
  visibleDividends: ETFDividend[] = [];
  private activeFilter: PortfolioFilter = EMPTY_FILTER;

  @Input() set filter(value: PortfolioFilter) {
    this.activeFilter = value ?? EMPTY_FILTER;
    this.applyFilter();
  }

  private applyFilter(): void {
    this.visibleDividends = filterRows(this.dividends, this.activeFilter, ['shortName', 'name']);
  }


  dividends: ETFDividend[] = [];
  currencies: any[];
  selectedDividends: ETFDividend[] = [];
  dividendDialog: boolean = false;
  dividend: ETFDividend = {} as ETFDividend;
  @ViewChild('fileInput') fileInput?: ElementRef<HTMLInputElement>;
  symbols: Symbol[] = [];
  exchanges: Exchange[] = [];
  exchangesLoading: boolean = true;
  etfsLoading: boolean = false;
  selectedETF: Symbol = {} as Symbol;
  selectedExchange: Exchange = {} as Exchange;

  constructor(private etfDividendService: EtfdividendService, globals: Globals, private stockService: StockService, private cdr: ChangeDetectorRef, private messageService: MessageService) {
    inject(CsvDropDirective).csvDropped.subscribe(event => this.onUpload(event));
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
    this.etfDividendService.getAllDividends().subscribe({
      next: (data) => {
        this.dividends = data;
        this.applyFilter();
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.log(error);
      }
    });
  }

  openNew() {
    this.dividend = {} as ETFDividend;
    this.symbols = [];
    this.selectedETF = {} as Symbol;
    this.selectedExchange = {} as Exchange;
    this.dividendDialog = true;
  }

  hideDialog() {
    this.dividendDialog = false;
  }

  editDividend(dividend: ETFDividend) {
    this.dividend = { ...dividend };
    this.selectedExchange = this.exchanges.find(e => e.Code === dividend.exchange)
      ?? { Code: dividend.exchange, Name: dividend.exchange } as Exchange;
    this.selectedETF = { Code: dividend.shortName, Name: dividend.name } as Symbol;
    this.loadSymbols(dividend.shortName);
    this.dividendDialog = true;
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
    const ids = this.selectedDividends.map(i => i.id).join(',');
    this.deleteByIds(ids);
  }

  deleteByIds(ids: string) {
    this.etfDividendService.deleteByIds(ids).subscribe({
      next: () => {
        this.selectedDividends = [];
        this.fetchData();
      }
    });
  }

  download() {
    this.etfDividendService.download().subscribe({
      next: (data) => {
        let fileName = 'etfdividends.csv';
        let a = document.createElement('a');
        a.href = window.URL.createObjectURL(data as Blob);
        a.download = fileName;
        a.click();
      }
    });
  }

  saveDividend() {
    this.dividend.name = this.selectedETF.Name;
    this.dividend.shortName = this.selectedETF.Code;
    this.dividend.exchange = this.selectedExchange.Code;
    if (this.dividend.id === undefined) {
      this.etfDividendService.create(this.dividend).subscribe({
        next: () => {
          this.fetchData();
          this.dividendDialog = false;
        }
      });
    } else {
      this.etfDividendService.update(this.dividend).subscribe({
        next: () => {
          this.fetchData();
          this.dividendDialog = false;
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
      this.etfDividendService.uploadCSV(file).subscribe({
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
