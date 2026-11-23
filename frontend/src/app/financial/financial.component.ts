import { Component, ViewChild } from '@angular/core';
import { MenuComponent } from '../menu/menu.component';
import { FinancialTransactionComponent } from './transaction/transaction.component';
import { FinancialReportComponent } from './report/report.component';
import { FinancialRuleComponent } from './rule/rule.component';
import { FinancialCategoryComponent } from './category/category.component';
import { FinancialCategoryRuleComponent } from './categoryrule/categoryrule.component';
import { ExclusionRuleRequest } from '../model/bankTransaction';
import { Bind } from 'primeng/bind';
import { Panel } from 'primeng/panel';
import { Divider } from 'primeng/divider';
import { Tabs, TabList, Tab, TabPanels, TabPanel } from 'primeng/tabs';

@Component({
    selector: 'app-financial',
    templateUrl: './financial.component.html',
    imports: [MenuComponent, Bind, Panel, Divider, Tabs, TabList, Tab, TabPanels, TabPanel,
        FinancialTransactionComponent, FinancialReportComponent, FinancialRuleComponent,
        FinancialCategoryComponent, FinancialCategoryRuleComponent]
})
export class FinancialComponent {

  @ViewChild(FinancialTransactionComponent) transactions!: FinancialTransactionComponent;
  @ViewChild(FinancialReportComponent) report!: FinancialReportComponent;
  @ViewChild(FinancialCategoryRuleComponent) categoryRules!: FinancialCategoryRuleComponent;

  activeTab: string = '0';
  ruleRequest: ExclusionRuleRequest | null = null;
  private ruleRequestSeq = 0;

  onDataChanged(): void {
    this.report?.refresh();
  }

  onCategoriesChanged(): void {
    this.transactions?.refresh();
    this.categoryRules?.refresh();
    this.report?.refresh();
  }

  onCategoryRulesChanged(): void {
    this.categoryRules?.refresh();
  }

  onCreateRule(accountNumber: string): void {
    this.ruleRequest = { accountNumber, seq: ++this.ruleRequestSeq };
    this.activeTab = '2';
  }
}
