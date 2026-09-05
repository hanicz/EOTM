import { Component } from '@angular/core';
import { MenuComponent } from '../menu/menu.component';
import { SalaryComponent } from './salary/salary.component';
import { Bind } from 'primeng/bind';
import { Panel } from 'primeng/panel';
import { Tabs, TabList, Tab, TabPanels, TabPanel } from 'primeng/tabs';

@Component({
    selector: 'app-history',
    templateUrl: './history.component.html',
    imports: [MenuComponent, Bind, Panel, Tabs, TabList, Tab, TabPanels, TabPanel, SalaryComponent]
})
export class HistoryComponent {

  activeTab: string = '0';
}
