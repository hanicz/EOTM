import { Component, OnInit, Output, EventEmitter } from '@angular/core';
import { Globals } from '../../util/global';
import { Investment } from '../../model/investment';
import { StockService } from '../../service/stock.service';

@Component({
  selector: 'app-holding',
  templateUrl: './holding.component.html',
  styleUrls: ['./holding.component.css']
})
export class HoldingComponent implements OnInit {

  investments: Investment[] = [];
  @Output() dataLoaded = new EventEmitter<Investment[]>();
  globals: Globals;

  investmentsLoading: boolean = true;

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
    this.stockService.getHolding(this.globals.stockCurrency).subscribe({
      next: (data) => {
        this.investmentsLoading = false;
        this.investments = data;
        this.dataLoaded.emit(this.investments);
      },
      error: (error) => {
        console.log(error);
      }
    });
  }
}
