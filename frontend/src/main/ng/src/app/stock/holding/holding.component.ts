import { Component, OnInit, Output, EventEmitter, ChangeDetectorRef } from '@angular/core';
import { Globals } from '../../util/global';
import { Investment } from '../../model/investment';
import { StockService } from '../../service/stock.service';
import { AlertService } from 'src/app/service/alert.service';
import { Bind } from 'primeng/bind';
import { TableModule } from 'primeng/table';
import { PrimeTemplate } from 'primeng/api';
import { Skeleton } from 'primeng/skeleton';
import { Image } from 'primeng/image';
import { Tag } from 'primeng/tag';
import { DecimalPipe, CurrencyPipe } from '@angular/common';

@Component({
    selector: 'app-holding',
    templateUrl: './holding.component.html',
    styleUrls: ['./holding.component.css'],
    imports: [Bind, TableModule, PrimeTemplate, Skeleton, Image, Tag, DecimalPipe, CurrencyPipe]
})
export class HoldingComponent implements OnInit {

  investments: Investment[] = [];
  @Output() dataLoaded = new EventEmitter<Investment[]>();
  globals: Globals;

  investmentsLoading: boolean = true;

  constructor(private stockService: StockService, globals: Globals, private alertService: AlertService, private cdr: ChangeDetectorRef) {
    this.globals = globals;

    this.fetchData();
  }

  ngOnInit(): void {
  }

  private fetchData(): void {
    this.stockService.getHolding().subscribe({
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

  alertClicked(investment: Investment, modulo: number){
    let average = investment.amount / investment.quantity;
    let data = {shortName: investment.shortName, exchange: investment.exchange, type: 'PRICE_OVER', valuePoint: (average + average * modulo).toFixed(2), name: investment.name}

    this.alertService.createNewStockAlert(data).subscribe({
      next: (data) => {
      }
    });
  }
}
