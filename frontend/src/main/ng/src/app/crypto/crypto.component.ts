import { Component, OnInit } from '@angular/core';
import { Transaction } from '../model/transaction';
import { MenuComponent } from '../menu/menu.component';
import { Bind } from 'primeng/bind';
import { Panel } from 'primeng/panel';
import { Tag } from 'primeng/tag';
import { Divider } from 'primeng/divider';
import { Tabs, TabList, Tab, TabPanels, TabPanel } from 'primeng/tabs';
import { Ripple } from 'primeng/ripple';
import { CryptoholdingComponent } from './cryptoholding/cryptoholding.component';
import { CryptopositionComponent } from './cryptoposition/cryptoposition.component';
import { TransactionComponent } from './transaction/transaction.component';
import { DecimalPipe, CurrencyPipe } from '@angular/common';
import { ChartComponent, ApexChart, ApexNonAxisChartSeries, ApexLegend } from 'ng-apexcharts';

export type AllocationChartOptions = {
  series: ApexNonAxisChartSeries;
  chart: ApexChart;
  labels: string[];
  legend: ApexLegend;
};

@Component({
    selector: 'app-crypto',
    templateUrl: './crypto.component.html',
    styleUrls: ['./crypto.component.css'],
    imports: [MenuComponent, Bind, Panel, Tag, Divider, Tabs, TabList, Ripple, Tab, TabPanels, TabPanel, CryptoholdingComponent, CryptopositionComponent, TransactionComponent, DecimalPipe, CurrencyPipe, ChartComponent]
})
export class CryptoComponent implements OnInit {

  transactions: Transaction[] = [];
  totalSpent: number = 0;
  totalWorth: number = 0;
  diffy: number = 0;
  percentage: number = 0;

  allocationChartOptions: Partial<AllocationChartOptions> = {
    series: [],
    chart: {
      type: 'pie',
      width: 380
    },
    labels: [],
    legend: {
      position: 'bottom'
    }
  };

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

    this.allocationChartOptions.series = this.transactions.map(t => t.liveValue ?? t.amount);
    this.allocationChartOptions.labels = this.transactions.map(t => t.symbol);
  }
}
