import { ChangeDetectorRef, Component } from '@angular/core';
import { MessageService, PrimeTemplate } from 'primeng/api';
import { RSU, RSUVestTaxEvent, RSUVestTaxReport, StockRSUTaxReport, TaxBreakdown, TaxReport, TaxableEvent,
  TaxableEventReport } from '../model/rsu';
import { TaxService } from '../service/tax.service';
import { StockService } from '../service/stock.service';
import { Exchange } from '../model/exchange';
import { Symbol } from '../model/symbol';
import { MenuComponent } from '../menu/menu.component';
import { ExchangeOptionComponent } from '../util/exchange-option.component';
import { SymbolOptionComponent } from '../util/symbol-option.component';
import { Bind } from 'primeng/bind';
import { Panel } from 'primeng/panel';
import { Tabs, TabList, Tab, TabPanels, TabPanel } from 'primeng/tabs';
import { ButtonDirective } from 'primeng/button';
import { Ripple } from 'primeng/ripple';
import { Tooltip } from 'primeng/tooltip';
import { TableModule } from 'primeng/table';
import { InputText } from 'primeng/inputtext';
import { InputNumber } from 'primeng/inputnumber';
import { Select } from 'primeng/select';
import { Toast } from 'primeng/toast';
import { FormsModule } from '@angular/forms';
import { DatePipe, DecimalPipe } from '@angular/common';

type SelectedTotals = Omit<TaxBreakdown, 'amount'> & { amountInHuf: number };

@Component({
    selector: 'app-tax',
    templateUrl: './tax.component.html',
    styleUrls: ['./tax.component.css'],
    imports: [MenuComponent, Bind, Panel, Tabs, TabList, Tab, TabPanels, TabPanel, ButtonDirective, Ripple,
        Tooltip, TableModule, PrimeTemplate, InputText, InputNumber, Select, Toast,
        FormsModule, DecimalPipe, DatePipe, ExchangeOptionComponent, SymbolOptionComponent]
})
export class TaxComponent {

  private static readonly TRANSACTION_TAB = '2';
  private static readonly STOCK_TAB = '3';
  private static readonly EQUITY_TAB = '4';

  rsus: RSU[] = [];
  report: TaxReport | null = null;
  calculating: boolean = false;

  date: string = '';
  quantity: number | null = null;

  exchanges: Exchange[] = [];
  symbols: Symbol[] = [];
  selectedExchange: Exchange = {} as Exchange;
  selectedStock: Symbol = {} as Symbol;
  exchangesLoading: boolean = true;
  stocksLoading: boolean = false;

  amount: number | null = null;
  amountTax: TaxBreakdown | null = null;
  amountCalculating: boolean = false;

  taxableReport: TaxableEventReport | null = null;
  taxableLoading: boolean = false;
  selectedTaxableEvents: TaxableEvent[] = [];

  stockReport: StockRSUTaxReport | null = null;
  stockLoading: boolean = false;

  equityReport: RSUVestTaxReport | null = null;
  equityLoading: boolean = false;
  equityRecalculating: boolean = false;
  selectedEquityEvents: RSUVestTaxEvent[] = [];

  constructor(private taxService: TaxService, private stockService: StockService,
    private messageService: MessageService, private cdr: ChangeDetectorRef) {
    this.stockService.getAllExchanges().subscribe({
      next: (data) => {
        this.exchangesLoading = false;
        this.exchanges = data;
        const us = data.find(e => e.Code === 'US');
        if (us) {
          this.selectedExchange = us;
          this.loadSymbols();
        }
        this.cdr.markForCheck();
      },
      error: () => {
        this.exchangesLoading = false;
        this.cdr.markForCheck();
      }
    });
  }

  onTabChange(value: string | number | undefined): void {
    if (String(value) === TaxComponent.TRANSACTION_TAB && this.taxableReport === null) {
      this.loadTaxableEvents();
    }
    if (String(value) === TaxComponent.STOCK_TAB && this.stockReport === null) {
      this.loadStockRSUEvents();
    }
    if (String(value) === TaxComponent.EQUITY_TAB && this.equityReport === null) {
      this.loadEquityEvents();
    }
  }

