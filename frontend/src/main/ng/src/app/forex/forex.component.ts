import { Component, OnInit, ViewChild } from '@angular/core';
import { ForexTransaction } from '../model/forextransaction';
import { MenuComponent } from '../menu/menu.component';
import { Bind } from 'primeng/bind';
import { Panel } from 'primeng/panel';
import { Tag } from 'primeng/tag';
import { Divider } from 'primeng/divider';
import { Tabs, TabList, Tab, TabPanels, TabPanel } from 'primeng/tabs';
import { Ripple } from 'primeng/ripple';
import { ButtonDirective } from 'primeng/button';
import { Tooltip } from 'primeng/tooltip';
import { ForexholdingComponent } from './forexholding/forexholding.component';
import { ForextransactionComponent } from './forextransaction/forextransaction.component';
import { DecimalPipe, CurrencyPipe } from '@angular/common';
import { ChartComponent, ApexChart, ApexNonAxisChartSeries, ApexLegend } from 'ng-apexcharts';

export type AllocationChartOptions = {
  series: ApexNonAxisChartSeries;
  chart: ApexChart;
  labels: string[];
  legend: ApexLegend;
};

@Component({
    selector: 'app-forex',
    templateUrl: './forex.component.html',
    styleUrls: ['./forex.component.css'],
    imports: [MenuComponent, Bind, Panel, Tag, Divider, Tabs, TabList, Ripple, Tab, TabPanels, TabPanel, ButtonDirective, Tooltip, ForexholdingComponent, ForextransactionComponent, DecimalPipe, CurrencyPipe, ChartComponent]
})
export class ForexComponent implements OnInit {

  @ViewChild(ForexholdingComponent) forexholding!: ForexholdingComponent;
  @ViewChild(ForextransactionComponent) forextransaction!: ForextransactionComponent;

  forexTransactions: ForexTransaction[] = [];
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

  refreshAll(): void {
    this.forexholding?.refresh();
    this.forextransaction?.refresh();
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

    this.allocationChartOptions.series = this.forexTransactions.map(i => i.liveValue ?? i.fromAmount);
    this.allocationChartOptions.labels = this.forexTransactions.map(i => `${i.fromCurrencyId}/${i.toCurrencyId}`);
  }

}
