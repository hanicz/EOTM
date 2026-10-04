import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { Skeleton } from 'primeng/skeleton';
import { Tooltip } from 'primeng/tooltip';
import { EtfWeight } from '../model/etf-profile';

@Component({
  selector: 'app-weight-list',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DecimalPipe, Skeleton, Tooltip],
  template: `
    @if (loading()) {
      @for (row of placeholders(); track $index) {
        <div class="weight-row" [class.weight-row-compact]="compact()">
          <p-skeleton width="8rem" height="0.8rem"></p-skeleton>
          <p-skeleton width="100%" height="0.4rem"></p-skeleton>
          <p-skeleton width="3rem" height="0.8rem"></p-skeleton>
        </div>
      }
    } @else {
      @for (item of rows(); track $index; let i = $index) {
        <div class="weight-row" [class.weight-row-compact]="compact()">
          <div class="weight-name">
            @if (ranked()) {
              <span class="weight-rank">{{ i + 1 }}</span>
            }
            <span class="weight-label" [pTooltip]="item.isin ?? ''" tooltipPosition="top">{{ item.name }}</span>
          </div>
          <div class="weight-track">
            <div class="weight-fill" [class.weight-fill-muted]="compact()" [style.width.%]="item.bar"></div>
          </div>
          <div class="weight-value">{{ item.weight != null ? (item.weight | number : '1.2-2') + '%' : '–' }}</div>
        </div>
      }
    }
  `,
  styles: [`
    :host {
      display: block;
    }

    .weight-row {
      display: grid;
      grid-template-columns: minmax(0, 2fr) minmax(0, 3fr) 4.5rem;
      align-items: center;
      gap: 10px;
      padding: 5px 0;
      border-bottom: 1px solid var(--p-content-border-color);
      font-size: 12px;
    }

    .weight-row:last-child {
      border-bottom: none;
    }

    .weight-row-compact {
      grid-template-columns: minmax(0, 2fr) minmax(0, 2fr) 4rem;
      padding: 3px 0;
    }

    .weight-name {
      display: flex;
      align-items: center;
      gap: 8px;
      min-width: 0;
    }

    .weight-rank {
      flex: 0 0 1.4rem;
      height: 1.4rem;
      border-radius: 999px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      font-size: 10px;
      font-weight: 700;
      background: var(--eotm-cream);
      color: var(--eotm-ink);
    }

    .weight-label {
      color: var(--eotm-ink);
      font-weight: 500;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .weight-track {
      height: 6px;
      border-radius: 999px;
      background: var(--eotm-cream);
      overflow: hidden;
    }

    .weight-fill {
      height: 100%;
      border-radius: 999px;
      background: var(--p-primary-color);
    }

    .weight-fill-muted {
      opacity: 0.6;
    }

    .weight-value {
      text-align: right;
      font-variant-numeric: tabular-nums;
      font-weight: 600;
      color: var(--eotm-ink);
    }

    @media screen and (max-width: 575px) {
      .weight-row {
        grid-template-columns: minmax(0, 1fr) 4.5rem;
      }

      .weight-track {
        grid-column: 1 / -1;
        grid-row: 2;
      }
    }
  `]
})
export class WeightListComponent {

  readonly items = input<EtfWeight[]>([]);
  readonly ranked = input<boolean>(false);
  readonly compact = input<boolean>(false);
  readonly loading = input<boolean>(false);
  readonly placeholderCount = input<number>(5);

  protected readonly placeholders = computed(() => Array(this.placeholderCount()).fill(0));

  protected readonly rows = computed(() => {
    const items = this.items() ?? [];
    const max = Math.max(0, ...items.map(item => item.weight ?? 0));
    return items.map(item => ({ ...item, bar: max > 0 && item.weight ? (item.weight / max) * 100 : 0 }));
  });
}
