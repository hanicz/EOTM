import { Component, OnInit } from '@angular/core';
import { MenuItem } from 'primeng/api';
import { Router } from '@angular/router';
import { Globals } from '../util/global';

@Component({
  selector: 'menu',
  templateUrl: './menu.component.html',
  styleUrls: ['./menu.component.css']
})
export class MenuComponent implements OnInit {
  globals: Globals;
  items: MenuItem[] = [];
  selectedCrypto: string = "EUR";
  selectedStock: string = "USD";

  constructor(private router: Router, globals: Globals) {
    this.globals = globals;
  }

  changedCrypto() {
    this.globals.changeCryptoCurrency(this.selectedCrypto);
  }

  changedStock() {
    this.globals.changeStockCurrency(this.selectedStock);
  }

  ngOnInit(): void {
    this.items = [
      {
        label: 'News',
        icon: 'far fa-newspaper',
        routerLink: ['/home']
      },
      {
        label: 'Stock',
        icon: 'fas fa-exchange-alt',
        routerLink: ['/stock']
      },
      {
        label: 'Crypto',
        icon: 'fab fa-bitcoin',
        routerLink: ['/crypto']
      },
      {
        label: 'Lookup',
        icon: 'fas fa-search',
        routerLink: ['/search']
      }
    ];
  }

}