  loadTaxableEvents(): void {
    this.taxableLoading = true;
    this.taxService.getTaxableEvents().subscribe({
      next: (data) => {
        this.taxableLoading = false;
        this.taxableReport = data;
        const ids = new Set(this.selectedTaxableEvents.map(e => e.id));
        this.selectedTaxableEvents = data.items.filter(e => ids.has(e.id));
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.taxableLoading = false;
        this.taxableReport = null;
        this.selectedTaxableEvents = [];
        this.showError(error);
        this.cdr.markForCheck();
      }
    });
  }

  get selectedTaxableTotals(): SelectedTotals {
    return this.sumTaxes(this.selectedTaxableEvents);
  }

  selectUnpaidTaxableEvents(): void {
    this.selectedTaxableEvents = this.taxableReport?.items.filter(e => !e.paid) ?? [];
  }

  clearTaxableSelection(): void {
    this.selectedTaxableEvents = [];
  }

  setPaid(id: number, paid: boolean): void {
    this.taxService.setTaxPaid(String(id), paid).subscribe({
      next: () => this.loadTaxableEvents(),
      error: () => this.messageService.add({
        severity: 'error',
        summary: 'Could not update',
        detail: 'Could not update the payment status.',
        life: 8000
      })
    });
  }

  downloadTaxableEvents(): void {
    this.taxService.downloadTaxableEventsCsv().subscribe({
      next: (data) => {
        let a = document.createElement('a');
        a.href = window.URL.createObjectURL(data as Blob);
        a.download = 'taxable-events.csv';
        a.click();
      },
      error: (error) => this.showError(error)
    });
  }

  loadStockRSUEvents(): void {
    this.stockLoading = true;
    this.taxService.getStockRSUEvents().subscribe({
      next: (data) => {
        this.stockLoading = false;
        this.stockReport = data;
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.stockLoading = false;
        this.stockReport = null;
        this.showError(error);
        this.cdr.markForCheck();
      }
    });
  }

  setStockPaid(id: number, paid: boolean): void {
    this.taxService.setStockRSUPaid(String(id), paid).subscribe({
      next: () => this.loadStockRSUEvents(),
      error: () => this.messageService.add({
        severity: 'error',
        summary: 'Could not update',
        detail: 'Could not update the payment status.',
        life: 8000
      })
    });
  }

  downloadStockRSUEvents(): void {
    this.taxService.downloadStockRSUCsv().subscribe({
      next: (data) => {
        let a = document.createElement('a');
        a.href = window.URL.createObjectURL(data as Blob);
        a.download = 'rsu-transactions.csv';
        a.click();
      },
      error: (error) => this.showError(error)
    });
  }

  loadEquityEvents(): void {
    this.equityLoading = true;
    this.taxService.getEquityEvents().subscribe({
      next: (data) => {
        this.equityLoading = false;
        this.equityReport = data;
        const ids = new Set(this.selectedEquityEvents.map(e => e.id));
        this.selectedEquityEvents = data.items.filter(e => ids.has(e.id));
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.equityLoading = false;
        this.equityReport = null;
        this.selectedEquityEvents = [];
        this.showError(error);
        this.cdr.markForCheck();
      }
    });
  }

  setEquityPaid(id: number, paid: boolean): void {
    this.taxService.setEquityPaid(String(id), paid).subscribe({
      next: () => this.loadEquityEvents(),
      error: () => this.messageService.add({
        severity: 'error',
        summary: 'Could not update',
        detail: 'Could not update the payment status.',
        life: 8000
      })
    });
  }

  recalculateSelectedEquity(): void {
    if (this.selectedEquityEvents.length === 0) return;

    this.equityRecalculating = true;
    this.taxService.recalculateEquity(this.selectedEquityEvents.map(e => e.id).join(',')).subscribe({
      next: () => {
        this.equityRecalculating = false;
        this.loadEquityEvents();
      },
      error: (error) => {
        this.equityRecalculating = false;
        this.showError(error);
        this.cdr.markForCheck();
      }
    });
  }

