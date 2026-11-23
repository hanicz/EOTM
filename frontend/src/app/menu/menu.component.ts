import { Component, OnInit, signal } from '@angular/core';
import { MenuItem, PrimeTemplate } from 'primeng/api';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { filter } from 'rxjs/operators';
import { UserProfile } from '../model/userprofile';
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
  readonly user = signal<UserProfile>({} as UserProfile);
  assetUrl: string = environment.assets_url;

  readonly menuItems: MenuItem[] = [
    {
      label: 'Portfolio', icon: 'eotm-icon eotm-icon-briefcase', items: [
        { label: 'Performance', icon: 'eotm-icon eotm-icon-chart-area', routerLink: ['/performance'] },
        { label: 'Securities', icon: 'eotm-icon eotm-icon-building-columns', routerLink: ['/security'] },
        { label: 'ETF', icon: 'eotm-icon eotm-icon-chart-line', routerLink: ['/etf'] },
        { label: 'Stock', icon: 'eotm-icon eotm-icon-arrow-trend-up', routerLink: ['/stock'] },
        { label: 'Forex', icon: 'eotm-icon eotm-icon-coins', routerLink: ['/forex'] },
        { label: 'Crypto', icon: 'eotm-icon eotm-icon-bitcoin', routerLink: ['/crypto'] },
        { label: 'Cash', icon: 'eotm-icon eotm-icon-money-bill', routerLink: ['/cash'] },
        { label: 'Pension', icon: 'eotm-icon eotm-icon-piggy-bank', routerLink: ['/pension'] }
      ]
    },
    { label: 'Financials', icon: 'eotm-icon eotm-icon-credit-card', routerLink: ['/financial'] },
    { label: 'Salary', icon: 'eotm-icon eotm-icon-wallet', routerLink: ['/salary'] },
    { label: 'Equity', icon: 'eotm-icon eotm-icon-star', routerLink: ['/equity'] },
    { label: 'FIRE', icon: 'eotm-icon eotm-icon-fire', routerLink: ['/fire'] },
    { label: 'Tax', icon: 'eotm-icon eotm-icon-file-invoice-dollar', routerLink: ['/tax'] },
    { label: 'Alerts & Reports', icon: 'eotm-icon eotm-icon-bell', routerLink: ['/alert'] },
    { label: 'Research', icon: 'eotm-icon eotm-icon-search', routerLink: ['/search'] },
    { label: 'News', icon: 'eotm-icon eotm-icon-newspaper', routerLink: ['/news'] }
  ];

  constructor(
    private router: Router,
    private userService: UserService,
    private globals: Globals
  ) {
    this.userService.getCurrentUser().subscribe(data => this.user.set(data));
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