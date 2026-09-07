import { ChangeDetectorRef, Component } from '@angular/core';
import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MessageService, PrimeTemplate } from 'primeng/api';
import { Bind } from 'primeng/bind';
import { Toast } from 'primeng/toast';
import { Toolbar } from 'primeng/toolbar';
import { ButtonDirective } from 'primeng/button';
import { Ripple } from 'primeng/ripple';
import { Tooltip } from 'primeng/tooltip';
import { TableModule } from 'primeng/table';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { Dialog } from 'primeng/dialog';
import {
  CompensationAmountMode, CompensationDraft, CompensationItem, CompensationPackage, CompensationTaxTreatment
} from '../../model/compensation';
import { SalaryBasis } from '../../model/salary';
import { CompensationService } from '../../service/compensation.service';
import { Globals } from '../../util/global';
import { DeltaComponent } from '../../util/delta.component';

interface ComparisonRow {
  label: string;
  mine: number;
  theirs: number;
}

@Component({
    selector: 'app-salary-compensation',
    templateUrl: './compensation.component.html',
    styleUrls: ['./compensation.component.css'],
    imports: [Bind, Toolbar, PrimeTemplate, Toast, ButtonDirective, Ripple, Tooltip, TableModule,
        InputText, Select, Dialog, FormsModule, CurrencyPipe, DatePipe, DecimalPipe, DeltaComponent]
})
export class SalaryCompensationComponent {

  current: CompensationPackage | null = null;
  loaded: boolean = false;
  selectedItems: CompensationItem[] = [];
  itemDialog: boolean = false;
  item: CompensationItem = this.emptyItem();

  draft: CompensationDraft | null = null;
  compared: CompensationPackage | null = null;
  currencies: any[];

  readonly treatments: { label: string, value: CompensationTaxTreatment }[] = [
    { label: 'Taxed as salary', value: 'TAXED_AS_SALARY' },
    { label: 'Tax-free', value: 'TAX_FREE' },
    { label: 'Received net', value: 'RECEIVED_NET' }
  ];

  readonly amountModes: { label: string, value: CompensationAmountMode }[] = [
    { label: 'Amount a month', value: 'MONTHLY_AMOUNT' },
    { label: '% of annual base', value: 'PERCENT_OF_ANNUAL' }
  ];

  readonly bases: { label: string, value: SalaryBasis }[] = [
    { label: 'Monthly', value: 'MONTHLY' },
    { label: 'Annual', value: 'ANNUAL' }
  ];

  readonly nameMaxLength = 64;
  readonly noteMaxLength = 64;
  readonly maxPercent = 500;
  readonly maxDependents = 10;

  constructor(private compensationService: CompensationService, private cdr: ChangeDetectorRef,
    private messageService: MessageService, globals: Globals) {
    this.currencies = globals.currencies;
    this.fetchData();
  }

  private fetchData(): void {
    this.compensationService.getCurrentPackage().subscribe({
      next: (data) => {
        this.current = data ?? null;
        this.loaded = true;
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.loaded = true;
        this.messageService.add({ severity: 'error', detail: error.error?.error ?? 'Could not load the package.' });
        this.cdr.markForCheck();
      }
    });
  }

  rowsOf(pkg: CompensationPackage): CompensationItem[] {
    return [this.baseRow(pkg), ...pkg.items];
  }

  treatmentLabel(treatment: CompensationTaxTreatment): string {
    return this.treatments.find(option => option.value === treatment)?.label ?? treatment;
  }

  treatmentClass(treatment: CompensationTaxTreatment): string {
    if (treatment === 'TAXED_AS_SALARY') {
      return 'state-neutral';
    }
    return treatment === 'TAX_FREE' ? 'state-buy' : 'state-info';
  }

  openNew(): void {
    this.item = this.emptyItem();
    this.itemDialog = true;
  }

  editItem(item: CompensationItem): void {
    this.item = { ...item, note: item.note ?? '' };
    this.itemDialog = true;
  }

  hideDialog(): void {
    this.itemDialog = false;
  }

  modeChanged(item: CompensationItem): void {
    if (item.amountMode === 'PERCENT_OF_ANNUAL') {
      item.monthlyAmount = null;
    } else {
      item.percent = null;
    }
  }

  amountMissing(item: CompensationItem): boolean {
    return item.amountMode === 'PERCENT_OF_ANNUAL' ? !item.percent : !item.monthlyAmount;
  }

  incomplete(item: CompensationItem): boolean {
    return !item.name?.trim() || this.amountMissing(item);
  }

