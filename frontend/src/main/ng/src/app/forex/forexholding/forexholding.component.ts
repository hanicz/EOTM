import { Component, OnInit, Output, EventEmitter, ChangeDetectorRef } from '@angular/core';
import { Globals } from '../../util/global';
import { ForexTransaction } from 'src/app/model/forextransaction';
import { ForexService } from 'src/app/service/forex.service';
import { Bind } from 'primeng/bind';
import { TableModule } from 'primeng/table';
import { PrimeTemplate } from 'primeng/api';
import { Skeleton } from 'primeng/skeleton';
import { Tag } from 'primeng/tag';
import { DecimalPipe, CurrencyPipe } from '@angular/common';

@Component({
    selector: 'app-forexholding',
    templateUrl: './forexholding.component.html',
    styleUrls: ['./forexholding.component.css'],
    imports: [Bind, TableModule, PrimeTemplate, Skeleton, Tag, DecimalPipe, CurrencyPipe]
})
export class ForexholdingComponent {

  forexTransactions: ForexTransaction[] = [];
  @Output() dataLoaded = new EventEmitter<ForexTransaction[]>();
  globals: Globals;

  transactionsLoading: boolean = true;

  constructor(private forexService: ForexService, globals: Globals, private cdr: ChangeDetectorRef) {
    this.globals = globals;

    this.fetchData();
    globals.stockCurrencyChange.subscribe(value => {
      this.fetchData();
    });
  }

  ngOnInit(): void {
  }

  private fetchData(): void {
    this.forexService.getHolding().subscribe({
      next: (data) => {
        this.transactionsLoading = false;
        this.forexTransactions = data;
        this.dataLoaded.emit(this.forexTransactions);
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.log(error);
      }
    });
  }

}
