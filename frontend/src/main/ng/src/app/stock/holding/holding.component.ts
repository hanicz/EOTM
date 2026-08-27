import { Component, OnInit, Output, EventEmitter, ChangeDetectorRef } from '@angular/core';
import { Globals } from '../../util/global';
import { Investment } from '../../model/investment';
import { StockService } from '../../service/stock.service';
import { AlertService } from 'src/app/service/alert.service';
import { Bind } from 'primeng/bind';
import { TableModule } from 'primeng/table';
import { PrimeTemplate } from 'primeng/api';
import { Skeleton } from 'primeng/skeleton';
import { TickerIdentityComponent } from '../../util/ticker-identity.component';
import { DeltaComponent } from '../../util/delta.component';
import { ButtonDirective } from 'primeng/button';
import { Ripple } from 'primeng/ripple';
import { Tooltip } from 'primeng/tooltip';
import { CurrencyPipe } from '@angular/common';

@Component({
    selector: 'app-holding',
    templateUrl: './holding.component.html',
    styleUrls: ['./holding.component.css'],
    imports: [Bind, TableModule, PrimeTemplate, Skeleton, ButtonDirective, Ripple, Tooltip,
        CurrencyPipe, TickerIdentityComponent, DeltaComponent]
})
export class HoldingComponent implements OnInit {

  investments: Investment[] = [];
  @Output() dataLoaded = new EventEmitter<Investment[]>();
  globals: Globals;

  investmentsLoading: boolean = true;

  readonly alertSteps = [0.05, 0.1];

  readonly skeletonRows = new Array(4).fill({});

  private readonly pendingAlerts = new Set<string>();
  private readonly createdAlerts = new Set<string>();

  constructor(private stockService: StockService, globals: Globals, private alertService: AlertService, private cdr: ChangeDetectorRef) {
    this.globals = globals;

    this.fetchData();
  }

  ngOnInit(): void {
  }

  refresh(): void {
    this.investmentsLoading = true;
    this.fetchData(true);
  }

  markForCheck(): void {
    this.cdr.markForCheck();
  }

  private fetchData(forceRefresh = false): void {
    this.stockService.getHolding(forceRefresh).subscribe({
      next: (data) => {
        this.investmentsLoading = false;
        this.investments = data;
        this.dataLoaded.emit(this.investments);
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.log(error);
      }
    });
  }

  alertTarget(investment: Investment, modulo: number): number {
    const average = investment.amount / investment.quantity;
    return average + average * modulo;
  }

  alertPending(investment: Investment, modulo: number): boolean {
    return this.pendingAlerts.has(this.alertKey(investment, modulo));
  }

  alertCreated(investment: Investment, modulo: number): boolean {
    return this.createdAlerts.has(this.alertKey(investment, modulo));
  }

  alertClicked(investment: Investment, modulo: number) {
    const key = this.alertKey(investment, modulo);
    if (this.pendingAlerts.has(key) || this.createdAlerts.has(key)) {
      return;
    }
    this.pendingAlerts.add(key);

    let data = {shortName: investment.shortName, exchange: investment.exchange, type: 'PRICE_OVER', valuePoint: this.alertTarget(investment, modulo).toFixed(2), name: investment.name}

    this.alertService.createNewStockAlert(data).subscribe({
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

  private alertKey(investment: Investment, modulo: number): string {
    return investment.shortName + '.' + investment.exchange + '@' + modulo;
  }
}
