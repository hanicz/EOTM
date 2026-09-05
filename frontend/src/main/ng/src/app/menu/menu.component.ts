import { Component, OnInit } from '@angular/core';
import { MenuItem, PrimeTemplate } from 'primeng/api';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { filter } from 'rxjs/operators';
import { User } from '../model/user';
import { UserService } from '../service/user.service';
import { Bind } from 'primeng/bind';
import { Menubar } from 'primeng/menubar';
import { ButtonDirective } from 'primeng/button';
import { Ripple } from 'primeng/ripple';
import { environment } from '../../environments/environment';
import { Globals } from '../util/global';

@Component({
    selector: 'menu',
    templateUrl: './menu.component.html',
    styleUrls: ['./menu.component.css'],
    imports: [Bind, Menubar, PrimeTemplate, ButtonDirective, Ripple, RouterLink]
})
export class MenuComponent implements OnInit {
  items: MenuItem[] = [];
  user: User = {} as User;
  assetUrl: string = environment.assets_url;

  readonly menuItems: MenuItem[] = [
    {
      label: 'Portfolio', icon: 'fa-solid fa-briefcase', items: [
        { label: 'Securities', icon: 'fa-solid fa-building-columns', routerLink: ['/security'] },
        { label: 'ETF', icon: 'fas fa-chart-line', routerLink: ['/etf'] },
        { label: 'Stock', icon: 'fa-solid fa-arrow-trend-up', routerLink: ['/stock'] },
        { label: 'Forex', icon: 'fa-solid fa-coins', routerLink: ['/forex'] },
        { label: 'Crypto', icon: 'fab fa-bitcoin', routerLink: ['/crypto'] },
        { label: 'Cash', icon: 'fa-solid fa-wallet', routerLink: ['/cash'] }
      ]
    },
    { label: 'Financials', icon: 'fa-solid fa-receipt', routerLink: ['/financial'] },
    { label: 'History', icon: 'fa-solid fa-clock-rotate-left', routerLink: ['/history'] },
    { label: 'FIRE', icon: 'fa-solid fa-fire', routerLink: ['/fire'] },
    { label: 'Tax', icon: 'fa-solid fa-file-invoice-dollar', routerLink: ['/tax'] },
    { label: 'Alerts & Reports', icon: 'fa-solid fa-bell', routerLink: ['/alert'] },
    { label: 'Lookup', icon: 'fas fa-search', routerLink: ['/search'] },
    { label: 'News', icon: 'far fa-newspaper', routerLink: ['/news'] }
  ];

  constructor(
    private router: Router,
    private userService: UserService,
    private globals: Globals
  ) {
    this.userService.getUserEmail().subscribe(data => this.user = data);
  }

  ngOnInit(): void {
    this.items = this.menuItems;
    this.markActiveGroups(this.router.url);
    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(event => this.markActiveGroups(event.urlAfterRedirects));
  }

  /* A parent item has no routerLink of its own, so PrimeNG never marks it
     active while one of its children is the open page. */
  private markActiveGroups(url: string): void {
    const path = url.split(/[?#]/)[0];
    this.items.forEach(item => {
      if (item.items) {
        item.styleClass = item.items.some(child => child.routerLink?.[0] === path)
          ? 'p-menubar-item-group-active'
          : undefined;
      }
    });
    this.items = [...this.items];
  }

  toggleWatchlist(): void {
    this.globals.watchlistToggleEvent.emit();
  }

  logOut(): void {
    localStorage.removeItem('token');
    this.userService.clearUserCache();
    this.router.navigate(['./']);
  }
}