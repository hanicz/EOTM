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

interface AssetSlice {
  label: string;
  value: number;
  percentage: number;
  color: string;
}

interface DonutSegment {
  label: string;
  value: number;
  percentage: number;
  color: string;
  dashArray: string;
  dashOffset: string;
}

/** Matches the asset class names the backend reports. */
const ASSET_COLOURS: { [assetClass: string]: string } = {
  'Stock': '#ef9f27',
  'Crypto': '#5f5e5a',
  'ETF': '#1b1b1b',
  'Forex': '#b4b2a9',
  'Securities': '#7a8c5c',
  'Cash': '#c9a227',
  'Pension': '#5c7a8c',
};

@Component({
    selector: 'app-dashboard',
    templateUrl: './dashboard.component.html',
    styleUrls: ['./dashboard.component.css'],
    imports: [MenuComponent, Bind, Panel, ButtonDirective, Ripple, Tooltip, Skeleton, DecimalPipe, CurrencyPipe, AlertTypePipe, MarketStatusComponent, NotepadComponent, UpcomingInterestComponent, UpcomingVestComponent, UpcomingStarVestComponent, FireSummaryComponent]
})
export class DashboardComponent implements OnInit {

  loading: boolean = true;

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

  slices: AssetSlice[] = [];
  donutSegments: DonutSegment[] = [];
  hoveredLabel: string | null = null;

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

  navigateTo(path: string): void {
    this.router.navigate([path]);
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

    this.buildAllocation(netWorth);
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

  private buildAllocation(netWorth: NetWorth): void {
    const raw: AssetSlice[] = (netWorth.assets ?? []).map(asset => ({
      label: asset.assetClass,
      value: asset.worth,
      percentage: 0,
      color: ASSET_COLOURS[asset.assetClass] ?? '#b4b2a9',
    })).sort((a, b) => b.value - a.value);

    const total = raw.reduce((sum, slice) => sum + slice.value, 0);
    if (total <= 0) {
      this.slices = raw;
      this.donutSegments = [];
      return;
    }

    raw.forEach(slice => slice.percentage = (slice.value / total) * 100);
    this.slices = raw;

    const circumference = 2 * Math.PI * 40;
    let offset = 0;
    this.donutSegments = raw.filter(s => s.value > 0).map(slice => {
      const length = (slice.percentage / 100) * circumference;
      const segment: DonutSegment = {
        label: slice.label,
        value: slice.value,
        percentage: slice.percentage,
        color: slice.color,
        dashArray: `${length} ${circumference - length}`,
        dashOffset: `${-offset}`,
      };
      offset += length;
      return segment;
    });
  }

  setHoveredSlice(label: string | null): void {
    this.hoveredLabel = label;
  }

  get hoveredSegment(): DonutSegment | null {
    return this.donutSegments.find(s => s.label === this.hoveredLabel) ?? null;
  }

  formatCompact(value: number): string {
    try {
      return new Intl.NumberFormat(undefined, {
        style: 'currency',
        currency: this.selectedCurrency,
        notation: 'compact',
        maximumFractionDigits: 1
      }).format(value);
    } catch {
      return value.toFixed(0);
    }
  }
}
