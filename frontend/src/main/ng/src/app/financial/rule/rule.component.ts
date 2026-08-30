import { Component, ChangeDetectorRef, Input, OnChanges, SimpleChanges } from '@angular/core';
import { AccountSide, ExclusionRule, ExclusionRuleRequest } from '../../model/bankTransaction';
import { FinancialService } from '../../service/financial.service';
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
import { Dialog } from 'primeng/dialog';
import { Checkbox } from 'primeng/checkbox';
import { FormsModule } from '@angular/forms';

@Component({
    selector: 'app-financial-rule',
    templateUrl: './rule.component.html',
    styleUrls: ['./rule.component.css'],
    imports: [Bind, Toolbar, PrimeTemplate, Toast, ButtonDirective, Ripple, Tooltip, TableModule,
        InputText, Select, Dialog, Checkbox, FormsModule]
})
export class FinancialRuleComponent implements OnChanges {

  @Input() ruleRequest: ExclusionRuleRequest | null = null;

  rules: ExclusionRule[] = [];
  selectedRules: ExclusionRule[] = [];
  ruleDialog: boolean = false;
  rule: ExclusionRule = { name: '', accountNumber: '', side: 'PARTNER_ACCOUNT', active: true } as ExclusionRule;
  readonly sides: { label: string, value: AccountSide }[] = [
    { label: 'Partner account', value: 'PARTNER_ACCOUNT' },
    { label: 'My account', value: 'OWN_ACCOUNT' },
    { label: 'Either side', value: 'ANY' }
  ];
  readonly accountMaxLength = 64;
  readonly nameMaxLength = 64;

  constructor(private financialService: FinancialService, private cdr: ChangeDetectorRef,
    private messageService: MessageService) {
    this.fetchData();
  }

  ngOnChanges(changes: SimpleChanges): void {
    const request = changes['ruleRequest']?.currentValue as ExclusionRuleRequest | null;
    if (request) {
      this.openNew(request.accountNumber);
    }
  }

  private fetchData(): void {
    this.financialService.getRules().subscribe({
      next: (data) => {
        this.rules = data;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.log(error);
      }
    });
  }

  sideLabel(side: AccountSide): string {
    return this.sides.find(option => option.value === side)?.label ?? side;
  }

  openNew(accountNumber?: string): void {
    this.rule = {
      name: '',
      accountNumber: accountNumber ?? '',
      side: 'PARTNER_ACCOUNT',
      active: true
    } as ExclusionRule;
    this.ruleDialog = true;
  }

  editRule(rule: ExclusionRule): void {
    this.rule = { ...rule, name: rule.name ?? '' };
    this.ruleDialog = true;
  }

  hideDialog(): void {
    this.ruleDialog = false;
  }

  saveRule(): void {
    const accountNumber = this.rule.accountNumber.trim();
    if (!accountNumber) {
      return;
    }
    this.rule.accountNumber = accountNumber;
    this.rule.name = this.rule.name?.trim() || null;

    const call = this.rule.id === undefined
      ? this.financialService.createRule(this.rule)
      : this.financialService.updateRule(this.rule);

    call.subscribe({
      next: () => {
        this.ruleDialog = false;
        this.selectedRules = [];
        this.fetchData();
      },
      error: (error) => {
        this.messageService.add({ severity: 'error', detail: error.error?.error ?? 'Could not save the rule.' });
      }
    });
  }

  toggleActive(rule: ExclusionRule): void {
    this.financialService.updateRule({ ...rule, active: !rule.active }).subscribe({
      next: () => {
        this.fetchData();
      },
      error: (error) => {
        this.messageService.add({ severity: 'error', detail: error.error?.error ?? 'Could not update the rule.' });
      }
    });
  }

  deleteClicked(): void {
    const ids = this.selectedRules.map(r => r.id).join(',');
    this.deleteByIds(ids);
  }

  deleteByIds(ids: string): void {
    this.financialService.deleteRulesByIds(ids).subscribe({
      next: () => {
        this.selectedRules = [];
        this.fetchData();
      },
      error: (error) => {
        this.messageService.add({ severity: 'error', detail: error.error?.error ?? 'Could not delete the rule.' });
      }
    });
  }
}
