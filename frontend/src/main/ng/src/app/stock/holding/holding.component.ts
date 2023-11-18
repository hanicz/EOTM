import { Component, OnInit, Output, EventEmitter } from '@angular/core';
import { Globals } from '../../util/global';
import { Investment } from '../../model/investment';
import { StockService } from '../../service/stock.service';
import { AlertService } from 'src/app/service/alert.service';

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

  constructor(private stockService: StockService, globals: Globals, private alertService: AlertService) {
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

  alertClicked(investment: Investment, modulo: number){
    let average = investment.amount / investment.quantity;
    let data = {shortName: investment.shortName, exchange: investment.exchange, type: 'PRICE_OVER', valuePoint: (average + average * modulo).toFixed(2), name: investment.name}

    this.alertService.createNewStockAlert(data).subscribe({
      next: (data) => {
        
      }
    });
  }
}
