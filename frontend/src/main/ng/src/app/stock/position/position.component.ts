import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { Investment } from '../../model/investment';
import { StockService } from '../../service/stock.service';
import { Globals } from '../../util/global';
import { Bind } from 'primeng/bind';
import { TableModule } from 'primeng/table';
import { PrimeTemplate } from 'primeng/api';
import { Image } from 'primeng/image';
import { Tag } from 'primeng/tag';
import { InputText } from 'primeng/inputtext';
import { DecimalPipe, CurrencyPipe } from '@angular/common';

@Component({
    selector: 'app-position',
    templateUrl: './position.component.html',
    styleUrls: ['./position.component.css'],
    imports: [Bind, TableModule, PrimeTemplate, Image, Tag, InputText, DecimalPipe, CurrencyPipe]
})
export class PositionComponent implements OnInit {

  investments: Investment[] = [];
  myMath = Math;
  globals: Globals;

  constructor(private stockService: StockService, globals: Globals, private cdr: ChangeDetectorRef) {
    this.globals = globals;

    this.fetchData();
    globals.stockCurrencyChange.subscribe(value => {
      this.fetchData();
    });
  }

  ngOnInit(): void {
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
