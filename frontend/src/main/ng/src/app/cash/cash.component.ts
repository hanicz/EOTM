import { ChangeDetectorRef, Component } from '@angular/core';
import { MessageService } from 'primeng/api';
import { Cash } from '../model/cash';
import { CashService } from '../service/cash.service';
import { MenuComponent } from '../menu/menu.component';
import { Bind } from 'primeng/bind';
import { Panel } from 'primeng/panel';
import { ButtonDirective } from 'primeng/button';
import { Ripple } from 'primeng/ripple';
import { InputNumber } from 'primeng/inputnumber';
import { Toast } from 'primeng/toast';
import { Skeleton } from 'primeng/skeleton';
import { FormsModule } from '@angular/forms';

@Component({
    selector: 'app-cash',
    templateUrl: './cash.component.html',
    styleUrls: ['./cash.component.css'],
    imports: [MenuComponent, Bind, Panel, ButtonDirective, Ripple, InputNumber, Toast, Skeleton, FormsModule]
})
export class CashComponent {

  amount: number | null = null;
  currency: string = 'HUF';
  loading: boolean = true;
  saving: boolean = false;

  constructor(
    private cashService: CashService,
    private messageService: MessageService,
    private cdr: ChangeDetectorRef
  ) {
    this.load();
  }

  save(): void {
    this.saving = true;
    const cash: Cash = { amount: this.amount ?? 0, currency: this.currency };
    this.cashService.update(cash).subscribe({
      next: (data) => {
        this.apply(data);
        this.saving = false;
        this.messageService.add({
          severity: 'success',
          summary: 'Saved',
          detail: 'Your cash balance has been updated.',
          life: 4000
        });
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.saving = false;
        this.showError(error, 'Could not save your cash balance');
        this.cdr.markForCheck();
      }
    });
  }

  private load(): void {
    this.cashService.getCash().subscribe({
      next: (data) => {
        this.apply(data);
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.loading = false;
        this.showError(error, 'Could not load your cash balance');
        this.cdr.markForCheck();
      }
    });
  }

  private apply(cash: Cash): void {
    this.amount = cash.amount ?? 0;
    this.currency = cash.currency ?? this.currency;
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
