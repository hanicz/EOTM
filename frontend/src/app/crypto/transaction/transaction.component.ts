import { Component, ElementRef, inject, OnInit, ViewChild, ChangeDetectorRef } from '@angular/core';
import { Input } from '@angular/core';
import { EMPTY_FILTER, PortfolioFilter, filterRows } from '../../util/tablefilter';
import { CsvDropDirective } from '../../util/csv-drop.directive';
import { Transaction } from '../../model/transaction';
import { CryptoService } from '../../service/crypto.service';
import { Globals } from '../../util/global';
import { environment } from '../../../environments/environment';
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
import { Image } from 'primeng/image';
import { Dialog } from 'primeng/dialog';
import { DecimalPipe, CurrencyPipe, DatePipe } from '@angular/common';

@Component({
    selector: 'app-transaction',
    hostDirectives: [CsvDropDirective],
    templateUrl: './transaction.component.html',
    imports: [Bind, Toolbar, PrimeTemplate, ButtonDirective, Ripple, Tooltip, TableModule, InputText, Select, FormsModule, Image, Dialog, DecimalPipe, CurrencyPipe, DatePipe, Toast]
})
export class TransactionComponent implements OnInit {
  visibleTransactions: Transaction[] = [];
  private activeFilter: PortfolioFilter = EMPTY_FILTER;

  @Input() set filter(value: PortfolioFilter) {
    this.activeFilter = value ?? EMPTY_FILTER;
    this.applyFilter();
  }

  private applyFilter(): void {
    this.visibleTransactions = filterRows(this.transactions, this.activeFilter, ['symbol']);
  }


  transactions: Transaction[] = [];
  currencies: any[];
  statuses: any[];
  selectedTransactions: Transaction[] = [];
  transactionDialog: boolean = false;
  transaction: Transaction = {} as Transaction;
  @ViewChild('fileInput') fileInput?: ElementRef<HTMLInputElement>;
  assetUrl: string;

  constructor(private cryptoService: CryptoService, globals: Globals, private cdr: ChangeDetectorRef, private messageService: MessageService) {
    inject(CsvDropDirective).csvDropped.subscribe(event => this.onUpload(event));
    this.currencies = globals.currencies;
    this.statuses = globals.statuses;
    this.assetUrl = environment.assets_url;
    this.fetchData();
  }

  ngOnInit(): void {
  }

  onChange(event: any): void {
    this.fetchData();
  }

  refresh(): void {
    this.fetchData();
  }

  private fetchData(): void {
    this.cryptoService.getTransactions().subscribe({
      next: (data) => {
        this.transactions = data;
        this.applyFilter();
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.log(error);
      }
    });
  }

  openNew() {
    this.transaction = {} as Transaction;
    this.transactionDialog = true;
  }

  hideDialog() {
    this.transactionDialog = false;
  }

  editTransaction(transaction: Transaction) {
    this.transaction = { ...transaction };
    this.transactionDialog = true;
  }

  deleteClicked() {
    const ids = this.selectedTransactions.map(i => i.id).join(',');
    this.deleteByIds(ids);
  }

  deleteByIds(ids: string) {
    this.cryptoService.deleteByIds(ids).subscribe({
      next: () => {
        this.selectedTransactions = [];
        this.fetchData();
      }
    });
  }

  download() {
    this.cryptoService.download().subscribe({
      next: (data) => {
        let fileName = 'transactions.csv';
        let a = document.createElement('a');
        a.href = window.URL.createObjectURL(data as Blob);
        a.download = fileName;
        a.click();
      }
    });
  }

  saveTransaction() {
    if (this.transaction.id === undefined) {
      this.cryptoService.create(this.transaction).subscribe({
        next: () => {
          this.fetchData();
          this.transactionDialog = false;
        }
      });
    } else {
      this.cryptoService.update(this.transaction).subscribe({
        next: () => {
          this.fetchData();
          this.transactionDialog = false;
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
      this.cryptoService.uploadCSV(file).subscribe({
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
