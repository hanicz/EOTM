import { Component, ChangeDetectorRef, EventEmitter, Output, ViewChild, inject } from '@angular/core';
import { from, of } from 'rxjs';
import { catchError, concatMap, map, toArray } from 'rxjs/operators';
import { BankTransaction, CATEGORY_CHIP_CLASS, CATEGORY_CHIP_NONE, CategoryColor, CategoryRule, SpendingCategory } from '../../model/bankTransaction';
import { ImportResult } from '../../model/importResult';
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
import { Dialog } from 'primeng/dialog';
import { Checkbox } from 'primeng/checkbox';
import { FormsModule } from '@angular/forms';
import { CurrencyPipe, DatePipe, NgClass } from '@angular/common';
import { CsvDropDirective } from '../../util/csv-drop.directive';

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
    hostDirectives: [CsvDropDirective],
    templateUrl: './transaction.component.html',
    styleUrls: ['./transaction.component.css'],
    imports: [Bind, Toolbar, PrimeTemplate, Toast, ButtonDirective, Ripple, Tooltip, FileUpload, TableModule,
        InputText, Select, Dialog, Checkbox, FormsModule, CurrencyPipe, DatePipe, NgClass]
})
export class FinancialTransactionComponent {

  @Output() dataChanged = new EventEmitter<void>();
  @Output() createRule = new EventEmitter<string>();
  @Output() categoryRulesChanged = new EventEmitter<void>();

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
  categoryFilter: number | null = null;
  categories: SpendingCategory[] = [];
  bulkCategoryId: number | null = null;
  categoryRuleDialog: boolean = false;
  categoryRuleSource: BankTransaction | null = null;
  categoryRulePattern: string = '';
  categoryRuleCategoryId: number | null = null;
  categoryRuleApplyToOthers: boolean = true;
  categoryRuleMatchCount: number = 0;
  categoryRuleUncategorizedCount: number = 0;
  savingCategoryRule: boolean = false;
  readonly patternMaxLength = 64;
  readonly memoMaxLength = 500;
  private readonly editableFields = ['bookingDate', 'memo'];
  private beforeEdit: TransactionEditValues | null = null;
  @ViewChild('fileUpload') fileUpload: any;


  constructor(private financialService: FinancialService, private cdr: ChangeDetectorRef,
    private messageService: MessageService) {
    const csvDrop = inject(CsvDropDirective);
    csvDrop.multiple.set(true);
    csvDrop.csvDropped.subscribe(event => this.onUpload(event));
    this.fetchData();
    this.fetchCategories();
  }

  refresh(): void {
    this.fetchData();
    this.fetchCategories();
  }

  private fetchCategories(): void {
    this.financialService.getCategories().subscribe({
      next: (data) => {
        this.categories = data;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.log(error);
      }
    });
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
    return !!this.fromDate || !!this.toDate || !!this.flagFilter || this.categoryFilter !== null;
  }

  filterChanged(): void {
    this.selectedTransactions = [];
    this.applyFilters();
  }

  clearFilters(): void {
    this.fromDate = '';
    this.toDate = '';
    this.flagFilter = null;
    this.categoryFilter = null;
    this.filterChanged();
  }

  private applyFilters(): void {
    this.filteredTransactions = this.transactions.filter(transaction => {
      const booked = this.bookedOn(transaction);
      return (!this.fromDate || booked >= this.fromDate)
        && (!this.toDate || booked <= this.toDate)
        && this.matchesFlag(transaction)
        && this.matchesCategory(transaction);
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

  private matchesCategory(transaction: BankTransaction): boolean {
    if (this.categoryFilter === null) {
      return true;
    }
    if (this.categoryFilter === 0) {
      return transaction.categoryId === null;
    }
    return transaction.categoryId === this.categoryFilter;
  }

  chipClass(color: CategoryColor | null): string {
    return color ? CATEGORY_CHIP_CLASS[color] : CATEGORY_CHIP_NONE;
  }

  get categoryOptions(): { label: string, value: number }[] {
    return [{ label: 'Uncategorized', value: 0 }]
      .concat(this.categories.map(category => ({ label: category.name, value: category.id })));
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

  categoryClicked(): void {
    const ids = this.selectedTransactions.map(t => t.id).join(',');
    this.financialService.setCategory(ids, this.bulkCategoryId === 0 ? null : this.bulkCategoryId).subscribe({
      next: () => {
        this.selectedTransactions = [];
        this.bulkCategoryId = null;
        this.fetchData();
        this.dataChanged.emit();
      },
      error: () => {
        this.messageService.add({ severity: 'error', detail: 'Could not set the category.' });
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

  categoryRuleClicked(transaction: BankTransaction): void {
    const partnerName = this.normalizeName(transaction.partnerName);
    if (!partnerName) {
      this.messageService.add({ severity: 'warn', detail: 'This record has no partner name to build a rule on.' });
      return;
    }
    if (!this.categories.length) {
      this.messageService.add({ severity: 'warn', detail: 'Add a category first, a rule has to point at one.' });
      return;
    }
    this.categoryRuleSource = transaction;
    this.categoryRulePattern = partnerName.substring(0, this.patternMaxLength);
    this.categoryRuleCategoryId = transaction.categoryId ?? this.categories[0].id;
    this.categoryRuleApplyToOthers = true;
    this.updateCategoryRuleMatches();
    this.categoryRuleDialog = true;
  }

  updateCategoryRuleMatches(): void {
    const pattern = this.normalizeName(this.categoryRulePattern);
    const matching = pattern
      ? this.transactions.filter(transaction => this.normalizeName(transaction.partnerName).includes(pattern))
      : [];
    this.categoryRuleMatchCount = matching.length;
    this.categoryRuleUncategorizedCount = matching.filter(transaction => transaction.categoryId === null).length;
  }

  hideCategoryRuleDialog(): void {
    this.categoryRuleDialog = false;
  }

  saveCategoryRule(): void {
    const source = this.categoryRuleSource;
    const pattern = this.categoryRulePattern.trim();
    const categoryId = this.categoryRuleCategoryId;
    if (!source || !pattern || categoryId === null) {
      return;
    }
    const applyToOthers = this.categoryRuleApplyToOthers;
    const rule = { name: null, pattern, categoryId, priority: 0, active: true } as CategoryRule;
    this.savingCategoryRule = true;

    this.financialService.createCategoryRule(rule).pipe(
      concatMap(() => this.financialService.setCategory(String(source.id), categoryId)),
      concatMap(() => applyToOthers
        ? this.financialService.applyCategoryRules().pipe(map(result => result.categorized as number | null))
        : of(null))
    ).subscribe({
      next: (categorized) => {
        this.savingCategoryRule = false;
        this.categoryRuleDialog = false;
        this.fetchData();
        this.dataChanged.emit();
        this.categoryRulesChanged.emit();
        this.messageService.add({
          severity: 'success',
          detail: categorized === null
            ? 'Rule saved and this transaction categorized.'
            : `Rule saved and this transaction categorized. Re-applying the rules categorized ${categorized} more.`
        });
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.savingCategoryRule = false;
        this.messageService.add({ severity: 'error', detail: error.error?.error ?? 'Could not save the rule.' });
        this.cdr.markForCheck();
      }
    });
  }

  private normalizeName(value: string | null | undefined): string {
    return (value ?? '').replace(/\s+/g, ' ').trim().toUpperCase();
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
