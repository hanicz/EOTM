import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { Input } from '@angular/core';
import { EMPTY_FILTER, PortfolioFilter, filterRows } from '../../util/tablefilter';
import { Investment } from '../../model/investment';
import { StockService } from '../../service/stock.service';
import { Globals } from '../../util/global';
import { Bind } from 'primeng/bind';
import { TableModule } from 'primeng/table';
import { PrimeTemplate } from 'primeng/api';
import { TickerIdentityComponent } from '../../util/ticker-identity.component';
import { DeltaComponent } from '../../util/delta.component';
import { FormsModule } from '@angular/forms';
import { DecimalPipe, CurrencyPipe } from '@angular/common';
import { WithProfit, withProfit } from '../../util/positionprofit';

@Component({
    selector: 'app-position',
    templateUrl: './position.component.html',
    imports: [Bind, TableModule, PrimeTemplate, FormsModule, DecimalPipe, CurrencyPipe, TickerIdentityComponent, DeltaComponent]
})
export class PositionComponent implements OnInit {
  visibleInvestments: WithProfit<Investment>[] = [];
  private activeFilter: PortfolioFilter = EMPTY_FILTER;

  @Input() set filter(value: PortfolioFilter) {
    this.activeFilter = value ?? EMPTY_FILTER;
    this.applyFilter();
  }

  private applyFilter(): void {
    this.visibleInvestments = filterRows(this.investments, this.activeFilter, ['shortName', 'name'], 'accountName');
  }


  investments: WithProfit<Investment>[] = [];
  globals: Globals;

  constructor(private stockService: StockService, globals: Globals, private cdr: ChangeDetectorRef) {
    this.globals = globals;

    this.fetchData();
  }

  ngOnInit(): void {
  }

  refresh(): void {
    this.fetchData();
  }

  private fetchData(): void {
    this.stockService.getPositions().subscribe({
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
