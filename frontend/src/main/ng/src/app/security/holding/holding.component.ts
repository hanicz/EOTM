import { Component, OnInit, Output, EventEmitter, ChangeDetectorRef } from '@angular/core';
import { Globals } from '../../util/global';
import { SecurityTransaction } from '../../model/securityTransaction';
import { SecurityService } from '../../service/security.service';
import { Bind } from 'primeng/bind';
import { TableModule } from 'primeng/table';
import { PrimeTemplate } from 'primeng/api';
import { Skeleton } from 'primeng/skeleton';
import { CurrencyPipe, DatePipe, DecimalPipe, NgClass } from '@angular/common';
import { Divider } from 'primeng/divider';
import { Tooltip } from 'primeng/tooltip';
import { SOON_IN_DAYS, UpcomingPayment, buildUpcomingPayments, daysUntil, dueInLabel } from '../../util/upcomingpayments';

export interface PaymentTotal {
  currencyId: string;
  total: number;
}

@Component({
    selector: 'app-security-holding',
    templateUrl: './holding.component.html',
    styleUrls: ['./holding.component.css'],
    imports: [Bind, TableModule, PrimeTemplate, Skeleton, Divider, Tooltip, CurrencyPipe, DatePipe, DecimalPipe, NgClass]
})
export class HoldingComponent implements OnInit {

  transactions: SecurityTransaction[] = [];
  upcomingPayments: UpcomingPayment[] = [];
  paymentTotals: PaymentTotal[] = [];
  @Output() dataLoaded = new EventEmitter<SecurityTransaction[]>();
  globals: Globals;

  transactionsLoading: boolean = true;
  readonly soonInDays = SOON_IN_DAYS;

  constructor(private securityService: SecurityService, globals: Globals, private cdr: ChangeDetectorRef) {
    this.globals = globals;

    this.fetchData();
  }

  ngOnInit(): void {
  }

  refresh(): void {
    this.transactionsLoading = true;
    this.fetchData();
  }

  markForCheck(): void {
    this.cdr.markForCheck();
  }

  dueInLabel(days: number): string {
    return dueInLabel(days);
  }

  isPaymentSoon(transaction: SecurityTransaction): boolean {
    return transaction.nextPaymentDate != null && daysUntil(transaction.nextPaymentDate) <= SOON_IN_DAYS;
  }

  private fetchData(): void {
    this.securityService.getHolding().subscribe({
      next: (data) => {
        this.transactionsLoading = false;
        this.transactions = data;
        this.applyPayments();
        this.dataLoaded.emit(this.transactions);
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.log(error);
      }
    });
  }

  private applyPayments(): void {
    this.upcomingPayments = buildUpcomingPayments(this.transactions);

    const totals = new Map<string, number>();
    this.upcomingPayments.forEach(p => totals.set(p.currencyId, (totals.get(p.currencyId) ?? 0) + p.nextPaymentAmount));
    this.paymentTotals = Array.from(totals, ([currencyId, total]) => ({ currencyId, total }));
  }
}
