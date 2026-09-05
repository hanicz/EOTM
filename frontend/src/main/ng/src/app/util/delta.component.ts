import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { CurrencyPipe, DecimalPipe, NgClass } from '@angular/common';

@Component({
  selector: 'app-delta',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CurrencyPipe, DecimalPipe, NgClass],
  template: `
    @if (value() == null) {
      <span class="cell-none">&mdash;</span>
    } @else {
      <span class="delta-cell">
        <span class="delta-amount" [ngClass]="direction()">
          {{ value() | currency : currency() : 'symbol' : digits() }}
        </span>
        @if (percent() != null) {
          <span class="delta-pill" [ngClass]="direction()">
            <i class="pi delta-caret" [ngClass]="caret()"></i>{{ percent() | number : '0.0-1' }}%
          </span>
        }
      </span>
    }
  `,
  styles: [`
    :host {
      display: block;
    }

    .delta-cell {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      gap: 0.1rem;
      line-height: 1.25;
    }

    .delta-amount {
      font-weight: 600;
      font-variant-numeric: tabular-nums;
      white-space: nowrap;
    }

    .delta-pill {
      font-size: 0.7rem;
    }
  `]
})
export class DeltaComponent {

  readonly value = input<number | null>(null);
  readonly percent = input<number | null>(null);
  readonly currency = input<string>('');
  readonly digits = input<string>('0.0-2');

  readonly direction = computed(() => {
    const value = this.value() ?? 0;
    if (value > 0) return 'delta-up';
    return (value < 0) ? 'delta-down' : 'delta-flat';
  });

  readonly caret = computed(() => {
    const value = this.value() ?? 0;
    if (value > 0) return 'pi-caret-up';
    return (value < 0) ? 'pi-caret-down' : 'pi-minus';
  });
}
