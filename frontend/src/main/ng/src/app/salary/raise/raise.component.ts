import { ChangeDetectorRef, Component } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { MessageService, PrimeTemplate } from 'primeng/api';
import { Bind } from 'primeng/bind';
import { Toast } from 'primeng/toast';
import { TableModule } from 'primeng/table';
import { SalaryRaise, SalaryRaiseScenario } from '../../model/salary';
import { SalaryService } from '../../service/salary.service';
import { DashboardService } from '../../service/dashboard.service';

@Component({
    selector: 'app-salary-raise',
    templateUrl: './raise.component.html',
    styleUrls: ['./raise.component.css'],
    imports: [Bind, PrimeTemplate, Toast, TableModule, CurrencyPipe, DatePipe]
})
export class SalaryRaiseComponent {

  raise: SalaryRaise | null = null;
  loaded: boolean = false;
  grossAnnualUsd: number | null = null;
  grossAnnualEur: number | null = null;

  private readonly BASE_CURRENCY = 'EUR';
  private rates: { [currency: string]: number } = { EUR: 1 };

  constructor(private salaryService: SalaryService, private dashboardService: DashboardService,
    private cdr: ChangeDetectorRef, private messageService: MessageService) {
    this.fetchData();
  }

  private fetchData(): void {
    this.salaryService.getRaise().subscribe({
      next: (data) => {
        this.raise = data ?? null;
        this.loaded = true;
        this.cdr.markForCheck();
        this.loadRatesAndConvert();
      },
      error: (error) => {
        this.loaded = true;
        this.messageService.add({ severity: 'error', detail: error.error?.error ?? 'Could not load the salary.' });
        this.cdr.markForCheck();
      }
    });
  }

  grossMonthlyDiff(scenario: SalaryRaiseScenario): number {
    return scenario.grossMonthly - (this.raise?.current.grossMonthly ?? 0);
  }

  netMonthlyDiff(scenario: SalaryRaiseScenario): number {
    return scenario.netMonthly - (this.raise?.current.netMonthly ?? 0);
  }

  private loadRatesAndConvert(): void {
    if (!this.raise) {
      return;
    }
    const needed = ['USD', this.raise.current.currencyId]
      .filter(currency => currency !== this.BASE_CURRENCY && !(currency in this.rates));

    if (needed.length === 0) {
      this.convertGrossAnnual();
      return;
    }

    this.dashboardService.getRates(needed).subscribe({
      next: (response) => {
        this.rates = { ...this.rates, ...response.rates };
        this.convertGrossAnnual();
      },
      error: (error) => {
        console.error('Error loading exchange rates:', error);
        this.convertGrossAnnual();
      }
    });
  }

  private convertGrossAnnual(): void {
    const grossAnnual = this.raise?.current.grossAnnual;
    const from = this.raise?.current.currencyId;

    this.grossAnnualUsd = this.convert(grossAnnual, from, 'USD');
    this.grossAnnualEur = this.convert(grossAnnual, from, this.BASE_CURRENCY);
    this.cdr.markForCheck();
  }

  private convert(amount: number | undefined, from: string | undefined, to: string): number | null {
    if (amount == null || !from) {
      return null;
    }
    const fromRate = this.rateFor(from);
    const toRate = this.rateFor(to);
    if (!fromRate || !toRate) {
      return null;
    }
    return (amount / fromRate) * toRate;
  }

  private rateFor(currency: string): number {
    return currency === this.BASE_CURRENCY ? 1 : this.rates[currency];
  }
}
