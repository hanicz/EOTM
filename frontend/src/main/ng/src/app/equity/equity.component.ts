import { Component } from '@angular/core';
import { MenuComponent } from '../menu/menu.component';
import { EquityRsuComponent } from './rsu/rsu.component';
import { EquityStarComponent } from './star/star.component';
import { Bind } from 'primeng/bind';
import { Panel } from 'primeng/panel';
import { Tabs, TabList, Tab, TabPanels, TabPanel } from 'primeng/tabs';

@Component({
    selector: 'app-equity',
    templateUrl: './equity.component.html',
    imports: [MenuComponent, Bind, Panel, Tabs, TabList, Tab, TabPanels, TabPanel, EquityRsuComponent, EquityStarComponent]
})
export class EquityComponent {

  activeTab: string = '0';
}
