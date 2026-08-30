import { Component, OnInit, Output, EventEmitter, ChangeDetectorRef  } from '@angular/core';
import { Transaction } from '../../model/transaction';
import { CryptoService } from '../../service/crypto.service';
import { environment } from '../../../environments/environment';
import { Bind } from 'primeng/bind';
import { TableModule } from 'primeng/table';
import { PrimeTemplate } from 'primeng/api';
import { Skeleton } from 'primeng/skeleton';
import { Image } from 'primeng/image';
import { ButtonDirective } from 'primeng/button';
import { Ripple } from 'primeng/ripple';
import { Tooltip } from 'primeng/tooltip';
import { AlertService } from '../../service/alert.service';
import { DecimalPipe, CurrencyPipe, NgClass } from '@angular/common';

@Component({
    selector: 'app-cryptoholding',
    templateUrl: './cryptoholding.component.html',
    styleUrls: ['./cryptoholding.component.css'],
    imports: [Bind, TableModule, PrimeTemplate, Skeleton, Image, ButtonDirective, Ripple, Tooltip,
        DecimalPipe, CurrencyPipe, NgClass]
})
export class CryptoholdingComponent implements OnInit {

  transactions: Transaction[] = [];
  @Output() dataLoaded = new EventEmitter<Transaction[]>();
  assetUrl: string;
  transactionsLoading: boolean = true;

  readonly alertSteps = [0.05, 0.1];

  private readonly pendingAlerts = new Set<string>();
  private readonly createdAlerts = new Set<string>();

  constructor(private cryptoService: CryptoService, private alertService: AlertService,
    private cdr: ChangeDetectorRef) {
    this.assetUrl = environment.assets_url;
    this.fetchData();
  }

  ngOnInit(): void {
  }

  refresh(): void {
    this.transactionsLoading = true;
    this.fetchData(true);
  }

  markForCheck(): void {
    this.cdr.markForCheck();
  }

  alertTarget(transaction: Transaction, modulo: number): number {
    const average = transaction.amount / transaction.quantity;
    return average + average * modulo;
  }

  alertPending(transaction: Transaction, modulo: number): boolean {
    return this.pendingAlerts.has(this.alertKey(transaction, modulo));
  }

  alertCreated(transaction: Transaction, modulo: number): boolean {
    return this.createdAlerts.has(this.alertKey(transaction, modulo));
  }

  alertClicked(transaction: Transaction, modulo: number) {
    const key = this.alertKey(transaction, modulo);
    if (this.pendingAlerts.has(key) || this.createdAlerts.has(key)) {
      return;
    }
    this.pendingAlerts.add(key);

    let data = {symbol: transaction.symbol, type: 'PRICE_OVER', valuePoint: this.alertTarget(transaction, modulo).toFixed(2)}

    this.alertService.createNewCryptoAlert(data).subscribe({
      next: () => {
        this.pendingAlerts.delete(key);
        this.createdAlerts.add(key);
        this.cdr.markForCheck();
      },
      error: () => {
        this.pendingAlerts.delete(key);
        this.cdr.markForCheck();
      }
    });
  }

  private alertKey(transaction: Transaction, modulo: number): string {
    return transaction.symbol + '@' + modulo;
  }

  private fetchData(forceRefresh = false): void {
    this.cryptoService.getHoldings(forceRefresh).subscribe({
      next: (data) => {
        this.transactionsLoading = false;
        this.transactions = data;
        this.dataLoaded.emit(this.transactions);
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.log(error);
      }
    });
  }
}
