import { Component, ChangeDetectorRef } from '@angular/core';
import { MonthlyCashFlow } from '../../../model/bankTransaction';
import { FinancialService } from '../../../service/financial.service';
import { Bind } from 'primeng/bind';
import { PrimeTemplate } from 'primeng/api';
import { Toolbar } from 'primeng/toolbar';
import { ButtonDirective } from 'primeng/button';
import { Ripple } from 'primeng/ripple';
import { Tooltip } from 'primeng/tooltip';
import { TableModule } from 'primeng/table';
import { Divider } from 'primeng/divider';
import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { ChartComponent, ApexChart, ApexAxisChartSeries, ApexXAxis, ApexYAxis, ApexLegend, ApexDataLabels, ApexTooltip, ApexStroke } from 'ng-apexcharts';

export type CashFlowChartOptions = {
  series: ApexAxisChartSeries;
  chart: ApexChart;
  xaxis: ApexXAxis;
  yaxis: ApexYAxis;
  legend: ApexLegend;
  dataLabels: ApexDataLabels;
  tooltip: ApexTooltip;
  stroke: ApexStroke;
  colors: string[];
};

export interface AverageSaving {
  label: string;
  windowMonths: number;
  monthsCounted: number;
  average: number | null;
}

export interface CurrencyCashFlow {
  currencyId: string;
  rows: MonthlyCashFlow[];
  totalIn: number;
  totalOut: number;
  averages: AverageSaving[];
  chartOptions: Partial<CashFlowChartOptions>;
}

@Component({
    selector: 'app-financial-cashflow',
    templateUrl: './cashflow.component.html',
    styleUrls: ['./cashflow.component.css'],
    imports: [Bind, PrimeTemplate, Toolbar, ButtonDirective, Ripple, Tooltip, TableModule, Divider, CurrencyPipe,
        DecimalPipe, ChartComponent]
})
export class FinancialCashFlowComponent {

  private static readonly WINDOWS = [3, 6, 12, 24];

  blocks: CurrencyCashFlow[] = [];
  loaded: boolean = false;

  constructor(private financialService: FinancialService, private cdr: ChangeDetectorRef) {
    this.fetchData();
  }

  refresh(): void {
    this.fetchData();
  }

  private fetchData(): void {
    this.financialService.getMonthlyCashFlow().subscribe({
      next: (data) => {
        this.blocks = this.groupByCurrency(data);
        this.loaded = true;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.log(error);
        this.loaded = true;
        this.cdr.markForCheck();
      }
    });
  }

  private groupByCurrency(data: MonthlyCashFlow[]): CurrencyCashFlow[] {
    const currencies = [...new Set(data.map(row => row.currencyId))].sort();

    return currencies.map(currencyId => {
      const rows = data.filter(row => row.currencyId === currencyId);
      const oldestFirst = [...rows].reverse();

      return {
        currencyId,
        rows,
        totalIn: rows.reduce((sum, row) => sum + row.moneyIn, 0),
        totalOut: rows.reduce((sum, row) => sum + row.moneyOut, 0),
        averages: this.buildAverages(rows),
        chartOptions: this.buildChart(oldestFirst)
      };
    });
  }

  /* Windows are counted back from the most recent month that has data, not from today. Anchoring to today
     would quietly pad the average with empty months whenever the export is a few weeks old. */
  private buildAverages(rows: MonthlyCashFlow[]): AverageSaving[] {
    if (rows.length === 0) {
      return [];
    }
    const anchor = this.monthIndex(rows[0]);

    return FinancialCashFlowComponent.WINDOWS.map(windowMonths => {
      const inWindow = rows.filter(row => {
        const index = this.monthIndex(row);
        return index > anchor - windowMonths && index <= anchor;
      });

      return {
        label: `Last ${windowMonths} months`,
        windowMonths,
        monthsCounted: inWindow.length,
        average: inWindow.length === 0 ? null
          : inWindow.reduce((sum, row) => sum + row.net, 0) / inWindow.length
      };
    });
  }

  private monthIndex(row: MonthlyCashFlow): number {
    return row.year * 12 + (row.month - 1);
  }

  private buildChart(rows: MonthlyCashFlow[]): Partial<CashFlowChartOptions> {
    return {
      series: [
        { name: 'In', type: 'column', data: rows.map(row => this.round(row.moneyIn)) },
        { name: 'Out', type: 'column', data: rows.map(row => this.round(row.moneyOut)) },
        { name: 'Net', type: 'line', data: rows.map(row => this.round(row.net)) }
      ],
      chart: { type: 'line', height: 360, stacked: false, toolbar: { show: false } },
      colors: ['#3f9d63', '#c1443b', '#ef9f27'],
      stroke: { width: [0, 0, 3], curve: 'straight' },
      dataLabels: { enabled: false },
      xaxis: { categories: rows.map(row => this.label(row)) },
      yaxis: { labels: { formatter: (value: number) => this.compact(value) } },
      legend: { position: 'bottom' },
      tooltip: { shared: true, intersect: false }
    };
  }

  label(row: MonthlyCashFlow): string {
    return `${row.year}-${String(row.month).padStart(2, '0')}`;
  }

  private round(value: number): number {
    return Math.round(value * 100) / 100;
  }

  private compact(value: number): string {
    const absolute = Math.abs(value);
    if (absolute >= 1_000_000) {
      return `${(value / 1_000_000).toFixed(1)}M`;
    }
    if (absolute >= 1_000) {
      return `${(value / 1_000).toFixed(0)}k`;
    }
    return `${value}`;
  }

  download(): void {
    this.financialService.downloadMonthlyCashFlow().subscribe({
      next: (data) => {
        const a = document.createElement('a');
        a.href = window.URL.createObjectURL(data as Blob);
        a.download = 'monthly_cash_flow.csv';
        a.click();
      }
    });
  }
}
