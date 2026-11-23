import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MenuComponent } from '../menu/menu.component';
import { EquityRsuComponent } from './rsu/rsu.component';
import { EquityStarComponent } from './star/star.component';
import { Bind } from 'primeng/bind';
import { Panel } from 'primeng/panel';
import { Tabs, TabList, Tab, TabPanels, TabPanel } from 'primeng/tabs';

const TABS: { [name: string]: string } = { rsu: '0', star: '1' };

@Component({
    selector: 'app-equity',
    templateUrl: './equity.component.html',
    imports: [MenuComponent, Bind, Panel, Tabs, TabList, Tab, TabPanels, TabPanel, EquityRsuComponent, EquityStarComponent]
})
export class EquityComponent {

  activeTab: string = TABS[inject(ActivatedRoute).snapshot.queryParamMap.get('tab') ?? ''] ?? '0';
}
