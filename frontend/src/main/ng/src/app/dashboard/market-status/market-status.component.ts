import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, computed, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Skeleton } from 'primeng/skeleton';
import { MarketService } from '../../service/market.service';
import { MarketExchange } from '../../model/market';
import { localTimeLabel, resolveMarketStatus, upcomingHolidays } from '../../util/marketstatus';

const REFRESH_INTERVAL_MS = 30_000;
const UPCOMING_LIMIT = 3;

@Component({
  selector: 'app-market-status',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, Skeleton],
  templateUrl: './market-status.component.html',
  styleUrls: ['./market-status.component.css']
})
export class MarketStatusComponent implements OnInit, OnDestroy {

  readonly loading = signal(true);
  readonly skeletonRows = new Array(4).fill({});

  private readonly exchanges = signal<MarketExchange[]>([]);
  private readonly now = signal(new Date());
  private timer?: ReturnType<typeof setInterval>;

  readonly rows = computed(() => this.exchanges().map(exchange => ({
    code: exchange.code,
    name: exchange.name,
    hours: `${exchange.openTime}\u2013${exchange.closeTime}`,
    localTime: localTimeLabel(exchange.timeZone, this.now()),
    status: resolveMarketStatus(exchange, this.now())
  })));

  readonly upcoming = computed(() => upcomingHolidays(this.exchanges(), UPCOMING_LIMIT));

  constructor(private marketService: MarketService) { }

  ngOnInit(): void {
    this.marketService.getExchanges().subscribe({
      next: data => {
        this.exchanges.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
    this.timer = setInterval(() => this.now.set(new Date()), REFRESH_INTERVAL_MS);
  }

  ngOnDestroy(): void {
    if (this.timer) clearInterval(this.timer);
  }
}
