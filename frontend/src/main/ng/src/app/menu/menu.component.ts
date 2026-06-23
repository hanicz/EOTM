import { Component, OnInit } from '@angular/core';
import { MenuItem } from 'primeng/api';
import { Router } from '@angular/router';
import { Globals } from '../util/global';
import { User } from '../model/user';
import { UserService } from '../service/user.service';

interface CurrencyType {
  id: string;
  label: string;
  key: keyof typeof MenuComponent.prototype.selectedCurrencies;
}

@Component({
  selector: 'menu',
  templateUrl: './menu.component.html',
  styleUrls: ['./menu.component.css']
})
export class MenuComponent implements OnInit {
  items: MenuItem[] = [];
  user: User = {} as User;
  
  selectedCurrencies = {
    stock: 'USD',
    etf: 'EUR'
  };

  currencyTypes: CurrencyType[] = [
    { id: 'stockDropdown', label: 'Stock', key: 'stock' },
    { id: 'etfDropdown', label: 'ETF', key: 'etf' }
  ];

  readonly menuItems: MenuItem[] = [
    { label: 'News', icon: 'far fa-newspaper', routerLink: ['/home'] },
    { label: 'Stock', icon: 'fa-solid fa-arrow-trend-up', routerLink: ['/stock'] },
    { label: 'Crypto', icon: 'fab fa-bitcoin', routerLink: ['/crypto'] },
    { label: 'ETF', icon: 'fas fa-chart-line', routerLink: ['/etf'] },
    { label: 'Forex', icon: 'fa-solid fa-coins', routerLink: ['/forex'] },
    { label: 'Alerts', icon: 'fa-solid fa-bell', routerLink: ['/alert'] },
    { label: 'Lookup', icon: 'fas fa-search', routerLink: ['/search'] },
    { label: 'Settings', icon: 'fa-solid fa-gear', routerLink: ['/settings'] }
  ];

  constructor(
    private router: Router,
    public globals: Globals,
    private userService: UserService
  ) {
    this.userService.getUserEmail().subscribe(data => this.user = data);
  }

  ngOnInit(): void {
    this.items = this.menuItems;
  }

  onCurrencyChange(type: string): void {
    const methodMap = {
      stock: () => this.globals.changeStockCurrency(this.selectedCurrencies.stock),
      etf: () => this.globals.changeETFCurrency(this.selectedCurrencies.etf)
    };
    methodMap[type as keyof typeof methodMap]?.();
  }

  logOut(): void {
    localStorage.removeItem('token');
    this.router.navigate(['./']);
  }
}