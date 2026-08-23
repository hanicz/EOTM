import { Component, ChangeDetectorRef, EventEmitter, Output, ViewChild } from '@angular/core';
import { BankTransaction } from '../../model/bankTransaction';
import { FinancialService } from '../../service/financial.service';
import { Bind } from 'primeng/bind';
import { Toolbar } from 'primeng/toolbar';
import { MessageService, PrimeTemplate } from 'primeng/api';
import { Toast } from 'primeng/toast';
import { ButtonDirective } from 'primeng/button';
import { Ripple } from 'primeng/ripple';
import { Tooltip } from 'primeng/tooltip';
import { FileUpload } from 'primeng/fileupload';
import { TableModule } from 'primeng/table';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { Tag } from 'primeng/tag';
import { FormsModule } from '@angular/forms';
import { CurrencyPipe, DatePipe } from '@angular/common';

interface MemoEditEvent {
    field?: string;
    data?: BankTransaction;
}

@Component({
    selector: 'app-financial-transaction',
    templateUrl: './transaction.component.html',
    styleUrls: ['./transaction.component.css'],
    imports: [Bind, Toolbar, PrimeTemplate, Toast, ButtonDirective, Ripple, Tooltip, FileUpload, TableModule,
        InputText, Select, Tag, FormsModule, CurrencyPipe, DatePipe]
})
export class FinancialTransactionComponent {

  @Output() dataChanged = new EventEmitter<void>();

  transactions: BankTransaction[] = [];
  filteredTransactions: BankTransaction[] = [];
  selectedTransactions: BankTransaction[] = [];
  types: { label: string, value: string }[] = [];
  fromDate: string = '';
  toDate: string = '';
  readonly memoMaxLength = 500;
  private memoBeforeEdit = '';
  @ViewChild('fileUpload') fileUpload: any;


  constructor(private financialService: FinancialService, private cdr: ChangeDetectorRef,
    private messageService: MessageService) {
    this.fetchData();
  }

  refresh(): void {
    this.fetchData();
  }

  private fetchData(): void {
    this.financialService.getTransactions().subscribe({
      next: (data) => {
        this.transactions = data;
        this.types = [...new Set(data.map(t => t.type))].sort().map(type => ({ label: type, value: type }));
        this.applyDateFilter();
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.log(error);
      }
    });
  }

  get hasDateFilter(): boolean {
    return !!this.fromDate || !!this.toDate;
  }

  dateFilterChanged(): void {
    this.selectedTransactions = [];
    this.applyDateFilter();
  }

  clearDateFilter(): void {
    this.fromDate = '';
    this.toDate = '';
    this.dateFilterChanged();
  }

  private applyDateFilter(): void {
    this.filteredTransactions = this.transactions.filter(transaction => {
      const booked = this.bookedOn(transaction);
      return (!this.fromDate || booked >= this.fromDate) && (!this.toDate || booked <= this.toDate);
    });
  }

  private bookedOn(transaction: BankTransaction): string {
    const booked: unknown = transaction.bookingDate;
    return typeof booked === 'string'
      ? booked.substring(0, 10)
      : new Date(booked as Date).toLocaleDateString('en-CA');
  }

  excludeClicked(excluded: boolean): void {
    const ids = this.selectedTransactions.map(t => t.id).join(',');
    this.setExcluded(ids, excluded);
  }

  setExcluded(ids: string, excluded: boolean): void {
    this.financialService.setExcluded(ids, excluded).subscribe({
      next: () => {
        this.selectedTransactions = [];
        this.fetchData();
        this.dataChanged.emit();
      },
      error: () => {
        this.messageService.add({ severity: 'error', detail: 'Could not update the exclusion.' });
      }
    });
  }

  taxableClicked(taxable: boolean): void {
    const ids = this.selectedTransactions.map(t => t.id).join(',');
    this.setTaxable(ids, taxable);
  }

  setTaxable(ids: string, taxable: boolean): void {
    this.financialService.setTaxable(ids, taxable).subscribe({
      next: () => {
        this.selectedTransactions = [];
        this.fetchData();
        this.dataChanged.emit();
      },
      error: (error) => {
        this.messageService.add({ severity: 'error', detail: error.error?.error ?? 'Could not update the taxable flag.' });
      }
    });
  }

  onMemoEditInit(event: MemoEditEvent): void {
    if (event.field !== 'memo' || !event.data) return;
    this.memoBeforeEdit = event.data.memo ?? '';
  }

  onMemoEditCancel(event: MemoEditEvent): void {
    if (event.field !== 'memo' || !event.data) return;
    event.data.memo = this.memoBeforeEdit;
  }

  onMemoEditComplete(event: MemoEditEvent): void {
    if (event.field !== 'memo' || !event.data) return;

    const transaction = event.data;
    const previous = this.memoBeforeEdit;
    const memo = (transaction.memo ?? '').trim();

    if (memo === previous) {
      transaction.memo = previous;
      return;
    }

    transaction.memo = memo;
    this.financialService.updateMemo(transaction.id, memo).subscribe({
      error: (error) => {
        transaction.memo = previous;
        this.cdr.markForCheck();
        this.messageService.add({
          severity: 'error',
          detail: error.error?.error ?? 'Could not save the memo.'
        });
      }
    });
  }

  deleteClicked(): void {
    const ids = this.selectedTransactions.map(t => t.id).join(',');
    this.deleteByIds(ids);
  }

  deleteByIds(ids: string): void {
    this.financialService.deleteByIds(ids).subscribe({
      next: () => {
        this.selectedTransactions = [];
        this.fetchData();
        this.dataChanged.emit();
      }
    });
  }

  download(): void {
    this.financialService.download().subscribe({
      next: (data) => {
        const a = document.createElement('a');
        a.href = window.URL.createObjectURL(data as Blob);
        a.download = 'bank_transactions.csv';
        a.click();
      }
    });
  }

  onUpload(event: any): void {
    for (let file of event.files) {
      this.financialService.uploadCSV(file).subscribe({
        next: (result) => {
          this.fetchData();
          this.fileUpload.clear();
          this.dataChanged.emit();
          this.messageService.add({
            severity: 'success',
            detail: `Import finished. ${result.created} added, ${result.updated} updated.`
          });
        },
        error: (error) => {
          this.fileUpload.clear();
          this.messageService.add({ severity: 'error', detail: error.error?.error ?? 'Import failed.' });
        }
      });
    }
  }
}
