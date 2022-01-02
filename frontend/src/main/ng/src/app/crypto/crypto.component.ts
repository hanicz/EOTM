import { Component, OnInit } from '@angular/core';
import { Transaction } from '../model/transaction';
import { Globals } from '../util/global';

@Component({
  selector: 'app-crypto',
  templateUrl: './crypto.component.html',
  styleUrls: ['./crypto.component.css']
})
export class CryptoComponent implements OnInit {

  transactions: Transaction[] = [];
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

  loadData(transactions: Transaction[]) {
    this.transactions = transactions;
    this.totalSpent = 0;
    this.totalWorth = 0;
    this.diffy = 0;
    this.percentage = 0;

    this.transactions.forEach(t => {
      this.totalSpent += t.amount;
      if (t.liveValue != undefined) {
        this.totalWorth += t.liveValue;
      } else {
        this.totalWorth += t.amount;
      }
    });
    this.diffy = this.totalWorth - this.totalSpent;
    this.percentage = this.diffy / this.totalSpent * 100;
  }
}
