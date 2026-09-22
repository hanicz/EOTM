import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { Input } from '@angular/core';
import { EMPTY_FILTER, PortfolioFilter, filterRows } from '../../util/tablefilter';
import { EtfService } from 'src/app/service/etf.service';
import { ETFInvestment } from 'src/app/model/etfinvestment';
import { Bind } from 'primeng/bind';
import { TableModule } from 'primeng/table';
import { PrimeTemplate } from 'primeng/api';
import { FormsModule } from '@angular/forms';
import { DecimalPipe, CurrencyPipe } from '@angular/common';
import { WithProfit, withProfit } from '../../util/positionprofit';
import { TickerIdentityComponent } from '../../util/ticker-identity.component';
import { DeltaComponent } from '../../util/delta.component';

@Component({
    selector: 'app-etfposition',
    templateUrl: './etfposition.component.html',
    imports: [Bind, TableModule, PrimeTemplate, FormsModule, DecimalPipe, CurrencyPipe, TickerIdentityComponent, DeltaComponent]
})
export class EtfpositionComponent implements OnInit {
  visibleInvestments: WithProfit<ETFInvestment>[] = [];
  private activeFilter: PortfolioFilter = EMPTY_FILTER;

  @Input() set filter(value: PortfolioFilter) {
    this.activeFilter = value ?? EMPTY_FILTER;
    this.applyFilter();
  }

  private applyFilter(): void {
    this.visibleInvestments = filterRows(this.investments, this.activeFilter, ['shortName', 'name'], 'accountName');
  }


  investments: WithProfit<ETFInvestment>[] = [];

  constructor(private etfService: EtfService, private cdr: ChangeDetectorRef) {
    this.fetchData();
  }

  ngOnInit(): void {
  }

  refresh(): void {
    this.fetchData();
  }

  private fetchData(): void {
    this.etfService.getPositions().subscribe({
      next: (data) => {
        this.investments = withProfit(data);
        this.applyFilter();
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.log(error);
      }
    });
  }
}
