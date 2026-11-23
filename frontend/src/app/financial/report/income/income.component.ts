import { Component, ChangeDetectorRef } from '@angular/core';
import { MonthlyIncome } from '../../../model/bankTransaction';
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
import { ChartComponent, ApexChart, ApexAxisChartSeries, ApexXAxis, ApexYAxis, ApexLegend, ApexDataLabels, ApexTooltip } from 'ng-apexcharts';

export type IncomeChartOptions = {
  series: ApexAxisChartSeries;
  chart: ApexChart;
  xaxis: ApexXAxis;
  yaxis: ApexYAxis;
  legend: ApexLegend;
  dataLabels: ApexDataLabels;
  tooltip: ApexTooltip;
};

export interface IncomeRow extends MonthlyIncome {
  monthKey: string;
  monthTotal: number;
  share: number;
}

export interface CurrencyIncome {
  currencyId: string;
  rows: IncomeRow[];
  months: number;
  total: number;
  average: number;
  chartOptions: Partial<IncomeChartOptions>;
}

@Component({
    selector: 'app-financial-income',
    templateUrl: './income.component.html',
    styleUrls: ['./income.component.css'],
    imports: [Bind, PrimeTemplate, Toolbar, ButtonDirective, Ripple, Tooltip, TableModule, Divider,
        CurrencyPipe, DecimalPipe, ChartComponent]
})
export class FinancialIncomeComponent {

  private static readonly MAX_CHART_SERIES = 8;

  blocks: CurrencyIncome[] = [];
  loaded: boolean = false;

  constructor(private financialService: FinancialService, private cdr: ChangeDetectorRef) {
    this.fetchData();
  }

  refresh(): void {
    this.fetchData();
  }

  private fetchData(): void {
    this.financialService.getMonthlyIncome().subscribe({
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

  private groupByCurrency(data: MonthlyIncome[]): CurrencyIncome[] {
    const currencies = [...new Set(data.map(row => row.currencyId))].sort();

    return currencies.map(currencyId => {
      const source = data.filter(row => row.currencyId === currencyId);
      const totals = new Map<string, number>();
      source.forEach(row => totals.set(this.monthKey(row), (totals.get(this.monthKey(row)) ?? 0) + row.amount));

      const rows: IncomeRow[] = source.map(row => {
        const monthTotal = totals.get(this.monthKey(row)) ?? 0;
        return {
          ...row,
          monthKey: this.monthKey(row),
          monthTotal,
          share: monthTotal === 0 ? 0 : (row.amount / monthTotal) * 100
        };
      });

      const total = source.reduce((sum, row) => sum + row.amount, 0);

      return {
        currencyId,
        rows,
        months: totals.size,
        total,
        average: totals.size === 0 ? 0 : total / totals.size,
        chartOptions: this.buildChart(rows, totals)
      };
    });
  }

  private buildChart(rows: IncomeRow[], totals: Map<string, number>): Partial<IncomeChartOptions> {
    const months = [...totals.keys()].reverse();

    const bySource = new Map<string, number>();
    rows.forEach(row => bySource.set(row.source, (bySource.get(row.source) ?? 0) + row.amount));
    const ranked = [...bySource.entries()].sort((a, b) => b[1] - a[1]).map(entry => entry[0]);
    const top = ranked.slice(0, FinancialIncomeComponent.MAX_CHART_SERIES);
    const hasOther = ranked.length > top.length;

    const seriesNames = hasOther ? [...top, 'Other'] : top;
    const series = seriesNames.map(name => ({
      name,
      data: months.map(month => {
        const matching = rows.filter(row => row.monthKey === month
          && (name === 'Other' ? !top.includes(row.source) : row.source === name));
        return this.round(matching.reduce((sum, row) => sum + row.amount, 0));
      })
    }));

    return {
      series,
      chart: { type: 'bar', height: 380, stacked: true, toolbar: { show: false } },
      dataLabels: { enabled: false },
      xaxis: { categories: months },
      yaxis: { labels: { formatter: (value: number) => this.compact(value) } },
      legend: { position: 'bottom' },
      tooltip: { shared: true, intersect: false }
    };
  }

  private monthKey(row: MonthlyIncome): string {
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
    this.financialService.downloadMonthlyIncome().subscribe({
      next: (data) => {
        const a = document.createElement('a');
        a.href = window.URL.createObjectURL(data as Blob);
        a.download = 'monthly_income.csv';
        a.click();
      }
    });
  }
}
