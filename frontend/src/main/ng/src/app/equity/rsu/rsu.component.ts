import { ChangeDetectorRef, Component } from '@angular/core';
import { MessageService, PrimeTemplate } from 'primeng/api';
import { RSUGrant, RSUGrantReport, VestingFrequency } from '../../model/equity';
import { EquityService } from '../../service/equity.service';
import { StockService } from '../../service/stock.service';
import { Exchange } from '../../model/exchange';
import { Symbol } from '../../model/symbol';
import { ExchangeOptionComponent } from '../../util/exchange-option.component';
import { SymbolOptionComponent } from '../../util/symbol-option.component';
import { TickerLogoComponent } from '../../util/ticker-logo.component';
import { Bind } from 'primeng/bind';
import { Toolbar } from 'primeng/toolbar';
import { Toast } from 'primeng/toast';
import { ButtonDirective } from 'primeng/button';
import { Ripple } from 'primeng/ripple';
import { Tooltip } from 'primeng/tooltip';
import { TableModule } from 'primeng/table';
import { InputText } from 'primeng/inputtext';
import { InputNumber } from 'primeng/inputnumber';
import { Select } from 'primeng/select';
import { Dialog } from 'primeng/dialog';
import { FormsModule } from '@angular/forms';
import { DatePipe, DecimalPipe } from '@angular/common';

@Component({
    selector: 'app-equity-rsu',
    templateUrl: './rsu.component.html',
    styleUrls: ['./rsu.component.css'],
    imports: [Bind, Toolbar, PrimeTemplate, Toast, ButtonDirective, Ripple, Tooltip, TableModule,
        InputText, InputNumber, Select, Dialog, FormsModule, DecimalPipe, DatePipe,
        ExchangeOptionComponent, SymbolOptionComponent, TickerLogoComponent]
})
export class EquityRsuComponent {

  report: RSUGrantReport | null = null;
  loading: boolean = true;
  selectedGrants: RSUGrant[] = [];
  expandedRows: { [key: string]: boolean } = {};

  grantDialog: boolean = false;
  grant: RSUGrant = this.emptyGrant();

  exchanges: Exchange[] = [];
  symbols: Symbol[] = [];
  selectedExchange: Exchange = {} as Exchange;
  selectedStock: Symbol = {} as Symbol;
  exchangesLoading: boolean = true;
  stocksLoading: boolean = false;

  readonly frequencies: { label: string, value: VestingFrequency }[] = [
    { label: 'Annual', value: 'ANNUAL' },
    { label: 'Quarterly', value: 'QUARTERLY' }
  ];
  readonly noteMaxLength = 64;
  readonly maxVestingYears = 10;

  constructor(private equityService: EquityService, private stockService: StockService,
    private messageService: MessageService, private cdr: ChangeDetectorRef) {
    this.fetchData();
    this.loadExchanges();
  }

  get canSave(): boolean {
    return !!this.grant.shortName && !!this.grant.exchange && !!this.grant.grantDate
      && !!this.grant.quantity && this.grant.quantity > 0
      && this.grant.vestingYears >= 1 && this.grant.vestingYears <= this.maxVestingYears;
  }

  scheduleLabel(grant: RSUGrant): string {
    const frequency = this.frequencies.find(option => option.value === grant.vestingFrequency);
    return `${grant.vestingYears}y × ${(frequency?.label ?? grant.vestingFrequency).toLowerCase()}`;
  }

  openNew(): void {
    this.grant = this.emptyGrant();
    this.selectedStock = {} as Symbol;
    this.applyExchange(this.grant.exchange);
    this.grantDialog = true;
  }

  editGrant(grant: RSUGrant): void {
    this.grant = {
      id: grant.id,
      shortName: grant.shortName,
      exchange: grant.exchange,
      currency: grant.currency,
      grantDate: grant.grantDate,
      quantity: grant.quantity,
      vestingYears: grant.vestingYears,
      vestingFrequency: grant.vestingFrequency,
      note: grant.note ?? ''
    };
    this.applyExchange(grant.exchange);
    this.grantDialog = true;
  }

  hideDialog(): void {
    this.grantDialog = false;
  }

  saveGrant(): void {
    if (!this.canSave) {
      return;
    }
    this.grant.note = this.grant.note?.trim() || null;
    this.grant.currency = this.grant.currency?.trim() || null;

    const call = this.grant.id === undefined
      ? this.equityService.create(this.grant)
      : this.equityService.update(this.grant);

    call.subscribe({
      next: () => {
        this.grantDialog = false;
        this.selectedGrants = [];
        this.fetchData();
      },
      error: (error) => this.showError(error, 'Could not save the grant.')
    });
  }

  deleteClicked(): void {
    this.deleteByIds(this.selectedGrants.map(grant => grant.id).join(','));
  }

  deleteByIds(ids: string): void {
    this.equityService.deleteByIds(ids).subscribe({
      next: () => {
        this.selectedGrants = [];
        this.fetchData();
      },
      error: (error) => this.showError(error, 'Could not delete the grant.')
    });
  }

  download(): void {
    this.equityService.downloadCsv().subscribe({
      next: (data) => {
        let a = document.createElement('a');
        a.href = window.URL.createObjectURL(data as Blob);
        a.download = 'rsu-grants.csv';
        a.click();
      },
      error: (error) => this.showError(error, 'Could not export the grants.')
    });
  }

  exchangeChanged(): void {
    this.selectedStock = {} as Symbol;
    this.grant.exchange = this.selectedExchange?.Code ?? '';
    this.grant.shortName = '';
    this.loadSymbols();
  }

  stockChanged(): void {
    this.grant.shortName = this.selectedStock?.Code ?? '';
  }

  private fetchData(): void {
    this.loading = true;
    this.equityService.getGrants().subscribe({
      next: (data) => {
        this.loading = false;
        this.report = data;
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.loading = false;
        this.report = null;
        this.showError(error, 'Could not load the grants.');
        this.cdr.markForCheck();
      }
    });
  }

  private loadExchanges(): void {
    this.stockService.getAllExchanges().subscribe({
      next: (data) => {
        this.exchangesLoading = false;
        this.exchanges = data;
        this.applyExchange(this.grant.exchange);
        this.cdr.markForCheck();
      },
      error: () => {
        this.exchangesLoading = false;
        this.cdr.markForCheck();
      }
    });
  }

  private applyExchange(code: string): void {
    const match = this.exchanges.find(exchange => exchange.Code === code);
    if (!match) {
      return;
    }
    this.selectedExchange = match;
    this.loadSymbols();
  }

  private loadSymbols(): void {
    if (!this.selectedExchange?.Code) {
      this.symbols = [];
      return;
    }
    this.stocksLoading = true;
    this.stockService.getAllSymbols(this.selectedExchange.Code).subscribe({
      next: (data) => {
        this.stocksLoading = false;
        this.symbols = data;
        this.selectedStock = data.find(symbol => symbol.Code === this.grant.shortName) ?? {} as Symbol;
        this.cdr.markForCheck();
      },
      error: () => {
        this.stocksLoading = false;
        this.symbols = [];
        this.cdr.markForCheck();
      }
    });
  }

  private showError(error: any, fallback: string): void {
    this.messageService.add({
      severity: 'error',
      summary: 'Something went wrong',
      detail: error?.error?.error ?? fallback,
      life: 8000
    });
  }

  private emptyGrant(): RSUGrant {
    return {
      shortName: '',
      exchange: 'US',
      currency: null,
      grantDate: '',
      quantity: null,
      vestingYears: 4,
      vestingFrequency: 'ANNUAL',
      note: ''
    };
  }
}
