import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { Investment } from '../../model/investment';
import { StockService } from '../../service/stock.service';
import { Globals } from '../../util/global';
import { Bind } from 'primeng/bind';
import { TableModule } from 'primeng/table';
import { PrimeTemplate } from 'primeng/api';
import { TickerIdentityComponent } from '../../util/ticker-identity.component';
import { DeltaComponent } from '../../util/delta.component';
import { InputText } from 'primeng/inputtext';
import { DecimalPipe, CurrencyPipe } from '@angular/common';

@Component({
    selector: 'app-position',
    templateUrl: './position.component.html',
    styleUrls: ['./position.component.css'],
    imports: [Bind, TableModule, PrimeTemplate, InputText, DecimalPipe, CurrencyPipe, TickerIdentityComponent, DeltaComponent]
})
export class PositionComponent implements OnInit {

  investments: Investment[] = [];
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
        this.investments = data;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.log(error);
      }
    });
  }
}
