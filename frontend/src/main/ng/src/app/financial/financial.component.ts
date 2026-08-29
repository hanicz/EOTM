import { Component, ViewChild } from '@angular/core';
import { MenuComponent } from '../menu/menu.component';
import { FinancialTransactionComponent } from './transaction/transaction.component';
import { FinancialReportComponent } from './report/report.component';
import { FinancialRuleComponent } from './rule/rule.component';
import { ExclusionRuleRequest } from '../model/bankTransaction';
import { Bind } from 'primeng/bind';
import { Panel } from 'primeng/panel';
import { Tabs, TabList, Tab, TabPanels, TabPanel } from 'primeng/tabs';

@Component({
    selector: 'app-financial',
    templateUrl: './financial.component.html',
    imports: [MenuComponent, Bind, Panel, Tabs, TabList, Tab, TabPanels, TabPanel,
        FinancialTransactionComponent, FinancialReportComponent, FinancialRuleComponent]
})
export class FinancialComponent {

  @ViewChild(FinancialTransactionComponent) transactions!: FinancialTransactionComponent;
  @ViewChild(FinancialReportComponent) report!: FinancialReportComponent;

  activeTab: string = '0';
  ruleRequest: ExclusionRuleRequest | null = null;
  private ruleRequestSeq = 0;

  onDataChanged(): void {
    this.report?.refresh();
  }

  onCreateRule(accountNumber: string): void {
    this.ruleRequest = { accountNumber, seq: ++this.ruleRequestSeq };
    this.activeTab = '2';
  }
}
