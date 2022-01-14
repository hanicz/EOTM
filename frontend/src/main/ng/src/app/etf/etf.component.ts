import { Component, OnInit } from '@angular/core';
import { Globals } from '../util/global';
import { ETFInvestment } from '../model/etfinvestment';

@Component({
  selector: 'app-etf',
  templateUrl: './etf.component.html',
  styleUrls: ['./etf.component.css']
})
export class EtfComponent implements OnInit {

  investments: ETFInvestment[] = [];
  totalSpent: number = 0;
  totalWorth: number = 0;
  diffy: number = 0;
  percentage: number = 0;
  globals: Globals;

  constructor(globals: Globals) {
    this.globals = globals;
  }

  ngOnInit(): void {
  }

  loadData(investments: ETFInvestment[]) {
    this.investments = investments;
    this.totalSpent = 0;
    this.totalWorth = 0;
    this.diffy = 0;
    this.percentage = 0;

    this.investments.forEach(i => {
      this.totalSpent += i.amount;
      if (i.liveValue != undefined) {
        this.totalWorth += i.liveValue;
      } else {
        this.totalWorth += i.amount;
      }
    });
    this.diffy = this.totalWorth - this.totalSpent;
    this.percentage = this.diffy / this.totalSpent * 100;
  }
}
