import { Component, OnInit } from '@angular/core';
import { Globals } from '../util/global';
import { ForexTransaction } from '../model/forextransaction';
import { MenuComponent } from '../menu/menu.component';
import { Bind } from 'primeng/bind';
import { Panel } from 'primeng/panel';
import { Tag } from 'primeng/tag';
import { Divider } from 'primeng/divider';
import { Tabs, TabList, Tab, TabPanels, TabPanel } from 'primeng/tabs';
import { Ripple } from 'primeng/ripple';
import { ForexholdingComponent } from './forexholding/forexholding.component';
import { ForextransactionComponent } from './forextransaction/forextransaction.component';
import { DecimalPipe, CurrencyPipe } from '@angular/common';

@Component({
    selector: 'app-forex',
    templateUrl: './forex.component.html',
    styleUrls: ['./forex.component.css'],
    imports: [MenuComponent, Bind, Panel, Tag, Divider, Tabs, TabList, Ripple, Tab, TabPanels, TabPanel, ForexholdingComponent, ForextransactionComponent, DecimalPipe, CurrencyPipe]
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
