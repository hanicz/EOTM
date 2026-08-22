import { Component, ViewChild } from '@angular/core';
import { MenuComponent } from '../menu/menu.component';
import { FinancialTransactionComponent } from './transaction/transaction.component';
import { FinancialReportComponent } from './report/report.component';
import { Bind } from 'primeng/bind';
import { Panel } from 'primeng/panel';
import { Tabs, TabList, Tab, TabPanels, TabPanel } from 'primeng/tabs';

@Component({
    selector: 'app-financial',
    templateUrl: './financial.component.html',
    imports: [MenuComponent, Bind, Panel, Tabs, TabList, Tab, TabPanels, TabPanel,
        FinancialTransactionComponent, FinancialReportComponent]
})
export class FinancialComponent {

  @ViewChild(FinancialTransactionComponent) transactions!: FinancialTransactionComponent;
  @ViewChild(FinancialReportComponent) report!: FinancialReportComponent;

  onDataChanged(): void {
    this.report?.refresh();
  }
}
