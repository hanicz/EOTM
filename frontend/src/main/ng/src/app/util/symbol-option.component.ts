import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { Symbol } from '../model/symbol';

@Component({
  selector: 'app-symbol-option',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (dense()) {
      <span class="symbol-dense">
        @if (symbol()?.Code) {
          <span class="symbol-code">{{ symbol()?.Code }}</span>
        }
        <span class="symbol-name">{{ symbol()?.Name }}</span>
      </span>
    } @else {
      <span class="symbol-name">{{ symbol()?.Name }}</span>
      <span class="symbol-meta">{{ meta() }}</span>
    }
  `,
  styles: [`
    :host {
      display: flex;
      flex-direction: column;
      justify-content: center;
      flex: 1 1 auto;
      min-width: 0;
    }

    .symbol-dense {
      display: flex;
      align-items: center;
      gap: 0.4rem;
      min-width: 0;
      line-height: 17px;
    }

    .symbol-name {
      min-width: 0;
      overflow: hidden;
      text-overflow: ellipsis;
      line-height: 17px;
    }

    .symbol-code {
      flex: 0 0 auto;
      font-weight: 600;
    }

    .symbol-meta {
      min-width: 0;
      overflow: hidden;
      text-overflow: ellipsis;
      color: var(--p-text-muted-color, #888780);
      font-size: 11px;
      line-height: 15px;
      font-variant-numeric: tabular-nums;
    }
  `]
})
export class SymbolOptionComponent {

  readonly symbol = input<Symbol | undefined>(undefined);
  readonly dense = input<boolean>(false);

  protected readonly meta = computed(() =>
    [this.symbol()?.Code, this.symbol()?.Isin, this.symbol()?.Type]
      .filter(part => !!part).join(' · '));
}
