import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { MenuComponent } from '../menu/menu.component';
import { Bind } from 'primeng/bind';
import { Panel } from 'primeng/panel';
import { ButtonDirective } from 'primeng/button';
import { Ripple } from 'primeng/ripple';
import { Tooltip } from 'primeng/tooltip';
import { Skeleton } from 'primeng/skeleton';
import { DecimalPipe, CurrencyPipe } from '@angular/common';
import { NetWorthService } from '../service/networth.service';
import { AlertService } from '../service/alert.service';
import { NetWorth } from '../model/networth';
import { DEFAULT_CURRENCY } from '../model/currency';
import { UserService } from '../service/user.service';
import { StockAlert } from '../model/stockalert';
import { CryptoAlert } from '../model/cryptoalert';
import { AlertTypePipe } from '../util/pipe';
import { MarketStatusComponent } from './market-status/market-status.component';
import { NotepadComponent } from './notepad/notepad.component';
import { UpcomingInterestComponent } from './upcoming-interest/upcoming-interest.component';
import { UpcomingVestComponent } from './upcoming-vest/upcoming-vest.component';
import { UpcomingStarVestComponent } from './upcoming-star-vest/upcoming-star-vest.component';
import { FireSummaryComponent } from './fire-summary/fire-summary.component';
import { PerformanceSummaryComponent } from './performance-summary/performance-summary.component';
import { ASSET_COLOURS } from '../util/assetcolours';
import { AllocationItem } from '../util/allocation';
import { AllocationDonutComponent } from '../util/allocation-donut.component';

const ASSET_CLASS_ROUTES: { [assetClass: string]: string } = {
  'Stock': '/stock',
  'Crypto': '/crypto',
  'ETF': '/etf',
  'Forex': '/forex',
  'Securities': '/security',
  'Cash': '/cash',
  'Pension': '/pension',
};

@Component({
    selector: 'app-dashboard',
    templateUrl: './dashboard.component.html',
    styleUrls: ['./dashboard.component.css'],
    imports: [MenuComponent, Bind, Panel, ButtonDirective, Ripple, Tooltip, Skeleton, DecimalPipe, CurrencyPipe, AlertTypePipe, MarketStatusComponent, NotepadComponent, UpcomingInterestComponent, UpcomingVestComponent, UpcomingStarVestComponent, FireSummaryComponent, PerformanceSummaryComponent, AllocationDonutComponent]
})
export class DashboardComponent implements OnInit {

  loading: boolean = true;
  refreshHistory: boolean = false;

  selectedCurrency: string = DEFAULT_CURRENCY;

  stockTotal: number = 0;
  stockChangePct: number = 0;
  cryptoTotal: number = 0;
  cryptoChangePct: number = 0;
  etfTotal: number = 0;
  etfChangePct: number = 0;
  forexTotal: number = 0;
  forexChangePct: number = 0;
  securityTotal: number = 0;
  securityRatePct: number = 0;
  cashTotal: number = 0;
  pensionTotal: number = 0;
  pensionChangePct: number = 0;

  netWorth: number = 0;
  netWorthChangePct: number = 0;

  allocationItems: AllocationItem[] = [];
  readonly assetColours = ASSET_COLOURS;

  stockAlerts: StockAlert[] = [];
  cryptoAlerts: CryptoAlert[] = [];

  constructor(
    private netWorthService: NetWorthService,
    private alertService: AlertService,
    private cdr: ChangeDetectorRef,
    private router: Router,
    private userService: UserService,
  ) {
    this.loadData();
  }

  ngOnInit(): void {
  }

  navigateTo(path: string, queryParams?: { [name: string]: string }): void {
    this.router.navigate([path], { queryParams });
  }

  openAssetClass(assetClass: string): void {
    const path = ASSET_CLASS_ROUTES[assetClass];
    if (path) {
      this.navigateTo(path);
    }
  }

  refresh(): void {
    this.loading = true;
    this.loadData(true);
  }

  private loadData(forceRefresh = false): void {
    this.userService.getPreferredCurrency().pipe(
      switchMap(currency => {
        this.selectedCurrency = currency;
        return forkJoin({
          netWorth: this.netWorthService.getNetWorth(currency, forceRefresh),
          stockAlerts: this.alertService.getStockAlerts(),
          cryptoAlerts: this.alertService.getCryptoAlerts(),
        });
      })
    ).subscribe({
      next: ({ netWorth, stockAlerts, cryptoAlerts }) => {
        this.stockAlerts = stockAlerts;
        this.cryptoAlerts = cryptoAlerts;
        this.apply(netWorth);
        this.refreshHistory = forceRefresh;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.log(error);
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  private apply(netWorth: NetWorth): void {
    this.stockTotal = this.worthOf(netWorth, 'Stock');
    this.stockChangePct = this.changeOf(netWorth, 'Stock');
    this.cryptoTotal = this.worthOf(netWorth, 'Crypto');
    this.cryptoChangePct = this.changeOf(netWorth, 'Crypto');
    this.etfTotal = this.worthOf(netWorth, 'ETF');
    this.etfChangePct = this.changeOf(netWorth, 'ETF');
    this.forexTotal = this.worthOf(netWorth, 'Forex');
    this.forexChangePct = this.changeOf(netWorth, 'Forex');
    this.securityTotal = this.worthOf(netWorth, 'Securities');
    this.securityRatePct = this.expectedRateOf(netWorth, 'Securities');
    this.cashTotal = this.worthOf(netWorth, 'Cash');
    this.pensionTotal = this.worthOf(netWorth, 'Pension');
    this.pensionChangePct = this.changeOf(netWorth, 'Pension');

    this.netWorth = netWorth.totalWorth;
    this.netWorthChangePct = netWorth.totalChangePct;

    this.allocationItems = (netWorth.assets ?? []).map(asset => ({ label: asset.assetClass, value: asset.worth }));
  }

  get hasAllocation(): boolean {
    return this.allocationItems.some(item => item.value > 0);
  }

  private worthOf(netWorth: NetWorth, assetClass: string): number {
    return netWorth.assets?.find(a => a.assetClass === assetClass)?.worth ?? 0;
  }

  private changeOf(netWorth: NetWorth, assetClass: string): number {
    return netWorth.assets?.find(a => a.assetClass === assetClass)?.changePct ?? 0;
  }

  private expectedRateOf(netWorth: NetWorth, assetClass: string): number {
    return netWorth.assets?.find(a => a.assetClass === assetClass)?.expectedRatePct ?? 0;
  }
}
