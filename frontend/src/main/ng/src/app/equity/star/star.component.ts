import { ChangeDetectorRef, Component } from '@angular/core';
import { MessageService, PrimeTemplate } from 'primeng/api';
import { STARGrant, STARGrantReport } from '../../model/equity';
import { EquityService } from '../../service/equity.service';
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
import { Checkbox } from 'primeng/checkbox';
import { Dialog } from 'primeng/dialog';
import { FormsModule } from '@angular/forms';
import { DatePipe, DecimalPipe } from '@angular/common';

@Component({
    selector: 'app-equity-star',
    templateUrl: './star.component.html',
    styleUrls: ['./star.component.css'],
    imports: [Bind, Toolbar, PrimeTemplate, Toast, ButtonDirective, Ripple, Tooltip, TableModule,
        InputText, InputNumber, Select, Checkbox, Dialog, FormsModule, DecimalPipe, DatePipe]
})
export class EquityStarComponent {

  report: STARGrantReport | null = null;
  loading: boolean = true;
  selectedGrants: STARGrant[] = [];
  expandedRows: { [key: string]: boolean } = {};

  grantDialog: boolean = false;
  grant: STARGrant = this.emptyGrant();

  readonly currencies = [
    { label: 'USD', value: 'USD' },
    { label: 'EUR', value: 'EUR' },
    { label: 'GBP', value: 'GBP' },
    { label: 'HUF', value: 'HUF' }
  ];
  readonly noteMaxLength = 64;
  readonly nameMaxLength = 32;
  readonly maxVestingYears = 10;

  constructor(private equityService: EquityService, private messageService: MessageService,
    private cdr: ChangeDetectorRef) {
    this.fetchData();
  }

  get canSave(): boolean {
    return !!this.grant.name?.trim() && !!this.grant.currency && !!this.grant.commencementDate
      && !!this.grant.quantity && this.grant.quantity > 0
      && this.grant.baseValue !== null && this.grant.baseValue >= 0
      && this.grant.currentValue !== null && this.grant.currentValue >= 0
      && this.grant.vestingYears >= 1 && this.grant.vestingYears <= this.maxVestingYears;
  }

  scheduleLabel(grant: STARGrant): string {
    return `${grant.vestingYears}y · 25% cliff`;
  }

  isUnderwater(grant: STARGrant): boolean {
    return (grant.spreadPerUnit ?? 0) <= 0;
  }

  applyAllLabel(): string {
    const name = this.grant.name?.trim();
    return name ? `Apply this current value to every STAR named ${name}`
      : 'Apply this current value to every STAR with this name';
  }

  openNew(): void {
    this.grant = this.emptyGrant();
    this.grantDialog = true;
  }

  editGrant(grant: STARGrant): void {
    this.grant = {
      id: grant.id,
      name: grant.name,
      currency: grant.currency,
      commencementDate: grant.commencementDate,
      quantity: grant.quantity,
      baseValue: grant.baseValue,
      currentValue: grant.currentValue,
      vestingYears: grant.vestingYears,
      note: grant.note ?? '',
      applyValueToAll: false
    };
    this.grantDialog = true;
  }

  hideDialog(): void {
    this.grantDialog = false;
  }

  saveGrant(): void {
    if (!this.canSave) {
      return;
    }
    this.grant.name = this.grant.name.trim();
    this.grant.note = this.grant.note?.trim() || null;

    const call = this.grant.id === undefined
      ? this.equityService.createStar(this.grant)
      : this.equityService.updateStar(this.grant);

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
    this.equityService.deleteStarByIds(ids).subscribe({
      next: () => {
        this.selectedGrants = [];
        this.fetchData();
      },
      error: (error) => this.showError(error, 'Could not delete the grant.')
    });
  }

  private fetchData(): void {
    this.loading = true;
    this.equityService.getStarGrants().subscribe({
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

  private showError(error: any, fallback: string): void {
    this.messageService.add({
      severity: 'error',
      summary: 'Something went wrong',
      detail: error?.error?.error ?? fallback,
      life: 8000
    });
  }

  private emptyGrant(): STARGrant {
    return {
      name: '',
      currency: 'USD',
      commencementDate: '',
      quantity: null,
      baseValue: null,
      currentValue: null,
      vestingYears: 4,
      note: '',
      applyValueToAll: false
    };
  }
}
