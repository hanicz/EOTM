import { Component, OnInit, Output, EventEmitter, ChangeDetectorRef } from '@angular/core';
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

  transactionsLoading: boolean = true;

  constructor(private forexService: ForexService, private cdr: ChangeDetectorRef) {
    this.fetchData();
  }

  ngOnInit(): void {
  }

  refresh(): void {
    this.transactionsLoading = true;
    this.fetchData();
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
