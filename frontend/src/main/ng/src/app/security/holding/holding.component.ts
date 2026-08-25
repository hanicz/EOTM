import { Component, OnInit, Output, EventEmitter, ChangeDetectorRef } from '@angular/core';
import { Globals } from '../../util/global';
import { SecurityTransaction } from '../../model/securityTransaction';
import { SecurityService } from '../../service/security.service';
import { Bind } from 'primeng/bind';
import { TableModule } from 'primeng/table';
import { PrimeTemplate } from 'primeng/api';
import { Skeleton } from 'primeng/skeleton';
import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { Divider } from 'primeng/divider';
import { Tag } from 'primeng/tag';
import { Tooltip } from 'primeng/tooltip';

const MILLISECONDS_PER_DAY = 86400000;
const SOON_IN_DAYS = 30;

export interface UpcomingPayment {
  securityName: string;
  currencyId: string;
  nextPaymentDate: Date;
  nextPaymentAmount: number;
  zeroCoupon: boolean;
  daysUntil: number;
}

export interface PaymentTotal {
  currencyId: string;
  total: number;
}

@Component({
    selector: 'app-security-holding',
    templateUrl: './holding.component.html',
    styleUrls: ['./holding.component.css'],
    imports: [Bind, TableModule, PrimeTemplate, Skeleton, Divider, Tag, Tooltip, CurrencyPipe, DatePipe, DecimalPipe]
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
    if (days === 0) return 'today';
    if (days === 1) return 'tomorrow';
    return days + ' days';
  }

  isPaymentSoon(transaction: SecurityTransaction): boolean {
    return transaction.nextPaymentDate != null && this.daysUntil(transaction.nextPaymentDate) <= SOON_IN_DAYS;
  }

  private fetchData(): void {
    this.securityService.getHolding().subscribe({
      next: (data) => {
        this.transactionsLoading = false;
        this.transactions = data;
        this.buildUpcomingPayments();
        this.dataLoaded.emit(this.transactions);
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.log(error);
      }
    });
  }

  private buildUpcomingPayments(): void {
    this.upcomingPayments = this.transactions
      .filter(t => t.nextPaymentDate != null && t.nextPaymentAmount != null)
      .map(t => ({
        securityName: t.securityName,
        currencyId: t.currencyId,
        nextPaymentDate: t.nextPaymentDate!,
        nextPaymentAmount: t.nextPaymentAmount!,
        zeroCoupon: t.zeroCoupon === true,
        daysUntil: this.daysUntil(t.nextPaymentDate!)
      }))
      .sort((a, b) => a.daysUntil - b.daysUntil);

    const totals = new Map<string, number>();
    this.upcomingPayments.forEach(p => totals.set(p.currencyId, (totals.get(p.currencyId) ?? 0) + p.nextPaymentAmount));
    this.paymentTotals = Array.from(totals, ([currencyId, total]) => ({ currencyId, total }));
  }

  private daysUntil(date: Date | string): number {
    const target = typeof date === 'string'
      ? new Date(+date.slice(0, 4), +date.slice(5, 7) - 1, +date.slice(8, 10))
      : new Date(date.getFullYear(), date.getMonth(), date.getDate());
    const now = new Date();
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    return Math.round((target.getTime() - today.getTime()) / MILLISECONDS_PER_DAY);
  }
}
