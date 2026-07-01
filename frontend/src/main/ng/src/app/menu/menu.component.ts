import { Component, OnInit } from '@angular/core';
import { MenuItem, PrimeTemplate } from 'primeng/api';
import { Router } from '@angular/router';
import { User } from '../model/user';
import { UserService } from '../service/user.service';
import { Bind } from 'primeng/bind';
import { Menubar } from 'primeng/menubar';
import { ButtonDirective } from 'primeng/button';
import { Ripple } from 'primeng/ripple';

@Component({
    selector: 'menu',
    templateUrl: './menu.component.html',
    styleUrls: ['./menu.component.css'],
    imports: [Bind, Menubar, PrimeTemplate, ButtonDirective, Ripple]
})
export class MenuComponent implements OnInit {
  items: MenuItem[] = [];
  user: User = {} as User;

  readonly menuItems: MenuItem[] = [
    { label: 'Dashboard', icon: 'fa-solid fa-gauge', routerLink: ['/dashboard'] },
    { label: 'News', icon: 'far fa-newspaper', routerLink: ['/home'] },
    { label: 'Stock', icon: 'fa-solid fa-arrow-trend-up', routerLink: ['/stock'] },
    { label: 'Crypto', icon: 'fab fa-bitcoin', routerLink: ['/crypto'] },
    { label: 'ETF', icon: 'fas fa-chart-line', routerLink: ['/etf'] },
    { label: 'Forex', icon: 'fa-solid fa-coins', routerLink: ['/forex'] },
    { label: 'Securities', icon: 'fa-solid fa-building-columns', routerLink: ['/security'] },
    { label: 'Alerts', icon: 'fa-solid fa-bell', routerLink: ['/alert'] },
    { label: 'Lookup', icon: 'fas fa-search', routerLink: ['/search'] },
    { label: 'Settings', icon: 'fa-solid fa-gear', routerLink: ['/settings'] }
  ];

  constructor(
    private router: Router,
    private userService: UserService
  ) {
    this.userService.getUserEmail().subscribe(data => this.user = data);
  }

  ngOnInit(): void {
    this.items = this.menuItems;
  }

  logOut(): void {
    localStorage.removeItem('token');
    this.userService.clearUserCache();
    this.router.navigate(['./']);
  }
}