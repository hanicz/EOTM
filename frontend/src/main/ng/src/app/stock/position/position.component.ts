import { Component, OnInit } from '@angular/core';
import { Investment } from '../../model/investment';
import { StockService } from '../../service/stock.service';
import { Globals } from '../../util/global';

@Component({
  selector: 'app-position',
  templateUrl: './position.component.html',
  styleUrls: ['./position.component.css']
})
export class PositionComponent implements OnInit {

  investments: Investment[] = [];
  myMath = Math;
  globals: Globals;

  constructor(private stockService: StockService, globals: Globals) {
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
      },
      error: (error) => {
        console.log(error);
      }
    });
  }
}