  saveItem(): void {
    if (this.incomplete(this.item)) {
      return;
    }
    this.item.name = this.item.name.trim();
    this.item.note = this.item.note?.trim() || null;

    const call = this.item.id === undefined
      ? this.compensationService.create(this.item)
      : this.compensationService.update(this.item);

    call.subscribe({
      next: () => {
        this.itemDialog = false;
        this.selectedItems = [];
        this.fetchData();
      },
      error: (error) => {
        this.messageService.add({ severity: 'error', detail: error.error?.error ?? 'Could not save the item.' });
      }
    });
  }

  deleteClicked(): void {
    this.deleteByIds(this.selectedItems.map(item => item.id).join(','));
  }

  deleteByIds(ids: string): void {
    this.compensationService.deleteByIds(ids).subscribe({
      next: () => {
        this.selectedItems = [];
        this.fetchData();
      },
      error: (error) => {
        this.messageService.add({ severity: 'error', detail: error.error?.error ?? 'Could not delete the item.' });
      }
    });
  }

  startDraft(): void {
    this.draft = {
      label: '',
      baseAmount: null,
      baseBasis: 'ANNUAL',
      currencyId: this.current?.currencyId ?? 'HUF',
      dependents: this.current?.dependents ?? 0,
      items: []
    };
    this.compared = null;
  }

  copyFromCurrent(): void {
    if (!this.current) {
      return;
    }
    this.draft = {
      label: '',
      baseAmount: this.current.baseGrossAnnual,
      baseBasis: 'ANNUAL',
      currencyId: this.current.currencyId,
      dependents: this.current.dependents,
      items: this.current.items.map(item => ({
        name: item.name,
        amountMode: item.amountMode,
        monthlyAmount: item.monthlyAmount,
        percent: item.percent,
        taxTreatment: item.taxTreatment,
        note: null
      }))
    };
    this.compared = null;
  }

  clearDraft(): void {
    this.draft = null;
    this.compared = null;
  }

  addDraftItem(): void {
    this.draft?.items.push(this.emptyItem());
    this.compared = null;
  }

  removeDraftItem(index: number): void {
    this.draft?.items.splice(index, 1);
    this.compared = null;
  }

  draftReady(): boolean {
    return !!this.draft?.baseAmount && !this.draft.items.some(item => this.incomplete(item));
  }

  compare(): void {
    if (!this.draft || !this.draftReady()) {
      return;
    }
    this.compensationService.compare(this.draft).subscribe({
      next: (data) => {
        this.compared = data;
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.messageService.add({ severity: 'error', detail: error.error?.error ?? 'Could not price the package.' });
      }
    });
  }

  comparable(): boolean {
    return !!this.current && !!this.compared && this.current.currencyId === this.compared.currencyId;
  }

  comparisonRows(): ComparisonRow[] {
    const mine = this.current;
    const theirs = this.compared;
    if (!mine || !theirs) {
      return [];
    }
    return [
      { label: 'Base gross monthly', mine: mine.baseGrossMonthly, theirs: theirs.baseGrossMonthly },
      { label: 'Base net monthly', mine: mine.baseNetMonthly, theirs: theirs.baseNetMonthly },
      { label: 'Total gross monthly', mine: mine.totalGrossMonthly, theirs: theirs.totalGrossMonthly },
      { label: 'Total gross annual', mine: mine.totalGrossAnnual, theirs: theirs.totalGrossAnnual },
      { label: 'Total net monthly', mine: mine.totalNetMonthly, theirs: theirs.totalNetMonthly },
      { label: 'Total net annual', mine: mine.totalNetAnnual, theirs: theirs.totalNetAnnual }
    ];
  }

  difference(row: ComparisonRow): number {
    return row.theirs - row.mine;
  }

  differencePercent(row: ComparisonRow): number | null {
    return row.mine > 0 ? ((row.theirs - row.mine) / row.mine) * 100 : null;
  }

  private baseRow(pkg: CompensationPackage): CompensationItem {
    return {
      name: pkg.label ?? 'Base salary',
      amountMode: 'MONTHLY_AMOUNT',
      monthlyAmount: pkg.baseGrossMonthly,
      percent: null,
      taxTreatment: 'TAXED_AS_SALARY',
      note: null,
      currencyId: pkg.currencyId,
      grossMonthly: pkg.baseGrossMonthly,
      grossAnnual: pkg.baseGrossAnnual,
      netMonthly: pkg.baseNetMonthly,
      netAnnual: pkg.baseNetAnnual,
      base: true
    };
  }

  private emptyItem(): CompensationItem {
    return {
      name: '',
      amountMode: 'MONTHLY_AMOUNT',
      monthlyAmount: null,
      percent: null,
      taxTreatment: 'TAXED_AS_SALARY',
      note: ''
    };
  }
}
