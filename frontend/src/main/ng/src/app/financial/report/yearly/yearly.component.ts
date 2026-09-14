import { Component, ChangeDetectorRef } from '@angular/core';
import { NgClass, CurrencyPipe, DecimalPipe } from '@angular/common';
import { forkJoin } from 'rxjs';
import { CATEGORY_CHIP_CLASS, CATEGORY_CHIP_NONE, CategoryColor, MonthlyCategorySpending, YearlyCashFlow } from '../../../model/bankTransaction';
import { FinancialService } from '../../../service/financial.service';
import { Bind } from 'primeng/bind';
import { PrimeTemplate } from 'primeng/api';
import { Toolbar } from 'primeng/toolbar';
import { ButtonDirective } from 'primeng/button';
import { Ripple } from 'primeng/ripple';
import { Tooltip } from 'primeng/tooltip';
import { TableModule } from 'primeng/table';
import { Divider } from 'primeng/divider';
import { ChartComponent, ApexChart, ApexAxisChartSeries, ApexXAxis, ApexYAxis, ApexLegend, ApexDataLabels, ApexTooltip, ApexStroke } from 'ng-apexcharts';

export type YearlyChartOptions = {
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

export interface YearlyRow extends YearlyCashFlow {
  spendingChange: number | null;
}

export interface CategoryYearRow {
  categoryName: string;
  categoryColor: CategoryColor | null;
  amounts: number[];
  change: number | null;
}

export interface CurrencyYearly {
  currencyId: string;
  rows: YearlyRow[];
  latest: YearlyRow;
  bestSavedYear: YearlyRow | null;
  categoryYears: number[];
  categoryRows: CategoryYearRow[];
  chartOptions: Partial<YearlyChartOptions>;
}

@Component({
    selector: 'app-financial-yearly',
    templateUrl: './yearly.component.html',
    styleUrls: ['./yearly.component.css'],
    imports: [NgClass, Bind, PrimeTemplate, Toolbar, ButtonDirective, Ripple, Tooltip, TableModule, Divider,
        CurrencyPipe, DecimalPipe, ChartComponent]
})
export class FinancialYearlyComponent {

  private static readonly MAX_CATEGORY_YEARS = 5;

  blocks: CurrencyYearly[] = [];
  loaded: boolean = false;

  constructor(private financialService: FinancialService, private cdr: ChangeDetectorRef) {
    this.fetchData();
  }

  refresh(): void {
    this.fetchData();
  }

  private fetchData(): void {
    forkJoin({
      years: this.financialService.getYearlyCashFlow(),
      categories: this.financialService.getMonthlyCategorySpending()
    }).subscribe({
      next: ({ years, categories }) => {
        this.blocks = this.groupByCurrency(years, categories);
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

  private groupByCurrency(years: YearlyCashFlow[], categories: MonthlyCategorySpending[]): CurrencyYearly[] {
    const currencies = [...new Set(years.map(row => row.currencyId))].sort();

    return currencies.map(currencyId => {
      const source = years.filter(row => row.currencyId === currencyId).sort((a, b) => b.year - a.year);
      const rows: YearlyRow[] = source.map((row, index) => ({
        ...row,
        spendingChange: this.spendingChange(row, source[index + 1])
      }));
      const categoryYears = rows.map(row => row.year).slice(0, FinancialYearlyComponent.MAX_CATEGORY_YEARS);

      return {
        currencyId,
        rows,
        latest: rows[0],
        bestSavedYear: this.bestSavedYear(rows),
        categoryYears,
        categoryRows: this.buildCategoryRows(categories.filter(row => row.currencyId === currencyId), rows,
          categoryYears),
        chartOptions: this.buildChart([...rows].reverse())
      };
    });
  }

  private spendingChange(current: YearlyCashFlow, previous: YearlyCashFlow | undefined): number | null {
    if (!previous || previous.year !== current.year - 1) {
      return null;
    }
    return this.percentChange(Math.abs(current.averageMonthlySpending ?? 0),
      Math.abs(previous.averageMonthlySpending ?? 0));
  }

  private bestSavedYear(rows: YearlyRow[]): YearlyRow | null {
    return rows.reduce<YearlyRow | null>((best, row) => {
      if (row.savedPercent === null) {
        return best;
      }
      return best === null || row.savedPercent > best.savedPercent! ? row : best;
    }, null);
  }

  private buildCategoryRows(source: MonthlyCategorySpending[], rows: YearlyRow[], years: number[]): CategoryYearRow[] {
    const monthsByYear = new Map(rows.map(row => [row.year, row.monthsCounted]));
    const byCategory = new Map<string, CategoryYearRow>();

    source.forEach(row => {
      const index = years.indexOf(row.year);
      if (index < 0) {
        return;
      }
      let entry = byCategory.get(row.categoryName);
      if (!entry) {
        entry = { categoryName: row.categoryName, categoryColor: row.categoryColor, amounts: years.map(() => 0), change: null };
        byCategory.set(row.categoryName, entry);
      }
      entry.amounts[index] += row.amount;
    });

    const consecutive = years.length > 1 && years[1] === years[0] - 1;

    return [...byCategory.values()]
      .map(entry => ({
        ...entry,
        amounts: entry.amounts.map(amount => this.round(amount)),
        change: consecutive
          ? this.percentChange(this.perMonth(entry.amounts[0], monthsByYear.get(years[0])),
            this.perMonth(entry.amounts[1], monthsByYear.get(years[1])))
          : null
      }))
      .sort((a, b) => (b.amounts[0] - a.amounts[0]) || (this.sum(b.amounts) - this.sum(a.amounts)));
  }

  private perMonth(amount: number, months: number | undefined): number {
    return months ? amount / months : 0;
  }

  private percentChange(current: number, previous: number): number | null {
    return previous === 0 ? null : ((current - previous) / previous) * 100;
  }

  private sum(values: number[]): number {
    return values.reduce((total, value) => total + value, 0);
  }

  private buildChart(rows: YearlyRow[]): Partial<YearlyChartOptions> {
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
      xaxis: { categories: rows.map(row => String(row.year)) },
      yaxis: { labels: { formatter: (value: number) => this.compact(value) } },
      legend: { position: 'bottom' },
      tooltip: { shared: true, intersect: false }
    };
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
    this.financialService.downloadYearlyCashFlow().subscribe({
      next: (data) => {
        const a = document.createElement('a');
        a.href = window.URL.createObjectURL(data as Blob);
        a.download = 'yearly_cash_flow.csv';
        a.click();
      }
    });
  }
}
