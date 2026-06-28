import { Component, OnInit, Output, EventEmitter, ChangeDetectorRef } from '@angular/core';
import { ETFInvestment } from 'src/app/model/etfinvestment';
import { EtfService } from 'src/app/service/etf.service';
import { Bind } from 'primeng/bind';
import { TableModule } from 'primeng/table';
import { PrimeTemplate } from 'primeng/api';
import { Skeleton } from 'primeng/skeleton';
import { Tag } from 'primeng/tag';
import { DecimalPipe, CurrencyPipe } from '@angular/common';

@Component({
    selector: 'app-etfholding',
    templateUrl: './etfholding.component.html',
    styleUrls: ['./etfholding.component.css'],
    imports: [Bind, TableModule, PrimeTemplate, Skeleton, Tag, DecimalPipe, CurrencyPipe]
})
export class EtfholdingComponent implements OnInit {

  investments: ETFInvestment[] = [];
  @Output() dataLoaded = new EventEmitter<ETFInvestment[]>();

  investmentsLoading: boolean = true;

  constructor(private etfService: EtfService, private cdr: ChangeDetectorRef) {
    this.fetchData();
  }

  ngOnInit(): void {
  }

  private fetchData(): void {
    this.etfService.getHolding().subscribe({
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
}
