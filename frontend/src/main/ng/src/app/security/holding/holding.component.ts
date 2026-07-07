import { Component, OnInit, Output, EventEmitter, ChangeDetectorRef } from '@angular/core';
import { Globals } from '../../util/global';
import { SecurityTransaction } from '../../model/securityTransaction';
import { SecurityService } from '../../service/security.service';
import { Bind } from 'primeng/bind';
import { TableModule } from 'primeng/table';
import { PrimeTemplate } from 'primeng/api';
import { Skeleton } from 'primeng/skeleton';
import { CurrencyPipe, DecimalPipe } from '@angular/common';

@Component({
    selector: 'app-security-holding',
    templateUrl: './holding.component.html',
    styleUrls: ['./holding.component.css'],
    imports: [Bind, TableModule, PrimeTemplate, Skeleton, CurrencyPipe, DecimalPipe]
})
export class HoldingComponent implements OnInit {

  transactions: SecurityTransaction[] = [];
  @Output() dataLoaded = new EventEmitter<SecurityTransaction[]>();
  globals: Globals;

  transactionsLoading: boolean = true;

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

  private fetchData(): void {
    this.securityService.getHolding().subscribe({
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
