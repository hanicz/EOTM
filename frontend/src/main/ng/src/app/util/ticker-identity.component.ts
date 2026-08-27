import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TickerLogoComponent } from './ticker-logo.component';

@Component({
  selector: 'app-ticker-identity',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TickerLogoComponent],
  template: `
    <span class="ticker-identity">
      <span class="ticker-identity-head">
        <app-ticker-logo [exchange]="exchange()" [symbol]="symbol()"></app-ticker-logo>
        <span class="ticker-identity-symbol">{{ symbol() }}.{{ exchange() }}</span>
      </span>
      @if (name()) {
        <span class="ticker-identity-name">{{ name() }}</span>
      }
    </span>
  `,
  styles: [`
    :host {
      display: block;
      min-width: 0;
    }

    .ticker-identity {
      display: flex;
      flex-direction: column;
      min-width: 0;
      line-height: 1.25;
    }

    .ticker-identity-head {
      display: flex;
      align-items: center;
      gap: 0.35rem;
      min-width: 0;
    }

    .ticker-identity-symbol {
      font-size: 0.8rem;
      font-weight: 600;
      color: #1b1b1b;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .ticker-identity-name {
      min-width: 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      color: var(--p-text-muted-color, #5f5e5a);
      font-size: 0.7rem;
    }
  `]
})
export class TickerIdentityComponent {

  readonly symbol = input<string>('');
  readonly exchange = input<string>('');
  readonly name = input<string>('');
}
