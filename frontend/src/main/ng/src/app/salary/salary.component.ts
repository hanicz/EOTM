import { Component } from '@angular/core';
import { MenuComponent } from '../menu/menu.component';
import { SalaryHistoryComponent } from './history/history.component';
import { SalaryRaiseComponent } from './raise/raise.component';
import { Bind } from 'primeng/bind';
import { Panel } from 'primeng/panel';
import { Tabs, TabList, Tab, TabPanels, TabPanel } from 'primeng/tabs';

@Component({
    selector: 'app-salary',
    templateUrl: './salary.component.html',
    imports: [MenuComponent, Bind, Panel, Tabs, TabList, Tab, TabPanels, TabPanel, SalaryHistoryComponent,
        SalaryRaiseComponent]
})
export class SalaryComponent {

  activeTab: string = '0';
}
