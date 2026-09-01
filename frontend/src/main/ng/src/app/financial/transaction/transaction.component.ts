import { Component, ChangeDetectorRef, EventEmitter, Output, ViewChild } from '@angular/core';
import { from, of } from 'rxjs';
import { catchError, concatMap, map, toArray } from 'rxjs/operators';
import { BankTransaction, ImportResult } from '../../model/bankTransaction';
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
import { FormsModule } from '@angular/forms';
import { CurrencyPipe, DatePipe } from '@angular/common';

interface TransactionEditEvent {
    field?: string;
    data?: BankTransaction;
}

interface TransactionEditValues {
    bookingDate: string;
    memo: string;
}

interface ImportOutcome {
    name: string;
    result: ImportResult | null;
    error: string;
}

@Component({
    selector: 'app-financial-transaction',
    templateUrl: './transaction.component.html',
    styleUrls: ['./transaction.component.css'],
    imports: [Bind, Toolbar, PrimeTemplate, Toast, ButtonDirective, Ripple, Tooltip, FileUpload, TableModule,
        InputText, Select, FormsModule, CurrencyPipe, DatePipe]
})
export class FinancialTransactionComponent {

  @Output() dataChanged = new EventEmitter<void>();
  @Output() createRule = new EventEmitter<string>();

  transactions: BankTransaction[] = [];
  filteredTransactions: BankTransaction[] = [];
  selectedTransactions: BankTransaction[] = [];
  readonly flags: { label: string, value: string }[] = [
    { label: 'Taxable', value: 'taxable' },
    { label: 'Not taxable', value: 'notTaxable' },
    { label: 'Excluded', value: 'excluded' },
    { label: 'Counted', value: 'counted' }
  ];
  fromDate: string = '';
  toDate: string = '';
  flagFilter: string | null = null;
  readonly memoMaxLength = 500;
  private readonly editableFields = ['bookingDate', 'memo'];
  private beforeEdit: TransactionEditValues | null = null;
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
        this.applyFilters();
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.log(error);
      }
    });
  }

  get hasFilters(): boolean {
    return !!this.fromDate || !!this.toDate || !!this.flagFilter;
  }

  filterChanged(): void {
    this.selectedTransactions = [];
    this.applyFilters();
  }

  clearFilters(): void {
    this.fromDate = '';
    this.toDate = '';
    this.flagFilter = null;
    this.filterChanged();
  }

  private applyFilters(): void {
    this.filteredTransactions = this.transactions.filter(transaction => {
      const booked = this.bookedOn(transaction);
      return (!this.fromDate || booked >= this.fromDate)
        && (!this.toDate || booked <= this.toDate)
        && this.matchesFlag(transaction);
    });
  }

  private matchesFlag(transaction: BankTransaction): boolean {
    switch (this.flagFilter) {
      case 'taxable': return !!transaction.taxable;
      case 'notTaxable': return !transaction.taxable;
      case 'excluded': return !!transaction.excluded;
      case 'counted': return !transaction.excluded;
      default: return true;
    }
  }

  amountAlertClass(transaction: BankTransaction): string {
    if (transaction.excluded) return '';
    if (transaction.amount <= -500000) return 'amount-alert-3';
    if (transaction.amount <= -200000) return 'amount-alert-2';
    if (transaction.amount <= -100000) return 'amount-alert-1';
    return '';
  }

  private bookedOn(transaction: BankTransaction): string {
    return transaction.bookingDate.substring(0, 10);
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

  ruleClicked(transaction: BankTransaction): void {
    const account = transaction.partnerAccount?.trim();
    if (!account) {
      this.messageService.add({ severity: 'warn', detail: 'This record has no partner account to build a rule on.' });
      return;
    }
    this.createRule.emit(account);
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

  onEditInit(event: TransactionEditEvent): void {
    if (!this.isEditable(event) || !event.data) return;
    this.beforeEdit = { bookingDate: event.data.bookingDate, memo: event.data.memo ?? '' };
  }

  onEditCancel(event: TransactionEditEvent): void {
    if (!this.isEditable(event) || !event.data || !this.beforeEdit) return;
    event.data.bookingDate = this.beforeEdit.bookingDate;
    event.data.memo = this.beforeEdit.memo;
  }

  onEditComplete(event: TransactionEditEvent): void {
    if (!this.isEditable(event) || !event.data || !this.beforeEdit) return;

    const transaction = event.data;
    const previous = this.beforeEdit;
    const bookingDate = transaction.bookingDate;
    const memo = (transaction.memo ?? '').trim();

    if (!bookingDate) {
      transaction.bookingDate = previous.bookingDate;
      return;
    }

    if (bookingDate === previous.bookingDate && memo === previous.memo) {
      transaction.memo = previous.memo;
      return;
    }

    transaction.memo = memo;
    this.financialService.updateTransaction(transaction.id, bookingDate, memo).subscribe({
      next: () => {
        if (bookingDate !== previous.bookingDate) {
          this.applyFilters();
          this.dataChanged.emit();
        }
      },
      error: (error) => {
        transaction.bookingDate = previous.bookingDate;
        transaction.memo = previous.memo;
        this.cdr.markForCheck();
        this.messageService.add({
          severity: 'error',
          detail: error.error?.error ?? 'Could not save the change.'
        });
      }
    });
  }

  private isEditable(event: TransactionEditEvent): boolean {
    return !!event.field && this.editableFields.includes(event.field);
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

  onUpload(event: { files: File[] }): void {
    const files = Array.from(event.files ?? []);
    if (!files.length) return;

    from(files).pipe(
      concatMap(file => this.financialService.uploadCSV(file).pipe(
        map(result => ({ name: file.name, result, error: '' } as ImportOutcome)),
        catchError(error => of({
          name: file.name,
          result: null,
          error: error.error?.error ?? 'Import failed.'
        } as ImportOutcome))
      )),
      toArray()
    ).subscribe(outcomes => {
      this.fileUpload.clear();
      this.reportImport(outcomes);
    });
  }

  private reportImport(outcomes: ImportOutcome[]): void {
    const imported = outcomes.filter(outcome => outcome.result);
    const failed = outcomes.filter(outcome => !outcome.result);

    if (imported.length) {
      this.fetchData();
      this.dataChanged.emit();
      const created = imported.reduce((sum, outcome) => sum + outcome.result!.created, 0);
      const updated = imported.reduce((sum, outcome) => sum + outcome.result!.updated, 0);
      const fileCount = imported.length > 1 ? ` from ${imported.length} files` : '';
      this.messageService.add({
        severity: 'success',
        detail: `Import finished. ${created} added, ${updated} updated${fileCount}.`
      });
    }

    if (failed.length === 1) {
      this.messageService.add({ severity: 'error', detail: `${failed[0].name}: ${failed[0].error}` });
    } else if (failed.length > 1) {
      const names = failed.map(outcome => outcome.name).join(', ');
      this.messageService.add({ severity: 'error', detail: `${failed.length} files could not be imported: ${names}` });
    }
  }
}
