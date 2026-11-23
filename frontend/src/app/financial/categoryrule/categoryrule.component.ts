import { Component, ChangeDetectorRef, EventEmitter, Output } from '@angular/core';
import { NgClass } from '@angular/common';
import { CATEGORY_CHIP_CLASS, CATEGORY_CHIP_NONE, CategoryColor, CategoryRule, SpendingCategory } from '../../model/bankTransaction';
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
import { InputNumber } from 'primeng/inputnumber';
import { Select } from 'primeng/select';
import { Dialog } from 'primeng/dialog';
import { Checkbox } from 'primeng/checkbox';
import { FormsModule } from '@angular/forms';

@Component({
    selector: 'app-financial-category-rule',
    templateUrl: './categoryrule.component.html',
    imports: [NgClass, Bind, Toolbar, PrimeTemplate, Toast, ButtonDirective, Ripple, Tooltip, TableModule,
        InputText, InputNumber, Select, Dialog, Checkbox, FormsModule]
})
export class FinancialCategoryRuleComponent {

  @Output() rulesApplied = new EventEmitter<void>();

  rules: CategoryRule[] = [];
  categories: SpendingCategory[] = [];
  selectedRules: CategoryRule[] = [];
  ruleDialog: boolean = false;
  rule: CategoryRule = { name: '', pattern: '', active: true } as CategoryRule;
  applying: boolean = false;
  readonly patternMaxLength = 64;
  readonly nameMaxLength = 64;

  constructor(private financialService: FinancialService, private cdr: ChangeDetectorRef,
    private messageService: MessageService) {
    this.fetchData();
  }

  refresh(): void {
    this.fetchData();
  }

  private fetchData(): void {
    this.financialService.getCategoryRules().subscribe({
      next: (data) => {
        this.rules = data;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.log(error);
      }
    });
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

  chipClass(color: CategoryColor | null): string {
    return color ? CATEGORY_CHIP_CLASS[color] : CATEGORY_CHIP_NONE;
  }

  categoryColor(categoryId: number): CategoryColor {
    return this.categories.find(category => category.id === categoryId)?.color ?? 'BLUE';
  }

  openNew(): void {
    this.rule = {
      name: '',
      pattern: '',
      categoryId: this.categories[0]?.id,
      active: true
    } as CategoryRule;
    this.ruleDialog = true;
  }

  editRule(rule: CategoryRule): void {
    this.rule = { ...rule, name: rule.name ?? '' };
    this.ruleDialog = true;
  }

  hideDialog(): void {
    this.ruleDialog = false;
  }

  saveRule(): void {
    const pattern = this.rule.pattern.trim();
    if (!pattern || this.rule.categoryId === undefined) {
      return;
    }
    this.rule.pattern = pattern;
    this.rule.name = this.rule.name?.trim() || null;

    const call = this.rule.id === undefined
      ? this.financialService.createCategoryRule(this.rule)
      : this.financialService.updateCategoryRule(this.rule);

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

  toggleActive(rule: CategoryRule): void {
    this.financialService.updateCategoryRule({ ...rule, active: !rule.active }).subscribe({
      next: () => {
        this.fetchData();
      },
      error: (error) => {
        this.messageService.add({ severity: 'error', detail: error.error?.error ?? 'Could not update the rule.' });
      }
    });
  }

  applyRules(): void {
    this.applying = true;
    this.financialService.applyCategoryRules().subscribe({
      next: (result) => {
        this.applying = false;
        this.messageService.add({
          severity: 'success',
          detail: `Categorized ${result.categorized}, cleared ${result.cleared}.`
        });
        this.rulesApplied.emit();
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.applying = false;
        this.messageService.add({ severity: 'error', detail: error.error?.error ?? 'Could not apply the rules.' });
        this.cdr.markForCheck();
      }
    });
  }

  deleteClicked(): void {
    const ids = this.selectedRules.map(r => r.id).join(',');
    this.deleteByIds(ids);
  }

  deleteByIds(ids: string): void {
    this.financialService.deleteCategoryRulesByIds(ids).subscribe({
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
