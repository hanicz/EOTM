import { Component, ChangeDetectorRef, EventEmitter, Output } from '@angular/core';
import { NgClass } from '@angular/common';
import { CATEGORY_CHIP_CLASS, CATEGORY_CHIP_NONE, CATEGORY_COLORS, CategoryColor, SpendingCategory } from '../../model/bankTransaction';
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
import { FormsModule } from '@angular/forms';

@Component({
    selector: 'app-financial-category',
    templateUrl: './category.component.html',
    styleUrls: ['./category.component.css'],
    imports: [NgClass, Bind, Toolbar, PrimeTemplate, Toast, ButtonDirective, Ripple, Tooltip, TableModule,
        InputText, Select, Dialog, FormsModule]
})
export class FinancialCategoryComponent {

  @Output() categoriesChanged = new EventEmitter<void>();

  categories: SpendingCategory[] = [];
  selectedCategories: SpendingCategory[] = [];
  categoryDialog: boolean = false;
  category: SpendingCategory = { name: '', color: 'BLUE' } as SpendingCategory;
  loaded: boolean = false;
  readonly colors = CATEGORY_COLORS;
  readonly nameMaxLength = 64;

  constructor(private financialService: FinancialService, private cdr: ChangeDetectorRef,
    private messageService: MessageService) {
    this.fetchData();
  }

  refresh(): void {
    this.fetchData();
  }

  private fetchData(): void {
    this.financialService.getCategories().subscribe({
      next: (data) => {
        this.categories = data;
        this.loaded = true;
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.loaded = true;
        console.log(error);
        this.cdr.markForCheck();
      }
    });
  }

  chipClass(color: CategoryColor | null): string {
    return color ? CATEGORY_CHIP_CLASS[color] : CATEGORY_CHIP_NONE;
  }

  colorLabel(color: CategoryColor): string {
    return this.colors.find(option => option.value === color)?.label ?? color;
  }

  openNew(): void {
    this.category = { name: '', color: 'BLUE' } as SpendingCategory;
    this.categoryDialog = true;
  }

  editCategory(category: SpendingCategory): void {
    this.category = { ...category };
    this.categoryDialog = true;
  }

  hideDialog(): void {
    this.categoryDialog = false;
  }

  saveCategory(): void {
    const name = this.category.name.trim();
    if (!name) {
      return;
    }
    this.category.name = name;

    const call = this.category.id === undefined
      ? this.financialService.createCategory(this.category)
      : this.financialService.updateCategory(this.category);

    call.subscribe({
      next: () => {
        this.categoryDialog = false;
        this.selectedCategories = [];
        this.fetchData();
        this.categoriesChanged.emit();
      },
      error: (error) => {
        this.messageService.add({ severity: 'error', detail: error.error?.error ?? 'Could not save the category.' });
      }
    });
  }

  addStarterSet(): void {
    this.financialService.createStarterCategories().subscribe({
      next: (data) => {
        this.messageService.add({ severity: 'success', detail: `Added ${data.length} categories and their rules.` });
        this.fetchData();
        this.categoriesChanged.emit();
      },
      error: (error) => {
        this.messageService.add({ severity: 'error', detail: error.error?.error ?? 'Could not add the starter set.' });
      }
    });
  }

  deleteClicked(): void {
    const ids = this.selectedCategories.map(c => c.id).join(',');
    this.deleteByIds(ids);
  }

  deleteByIds(ids: string): void {
    this.financialService.deleteCategoriesByIds(ids).subscribe({
      next: () => {
        this.selectedCategories = [];
        this.fetchData();
        this.categoriesChanged.emit();
      },
      error: (error) => {
        this.messageService.add({ severity: 'error', detail: error.error?.error ?? 'Could not delete the category.' });
      }
    });
  }
}
