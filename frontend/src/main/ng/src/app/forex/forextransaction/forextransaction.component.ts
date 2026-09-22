import { Component, ElementRef, inject, OnInit, ViewChild, ChangeDetectorRef } from '@angular/core';
import { Input } from '@angular/core';
import { EMPTY_FILTER, PortfolioFilter, filterRows } from '../../util/tablefilter';
import { CsvDropDirective } from '../../util/csv-drop.directive';
import { ForexTransaction } from 'src/app/model/forextransaction';
import { ForexService } from 'src/app/service/forex.service';
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
import { CurrencyPipe, DatePipe } from '@angular/common';

@Component({
    selector: 'app-forextransaction',
    hostDirectives: [CsvDropDirective],
    templateUrl: './forextransaction.component.html',
    imports: [Bind, Toolbar, PrimeTemplate, ButtonDirective, Ripple, Tooltip, TableModule, InputText, Select, FormsModule, Dialog, CurrencyPipe, DatePipe, Toast]
})
export class ForextransactionComponent {
  visibleTransactions: ForexTransaction[] = [];
  private activeFilter: PortfolioFilter = EMPTY_FILTER;

  @Input() set filter(value: PortfolioFilter) {
    this.activeFilter = value ?? EMPTY_FILTER;
    this.applyFilter();
  }

  private applyFilter(): void {
    this.visibleTransactions = filterRows(this.forexTransactions, this.activeFilter, ['fromCurrencyId', 'toCurrencyId']);
  }


  forexTransactions: ForexTransaction[] = [];
  currencies: any[];
  statuses: any[];
  selectedForexTransactions: ForexTransaction[] = [];
  forexDialog: boolean = false;
  forexTransaction: ForexTransaction = {} as ForexTransaction;
  @ViewChild('fileInput') fileInput?: ElementRef<HTMLInputElement>;

  constructor(private forexService: ForexService, globals: Globals, private cdr: ChangeDetectorRef, private messageService: MessageService) {
    inject(CsvDropDirective).csvDropped.subscribe(event => this.onUpload(event));
    this.currencies = globals.currencies;

    this.statuses = [
      { label: 'BUY', value: 'B' },
      { label: 'SELL', value: 'S' }
    ];

    this.fetchData();
  }

  ngOnInit(): void {
  }

  refresh(): void {
    this.fetchData();
  }

  private fetchData(): void {
    this.forexService.getTransactions().subscribe({
      next: (data) => {
        this.forexTransactions = data;
        this.applyFilter();
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.log(error);
      }
    });
  }

  openNew() {
    this.forexTransaction = {} as ForexTransaction;
    this.forexDialog = true;
  }

  hideDialog() {
    this.forexDialog = false;
  }

  editForexTransaction(forexTransaction: ForexTransaction) {
    this.forexTransaction = { ...forexTransaction };
    this.forexDialog = true;
  }

  deleteClicked() {
    const ids = this.selectedForexTransactions.map(i => i.forexTransactionId).join(',');
    this.deleteByIds(ids);
  }

  deleteByIds(ids: string) {
    this.forexService.deleteByIds(ids).subscribe({
      next: () => {
        this.selectedForexTransactions = [];
        this.fetchData();
      }
    });
  }

  download() {
    this.forexService.download().subscribe({
      next: (data) => {
        let fileName = 'forexTransactions.csv';
        let a = document.createElement('a');
        a.href = window.URL.createObjectURL(data as Blob);
        a.download = fileName;
        a.click();
      }
    });
  }

  saveForexTransaction() {
    if (this.forexTransaction.forexTransactionId === undefined) {
      this.forexService.create(this.forexTransaction).subscribe({
        next: () => {
          this.fetchData();
          this.forexDialog = false;
        }
      });
    } else {
      this.forexService.update(this.forexTransaction).subscribe({
        next: () => {
          this.fetchData();
          this.forexDialog = false;
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
      this.forexService.uploadCSV(file).subscribe({
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