  downloadEquityEvents(): void {
    this.taxService.downloadEquityCsv().subscribe({
      next: (data) => {
        let a = document.createElement('a');
        a.href = window.URL.createObjectURL(data as Blob);
        a.download = 'rsu-vests.csv';
        a.click();
      },
      error: (error) => this.showError(error)
    });
  }

  get selectedEquityTotals(): SelectedTotals {
    return this.sumTaxes(this.selectedEquityEvents);
  }

  selectUnpaidEquityEvents(): void {
    this.selectedEquityEvents = this.equityReport?.items.filter(e => !e.paid) ?? [];
  }

  clearEquitySelection(): void {
    this.selectedEquityEvents = [];
  }

  private sumTaxes(items: { amountInHuf: number; tax: TaxBreakdown }[]): SelectedTotals {
    return items.reduce((sum, e) => ({
      amountInHuf: sum.amountInHuf + e.amountInHuf,
      taxBase: sum.taxBase + e.tax.taxBase,
      szocho: sum.szocho + e.tax.szocho,
      szja: sum.szja + e.tax.szja,
      total: sum.total + e.tax.total
    }), { amountInHuf: 0, taxBase: 0, szocho: 0, szja: 0, total: 0 });
  }

  exchangeChanged(event: any): void {
    this.selectedStock = {} as Symbol;
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
        this.cdr.markForCheck();
      },
      error: () => {
        this.stocksLoading = false;
        this.symbols = [];
        this.cdr.markForCheck();
      }
    });
  }

  get canAdd(): boolean {
    return !!this.selectedStock?.Code && !!this.selectedExchange?.Code
      && this.isDate(this.date) && !!this.quantity && this.quantity > 0;
  }

  addRSU(): void {
    if (!this.canAdd) return;

    this.rsus = [...this.rsus, {
      shortName: this.selectedStock.Code,
      exchange: this.selectedExchange.Code,
      date: this.date.trim(),
      quantity: this.quantity!
    }];

    this.selectedStock = {} as Symbol;
    this.date = '';
    this.quantity = null;
    this.report = null;
  }

  removeRSU(index: number): void {
    this.rsus = this.rsus.filter((_, i) => i !== index);
    this.report = null;
  }

  clear(): void {
    this.rsus = [];
    this.report = null;
  }

  calculate(): void {
    if (this.rsus.length === 0) return;

    this.calculating = true;
    this.taxService.calculateForRSUs(this.rsus).subscribe({
      next: (data) => {
        this.calculating = false;
        this.report = data;
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.calculating = false;
        this.report = null;
        this.showError(error);
        this.cdr.markForCheck();
      }
    });
  }

  download(): void {
    if (this.rsus.length === 0) return;

    this.taxService.downloadRSUCsv(this.rsus).subscribe({
      next: (data) => {
        let a = document.createElement('a');
        a.href = window.URL.createObjectURL(data as Blob);
        a.download = 'rsu-tax.csv';
        a.click();
      },
      error: (error) => this.showError(error)
    });
  }

  calculateAmount(): void {
    if (this.amount === null) return;

    this.amountCalculating = true;
    this.taxService.calculateForAmount(this.amount).subscribe({
      next: (data) => {
        this.amountCalculating = false;
        this.amountTax = data;
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.amountCalculating = false;
        this.amountTax = null;
        this.showError(error);
        this.cdr.markForCheck();
      }
    });
  }

  /** The rate or close actually used differs from the requested date on weekends and holidays. */
  isFallback(requested: string, used: string): boolean {
    return requested !== used;
  }

  private showError(error: any): void {
    this.messageService.add({
      severity: 'error',
      summary: 'Could not calculate',
      detail: error?.error?.error ?? 'Something went wrong, please try again.',
      life: 8000
    });
  }

  /** Keeps Add disabled until the typed date is a real YYYY-MM-DD, rather than failing at the backend. */
  private isDate(value: string): boolean {
    const trimmed = value?.trim();
    if (!/^\d{4}-\d{2}-\d{2}$/.test(trimmed)) return false;
    return !Number.isNaN(Date.parse(trimmed));
  }
}
