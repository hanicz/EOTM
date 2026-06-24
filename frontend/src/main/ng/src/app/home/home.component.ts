import { Component, OnInit } from '@angular/core';
import { MenuComponent } from '../menu/menu.component';
import { Bind } from 'primeng/bind';
import { Panel } from 'primeng/panel';
import { SelectButton } from 'primeng/selectbutton';
import { FormsModule } from '@angular/forms';
import { NewsComponent } from '../news/news.component';

@Component({
    selector: 'app-home',
    templateUrl: './home.component.html',
    styleUrls: ['./home.component.css'],
    imports: [MenuComponent, Bind, Panel, SelectButton, FormsModule, NewsComponent]
})
export class HomeComponent implements OnInit {

  selectedType = 'category/reddit';
  options: any[];

  constructor() {
    this.options = [
      { label: 'Reddit', value: 'category/reddit' },
      { label: 'General', value: 'category/general' },
      { label: 'Forex', value: 'category/forex' },
      { label: 'Crypto', value: 'category/crypto' }
    ];
  }

  ngOnInit(): void {
  }
}
