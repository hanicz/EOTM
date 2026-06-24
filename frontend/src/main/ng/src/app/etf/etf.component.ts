import { Component, OnInit } from '@angular/core';
import { Globals } from '../util/global';
import { ETFInvestment } from '../model/etfinvestment';
import { MenuComponent } from '../menu/menu.component';
import { Bind } from 'primeng/bind';
import { Panel } from 'primeng/panel';
import { Tag } from 'primeng/tag';
import { Divider } from 'primeng/divider';
import { Tabs, TabList, Tab, TabPanels, TabPanel } from 'primeng/tabs';
import { Ripple } from 'primeng/ripple';
import { EtfholdingComponent } from './etfholding/etfholding.component';
import { EtfpositionComponent } from './etfposition/etfposition.component';
import { EtfinvestmentComponent } from './etfinvestment/etfinvestment.component';
import { EtfdividendComponent } from './etfdividend/etfdividend.component';
import { DecimalPipe, CurrencyPipe } from '@angular/common';

@Component({
    selector: 'app-etf',
    templateUrl: './etf.component.html',
    styleUrls: ['./etf.component.css'],
    imports: [MenuComponent, Bind, Panel, Tag, Divider, Tabs, TabList, Ripple, Tab, TabPanels, TabPanel, EtfholdingComponent, EtfpositionComponent, EtfinvestmentComponent, EtfdividendComponent, DecimalPipe, CurrencyPipe]
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
