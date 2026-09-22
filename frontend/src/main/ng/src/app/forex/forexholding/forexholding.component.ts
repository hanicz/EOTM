import { Component, OnInit, Output, EventEmitter, ChangeDetectorRef } from '@angular/core';
import { Input } from '@angular/core';
import { EMPTY_FILTER, PortfolioFilter, filterRows } from '../../util/tablefilter';
import { ForexTransaction } from 'src/app/model/forextransaction';
import { ForexService } from 'src/app/service/forex.service';
import { Bind } from 'primeng/bind';
import { TableModule } from 'primeng/table';
import { PrimeTemplate } from 'primeng/api';
import { Skeleton } from 'primeng/skeleton';
import { Tooltip } from 'primeng/tooltip';
import { DecimalPipe, CurrencyPipe } from '@angular/common';

@Component({
    selector: 'app-forexholding',
    templateUrl: './forexholding.component.html',
    imports: [Bind, TableModule, PrimeTemplate, Skeleton, Tooltip, DecimalPipe, CurrencyPipe]
})
export class ForexholdingComponent {
  visibleTransactions: ForexTransaction[] = [];
  private activeFilter: PortfolioFilter = EMPTY_FILTER;

  @Input() set filter(value: PortfolioFilter) {
    this.activeFilter = value ?? EMPTY_FILTER;
    this.applyFilter();
  }

  private applyFilter(): void {
    this.visibleTransactions = filterRows(this.forexTransactions, this.activeFilter, ['fromCurrencyId', 'toCurrencyId']);
  }


  forexTransactions: ForexTransaction[] = [];
  @Output() dataLoaded = new EventEmitter<ForexTransaction[]>();

  transactionsLoading: boolean = true;

  constructor(private forexService: ForexService, private cdr: ChangeDetectorRef) {
    this.fetchData();
  }

  ngOnInit(): void {
  }

  refresh(): void {
    this.transactionsLoading = true;
    this.fetchData(true);
  }

  private fetchData(forceRefresh = false): void {
    this.forexService.getHolding(forceRefresh).subscribe({
      next: (data) => {
        this.transactionsLoading = false;
        this.forexTransactions = data;
        this.applyFilter();
        this.dataLoaded.emit(this.forexTransactions);
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.log(error);
      }
    });
  }

}
