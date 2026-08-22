import { Component, ViewChild } from '@angular/core';
import { FinancialCashFlowComponent } from './cashflow/cashflow.component';
import { FinancialIncomeComponent } from './income/income.component';
import { Bind } from 'primeng/bind';
import { Tabs, TabList, Tab, TabPanels, TabPanel } from 'primeng/tabs';

@Component({
    selector: 'app-financial-report',
    templateUrl: './report.component.html',
    imports: [Bind, Tabs, TabList, Tab, TabPanels, TabPanel, FinancialCashFlowComponent, FinancialIncomeComponent]
})
export class FinancialReportComponent {

  @ViewChild(FinancialCashFlowComponent) cashFlow!: FinancialCashFlowComponent;
  @ViewChild(FinancialIncomeComponent) income!: FinancialIncomeComponent;

  refresh(): void {
    this.cashFlow?.refresh();
    this.income?.refresh();
  }
}
