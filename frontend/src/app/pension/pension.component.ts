import { ChangeDetectorRef, Component } from '@angular/core';
import { MessageService } from 'primeng/api';
import { Pension } from '../model/pension';
import { PensionService } from '../service/pension.service';
import { Globals } from '../util/global';
import { MenuComponent } from '../menu/menu.component';
import { Bind } from 'primeng/bind';
import { Panel } from 'primeng/panel';
import { ButtonDirective } from 'primeng/button';
import { Ripple } from 'primeng/ripple';
import { InputNumber } from 'primeng/inputnumber';
import { Select } from 'primeng/select';
import { Toast } from 'primeng/toast';
import { Skeleton } from 'primeng/skeleton';
import { FormsModule } from '@angular/forms';
import { CurrencyPipe, DecimalPipe } from '@angular/common';

@Component({
    selector: 'app-pension',
    templateUrl: './pension.component.html',
    styleUrls: ['./pension.component.css'],
    imports: [MenuComponent, Bind, Panel, ButtonDirective, Ripple, InputNumber, Select, Toast, Skeleton, FormsModule, CurrencyPipe, DecimalPipe]
})
export class PensionComponent {

  totalContribution: number | null = null;
  currentValue: number | null = null;
  currency: string = 'HUF';
  currencies: any[];
  loading: boolean = true;
  saving: boolean = false;

  constructor(
    private pensionService: PensionService,
    private messageService: MessageService,
    private cdr: ChangeDetectorRef,
    globals: Globals
  ) {
    this.currencies = globals.currencies;
    this.load();
  }

  get gain(): number {
    return (this.currentValue ?? 0) - (this.totalContribution ?? 0);
  }

  get gainPct(): number {
    const contributed = this.totalContribution ?? 0;
    return contributed === 0 ? 0 : (this.gain / contributed) * 100;
  }

  save(): void {
    this.saving = true;
    const pension: Pension = {
      totalContribution: this.totalContribution ?? 0,
      currentValue: this.currentValue ?? 0,
      currency: this.currency
    };
    this.pensionService.update(pension).subscribe({
      next: (data) => {
        this.apply(data);
        this.saving = false;
        this.messageService.add({
          severity: 'success',
          summary: 'Saved',
          detail: 'Your pension fund has been updated.',
          life: 4000
        });
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.saving = false;
        this.showError(error, 'Could not save your pension fund');
        this.cdr.markForCheck();
      }
    });
  }

  private load(): void {
    this.pensionService.getPension().subscribe({
      next: (data) => {
        this.apply(data);
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.loading = false;
        this.showError(error, 'Could not load your pension fund');
        this.cdr.markForCheck();
      }
    });
  }

  private apply(pension: Pension): void {
    this.totalContribution = pension.totalContribution ?? 0;
    this.currentValue = pension.currentValue ?? 0;
    this.currency = pension.currency ?? this.currency;
  }

  private showError(error: any, summary: string): void {
    this.messageService.add({
      severity: 'error',
      summary: summary,
      detail: error?.error?.error ?? 'Something went wrong, please try again.',
      life: 8000
    });
  }
}
