import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { Exchange } from '../model/exchange';

@Component({
  selector: 'app-exchange-option',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (dense()) {
      <span class="exchange-dense">
        @if (exchange()?.Code) {
          <span class="exchange-code">{{ exchange()?.Code }}</span>
        }
        <span class="exchange-name">{{ exchange()?.Name }}</span>
      </span>
    } @else {
      <span class="exchange-name">{{ exchange()?.Name }}</span>
      <span class="exchange-meta">{{ meta() }}</span>
    }
  `,
  styles: [`
    :host {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      flex: 1 1 auto;
      min-width: 0;
      line-height: 17px;
    }

    .exchange-dense {
      display: flex;
      align-items: center;
      gap: 0.4rem;
      min-width: 0;
    }

    .exchange-name {
      flex: 0 1 auto;
      min-width: 0;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .exchange-code {
      flex: 0 0 auto;
      font-weight: 600;
    }

    .exchange-meta {
      flex: 0 0 auto;
      margin-left: auto;
      color: var(--p-text-muted-color, #888780);
      font-size: 11px;
      font-variant-numeric: tabular-nums;
    }
  `]
})
export class ExchangeOptionComponent {

  readonly exchange = input<Exchange | undefined>(undefined);
  readonly dense = input<boolean>(false);

  protected readonly meta = computed(() =>
    [this.exchange()?.Code, this.exchange()?.Currency].filter(part => !!part).join(' · '));
}
