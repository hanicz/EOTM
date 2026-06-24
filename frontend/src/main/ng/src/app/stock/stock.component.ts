import { Component, OnInit } from '@angular/core';
import { Globals } from '../util/global';
import { Investment } from '../model/investment';
import { MenuComponent } from '../menu/menu.component';
import { Bind } from 'primeng/bind';
import { Panel } from 'primeng/panel';
import { Tag } from 'primeng/tag';
import { Divider } from 'primeng/divider';
import { Tabs, TabList, Tab, TabPanels, TabPanel } from 'primeng/tabs';
import { Ripple } from 'primeng/ripple';
import { HoldingComponent } from './holding/holding.component';
import { PositionComponent } from './position/position.component';
import { InvestmentComponent } from './investment/investment.component';
import { DividendComponent } from './dividend/dividend.component';
import { DecimalPipe, CurrencyPipe } from '@angular/common';

@Component({
    selector: 'app-stock',
    templateUrl: './stock.component.html',
    styleUrls: ['./stock.component.css'],
    imports: [MenuComponent, Bind, Panel, Tag, Divider, Tabs, TabList, Ripple, Tab, TabPanels, TabPanel, HoldingComponent, PositionComponent, InvestmentComponent, DividendComponent, DecimalPipe, CurrencyPipe]
})
export class StockComponent implements OnInit {

  investments: Investment[] = [];
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

  loadData(investments: Investment[]) {
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
