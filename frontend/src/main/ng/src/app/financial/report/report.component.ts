import { Component, ViewChild } from '@angular/core';
import { FinancialCashFlowComponent } from './cashflow/cashflow.component';
import { FinancialIncomeComponent } from './income/income.component';
import { FinancialCategoryReportComponent } from './category/category.component';
import { FinancialYearlyComponent } from './yearly/yearly.component';
import { Bind } from 'primeng/bind';
import { Tabs, TabList, Tab, TabPanels, TabPanel } from 'primeng/tabs';

@Component({
    selector: 'app-financial-report',
    templateUrl: './report.component.html',
    imports: [Bind, Tabs, TabList, Tab, TabPanels, TabPanel, FinancialCashFlowComponent, FinancialIncomeComponent,
        FinancialCategoryReportComponent, FinancialYearlyComponent]
})
export class FinancialReportComponent {

  @ViewChild(FinancialCashFlowComponent) cashFlow!: FinancialCashFlowComponent;
  @ViewChild(FinancialIncomeComponent) income!: FinancialIncomeComponent;
  @ViewChild(FinancialCategoryReportComponent) categorySpending!: FinancialCategoryReportComponent;
  @ViewChild(FinancialYearlyComponent) yearly!: FinancialYearlyComponent;

  refresh(): void {
    this.cashFlow?.refresh();
    this.income?.refresh();
    this.categorySpending?.refresh();
    this.yearly?.refresh();
  }
}
