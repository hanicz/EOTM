import { ChangeDetectionStrategy, Component, OnInit, computed, signal } from '@angular/core';
import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { switchMap } from 'rxjs/operators';
import { Skeleton } from 'primeng/skeleton';
import { Tooltip } from 'primeng/tooltip';
import { FireService } from '../../service/fire.service';
import { UserService } from '../../service/user.service';
import { FireSnapshot } from '../../model/fire';
import { DEFAULT_CURRENCY } from '../../model/currency';

const FULL_WINDOW_MONTHS = 3;
const SKELETON_STATS = 3;

@Component({
  selector: 'app-fire-summary',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CurrencyPipe, DecimalPipe, Skeleton, Tooltip],
  templateUrl: './fire-summary.component.html',
  styleUrls: ['./fire-summary.component.css']
})
export class FireSummaryComponent implements OnInit {

  readonly loading = signal(true);
  readonly skeletonStats = new Array(SKELETON_STATS).fill({});
  readonly currency = signal(DEFAULT_CURRENCY);
  readonly snapshot = signal<FireSnapshot | null>(null);

  readonly barWidth = computed(() => Math.max(0, Math.min(100, this.snapshot()?.progressPct ?? 0)));

  readonly yearsLabel = computed(() => {
    const snapshot = this.snapshot();
    if (!snapshot || snapshot.fireNumber == null) return '—';
    if (snapshot.fiReached) return 'Reached';
    if (snapshot.yearsToFire != null) return `${snapshot.yearsToFire} yrs`;
    return snapshot.hasCashFlow ? 'Not on track' : '—';
  });

  readonly notes = computed(() => {
    const snapshot = this.snapshot();
    if (!snapshot) return [];

    const notes: string[] = [];
    if (snapshot.hasCashFlow && snapshot.monthsCounted < FULL_WINDOW_MONTHS) {
      notes.push(`Based on ${snapshot.monthsCounted} month${snapshot.monthsCounted === 1 ? '' : 's'}`);
    }
    if (snapshot.hasCashFlow && snapshot.monthlySavings < 0) {
      notes.push('Spending more than you earn');
    }
    if (snapshot.targetSource === 'DERIVED') {
      notes.push('Target derived from spending');
    }

    const skipped = [...snapshot.ignoredCurrencies, ...snapshot.unconvertedCurrencies];
    if (skipped.length > 0) {
      notes.push(`Excludes ${skipped.join(', ')}`);
    }
    return notes;
  });

  constructor(private fireService: FireService, private userService: UserService) { }

  ngOnInit(): void {
    this.userService.getPreferredCurrency().pipe(
      switchMap(currency => {
        this.currency.set(currency);
        return this.fireService.getSnapshot(currency);
      })
    ).subscribe({
      next: data => {
        this.snapshot.set(data);
        this.loading.set(false);
      },
      error: error => {
        console.log(error);
        this.loading.set(false);
      }
    });
  }
}
