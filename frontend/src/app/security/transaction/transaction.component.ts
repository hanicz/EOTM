import { Component, ElementRef, inject, OnInit, ViewChild, ChangeDetectorRef } from '@angular/core';
import { Input } from '@angular/core';
import { EMPTY_FILTER, PortfolioFilter, filterRows } from '../../util/tablefilter';
import { CsvDropDirective } from '../../util/csv-drop.directive';
import { SecurityTransaction } from '../../model/securityTransaction';
import { SecurityService } from '../../service/security.service';
import { Globals } from '../../util/global';
import { Security } from 'src/app/model/security';
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
import { CurrencyPipe, DatePipe, NgClass } from '@angular/common';

@Component({
    selector: 'app-security-transaction',
    hostDirectives: [CsvDropDirective],
    templateUrl: './transaction.component.html',
    imports: [Bind, Toolbar, PrimeTemplate, ButtonDirective, Ripple, Tooltip, TableModule, InputText, Select, FormsModule, Dialog, CurrencyPipe, DatePipe, Toast, NgClass]
})
export class TransactionComponent implements OnInit {
  visibleTransactions: SecurityTransaction[] = [];
  private activeFilter: PortfolioFilter = EMPTY_FILTER;

  @Input() set filter(value: PortfolioFilter) {
    this.activeFilter = value ?? EMPTY_FILTER;
    this.applyFilter();
  }

  private applyFilter(): void {
    this.visibleTransactions = filterRows(this.transactions, this.activeFilter, ['securityName', 'securityId']);
  }


  transactions: SecurityTransaction[] = [];
  currencies: any[];
  statuses: any[];
  selectedTransactions: SecurityTransaction[] = [];
  transactionDialog: boolean = false;
  transaction: SecurityTransaction = {} as SecurityTransaction;
  @ViewChild('fileInput') fileInput?: ElementRef<HTMLInputElement>;
  globals: Globals;
  securities: Security[] = [];
  selectedExistingSecurity: Security | null = null;

  constructor(private securityService: SecurityService, globals: Globals, private cdr: ChangeDetectorRef, private messageService: MessageService) {
    inject(CsvDropDirective).csvDropped.subscribe(event => this.onUpload(event));
    this.globals = globals;
    this.currencies = globals.currencies;

    this.statuses = [
      { label: 'BUY', value: 'B' },
      { label: 'SELL', value: 'S' }
    ];

    this.securityService.getAllSecurities().subscribe({
      next: (data) => {
        this.securities = data;
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
    this.securityService.getTransactions().subscribe({
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
    this.transaction = {} as SecurityTransaction;
    this.selectedExistingSecurity = null;
    this.transactionDialog = true;
  }

  hideDialog() {
    this.transactionDialog = false;
  }

  editTransaction(transaction: SecurityTransaction) {
    this.transaction = { ...transaction };
    this.selectedExistingSecurity = this.securities.find(s => s.id === transaction.securityId) ?? null;
    this.transactionDialog = true;
  }

  existingSecurityChanged(): void {
    if (this.selectedExistingSecurity) {
      this.transaction.securityId = this.selectedExistingSecurity.id;
      this.transaction.securityName = this.selectedExistingSecurity.name;
    }
  }

  deleteClicked() {
    const ids = this.selectedTransactions.map(i => i.transactionId).join(',');
    this.deleteByIds(ids);
  }

  deleteByIds(ids: string) {
    this.securityService.deleteByIds(ids).subscribe({
      next: () => {
        this.selectedTransactions = [];
        this.fetchData();
      }
    });
  }

  download() {
    this.securityService.download().subscribe({
      next: (data) => {
        let fileName = 'security_transactions.csv';
        let a = document.createElement('a');
        a.href = window.URL.createObjectURL(data as Blob);
        a.download = fileName;
        a.click();
      }
    });
  }

  saveTransaction() {
    if (this.transaction.transactionId === undefined) {
      this.securityService.create(this.transaction).subscribe({
        next: () => {
          this.fetchData();
          this.transactionDialog = false;
        }
      });
    } else {
      this.securityService.update(this.transaction).subscribe({
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
      this.securityService.uploadCSV(file).subscribe({
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
