import { Component, OnInit } from '@angular/core';
import { Globals } from '../util/global';
import { ForexTransaction } from '../model/forextransaction';

@Component({
  selector: 'app-forex',
  templateUrl: './forex.component.html',
  styleUrls: ['./forex.component.css']
})
export class ForexComponent implements OnInit {

  forexTransactions: ForexTransaction[] = [];
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

  loadData(forexTransactions: ForexTransaction[]) {
    this.forexTransactions = forexTransactions;
    this.totalSpent = 0;
    this.totalWorth = 0;
    this.diffy = 0;
    this.percentage = 0;

    this.forexTransactions.forEach(i => {
      this.totalSpent += i.fromAmount;
      if (i.liveValue != undefined) {
        this.totalWorth += i.liveValue;
      }
    });
    this.diffy = this.totalWorth - this.totalSpent;
    this.percentage = this.diffy / this.totalSpent * 100;
  }

}
