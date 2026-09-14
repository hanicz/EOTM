import { Component, ChangeDetectorRef } from '@angular/core';
import { NgClass } from '@angular/common';
import { CATEGORY_CHIP_CLASS, CATEGORY_CHIP_NONE, CATEGORY_HEX, CategoryColor, MonthlyCategorySpending } from '../../../model/bankTransaction';
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

export type CategoryChartOptions = {
  series: ApexAxisChartSeries;
  chart: ApexChart;
  xaxis: ApexXAxis;
  yaxis: ApexYAxis;
  legend: ApexLegend;
  dataLabels: ApexDataLabels;
  tooltip: ApexTooltip;
  colors: string[];
};

export interface CategorySpendingRow extends MonthlyCategorySpending {
  monthKey: string;
  monthTotal: number;
  share: number;
}

export interface CurrencyCategorySpending {
  currencyId: string;
  rows: CategorySpendingRow[];
  months: number;
  total: number;
  average: number;
  topName: string;
  topAmount: number;
  chartOptions: Partial<CategoryChartOptions>;
}

@Component({
    selector: 'app-financial-category-report',
    templateUrl: './category.component.html',
    styleUrls: ['./category.component.css'],
    imports: [NgClass, Bind, PrimeTemplate, Toolbar, ButtonDirective, Ripple, Tooltip, TableModule, Divider,
        CurrencyPipe, DecimalPipe, ChartComponent]
})
export class FinancialCategoryReportComponent {

  private static readonly MAX_CHART_SERIES = 8;
  private static readonly OTHER = 'Other';
  private static readonly OTHER_HEX = '#8a8880';

  blocks: CurrencyCategorySpending[] = [];
  loaded: boolean = false;

  constructor(private financialService: FinancialService, private cdr: ChangeDetectorRef) {
    this.fetchData();
  }

  refresh(): void {
    this.fetchData();
  }

  private fetchData(): void {
    this.financialService.getMonthlyCategorySpending().subscribe({
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

  chipClass(color: CategoryColor | null): string {
    return color ? CATEGORY_CHIP_CLASS[color] : CATEGORY_CHIP_NONE;
  }

  hex(color: CategoryColor | null): string {
    return color ? CATEGORY_HEX[color] : FinancialCategoryReportComponent.OTHER_HEX;
  }

  private groupByCurrency(data: MonthlyCategorySpending[]): CurrencyCategorySpending[] {
    const currencies = [...new Set(data.map(row => row.currencyId))].sort();

    return currencies.map(currencyId => {
      const source = data.filter(row => row.currencyId === currencyId);
      const totals = new Map<string, number>();
      source.forEach(row => totals.set(this.monthKey(row), (totals.get(this.monthKey(row)) ?? 0) + row.amount));

      const rows: CategorySpendingRow[] = source.map(row => {
        const monthTotal = totals.get(this.monthKey(row)) ?? 0;
        return {
          ...row,
          monthKey: this.monthKey(row),
          monthTotal,
          share: monthTotal === 0 ? 0 : (row.amount / monthTotal) * 100
        };
      });

      const total = source.reduce((sum, row) => sum + row.amount, 0);
      const byCategory = this.byCategory(rows);
      const ranked = [...byCategory.entries()].sort((a, b) => b[1] - a[1]);

      return {
        currencyId,
        rows,
        months: totals.size,
        total,
        average: totals.size === 0 ? 0 : total / totals.size,
        topName: ranked[0]?.[0] ?? '',
        topAmount: ranked[0]?.[1] ?? 0,
        chartOptions: this.buildChart(rows, totals)
      };
    });
  }

  private byCategory(rows: CategorySpendingRow[]): Map<string, number> {
    const byCategory = new Map<string, number>();
    rows.forEach(row => byCategory.set(row.categoryName, (byCategory.get(row.categoryName) ?? 0) + row.amount));
    return byCategory;
  }

  private buildChart(rows: CategorySpendingRow[], totals: Map<string, number>): Partial<CategoryChartOptions> {
    const months = [...totals.keys()].reverse();

    const ranked = [...this.byCategory(rows).entries()].sort((a, b) => b[1] - a[1]).map(entry => entry[0]);
    const top = ranked.slice(0, FinancialCategoryReportComponent.MAX_CHART_SERIES);
    const hasOther = ranked.length > top.length;

    const seriesNames = hasOther ? [...top, FinancialCategoryReportComponent.OTHER] : top;
    const series = seriesNames.map(name => ({
      name,
      data: months.map(month => {
        const matching = rows.filter(row => row.monthKey === month
          && (name === FinancialCategoryReportComponent.OTHER ? !top.includes(row.categoryName)
            : row.categoryName === name));
        return this.round(matching.reduce((sum, row) => sum + row.amount, 0));
      })
    }));

    return {
      series,
      colors: seriesNames.map(name => this.seriesHex(name, rows)),
      chart: { type: 'bar', height: 380, stacked: true, toolbar: { show: false } },
      dataLabels: { enabled: false },
      xaxis: { categories: months },
      yaxis: { labels: { formatter: (value: number) => this.compact(value) } },
      legend: { position: 'bottom' },
      tooltip: { shared: true, intersect: false }
    };
  }

  private seriesHex(name: string, rows: CategorySpendingRow[]): string {
    if (name === FinancialCategoryReportComponent.OTHER) {
      return FinancialCategoryReportComponent.OTHER_HEX;
    }
    const color = rows.find(row => row.categoryName === name)?.categoryColor;
    return color ? CATEGORY_HEX[color] : FinancialCategoryReportComponent.OTHER_HEX;
  }

  private monthKey(row: MonthlyCategorySpending): string {
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
    this.financialService.downloadMonthlyCategorySpending().subscribe({
      next: (data) => {
        const a = document.createElement('a');
        a.href = window.URL.createObjectURL(data as Blob);
        a.download = 'spending_by_category.csv';
        a.click();
      }
    });
  }
}
