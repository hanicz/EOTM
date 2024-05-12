import { Component, OnInit } from '@angular/core';
import { MenuItem } from 'primeng/api';
import { Router } from '@angular/router';
import { Globals } from '../util/global';
import { User } from '../model/user';
import { UserService } from '../service/user.service';

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
  selectedETF: string = "EUR";
  user: User = {} as User;

  constructor(private router: Router, globals: Globals, private userService: UserService) {
    this.globals = globals;
    
    this.userService.getUserEmail().subscribe({
      next: (data) => {
        this.user = data;
      }
    });
  }

  changedCrypto() {
    this.globals.changeCryptoCurrency(this.selectedCrypto);
  }

  changedStock() {
    this.globals.changeStockCurrency(this.selectedStock);
  }

  changedETF() {
    this.globals.changeETFCurrency(this.selectedETF);
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
        icon: 'fa-solid fa-arrow-trend-up',
        routerLink: ['/stock']
      },
      {
        label: 'Crypto',
        icon: 'fab fa-bitcoin',
        routerLink: ['/crypto']
      },
      {
        label: 'ETF',
        icon: 'fas fa-chart-line',
        routerLink: ['/etf']
      },
      {
        label: 'Forex',
        icon: 'fa-solid fa-coins',
        routerLink: ['/forex']
      },
      {
        label: 'Alerts',
        icon: 'fa-solid fa-bell',
        routerLink: ['/alert']
      },
      {
        label: 'Lookup',
        icon: 'fas fa-search',
        routerLink: ['/search']
      },
      {
        label: 'Settings',
        icon: 'fa-solid fa-gear',
        routerLink: ['/settings']
      }
    ];
  }

  logOut() {
    localStorage.removeItem('token');
    this.router.navigate(['./'])
  }

}
