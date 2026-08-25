import { Component, OnInit, Output, EventEmitter, ChangeDetectorRef } from '@angular/core';
import { ETFInvestment } from 'src/app/model/etfinvestment';
import { EtfService } from 'src/app/service/etf.service';
import { Bind } from 'primeng/bind';
import { TableModule } from 'primeng/table';
import { PrimeTemplate } from 'primeng/api';
import { Skeleton } from 'primeng/skeleton';
import { Tag } from 'primeng/tag';
import { Tooltip } from 'primeng/tooltip';
import { DecimalPipe, CurrencyPipe } from '@angular/common';
import { TickerLogoComponent } from '../../util/ticker-logo.component';

@Component({
    selector: 'app-etfholding',
    templateUrl: './etfholding.component.html',
    styleUrls: ['./etfholding.component.css'],
    imports: [Bind, TableModule, PrimeTemplate, Skeleton, Tag, Tooltip, DecimalPipe, CurrencyPipe, TickerLogoComponent]
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

  refresh(): void {
    this.investmentsLoading = true;
    this.fetchData(true);
  }

  markForCheck(): void {
    this.cdr.markForCheck();
  }

  private fetchData(forceRefresh = false): void {
    this.etfService.getHolding(forceRefresh).subscribe({
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
