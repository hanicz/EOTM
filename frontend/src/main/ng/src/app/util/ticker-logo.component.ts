import { ChangeDetectionStrategy, Component, computed, input, linkedSignal } from '@angular/core';

@Component({
  selector: 'app-ticker-logo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (source(); as url) {
      <img class="ticker-logo-image" alt="" loading="lazy" [src]="url" [style.width.px]="size()"
        [style.height.px]="size()" [style.border-radius.px]="radius()" (error)="onError()">
    } @else {
      <span class="ticker-logo-initials" aria-hidden="true" [style.width.px]="size()"
        [style.height.px]="size()" [style.border-radius.px]="radius()" [style.font-size.px]="fontSize()"
        [style.background-color]="background()">{{ initials() }}</span>
    }
  `,
  styles: [`
    :host {
      display: inline-flex;
      align-items: center;
      flex: 0 0 auto;
    }

    .ticker-logo-image {
      object-fit: contain;
    }

    .ticker-logo-initials {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      color: #ffffff;
      font-weight: 700;
      line-height: 1;
      letter-spacing: -0.03em;
      text-transform: uppercase;
      user-select: none;
    }
  `]
})
export class TickerLogoComponent {

  private static readonly HUES = [4, 24, 42, 90, 150, 176, 200, 220, 260, 290, 320, 340];

  readonly symbol = input<string>('');
  readonly exchange = input<string>('');
  readonly src = input<string>('');
  readonly size = input<number>(15);

  private readonly attempt = linkedSignal<string, number>({
    source: () => this.src() + '|' + this.exchange() + '/' + this.symbol(),
    computation: () => 0
  });

  private readonly cleanSymbol = computed(() => (this.symbol() ?? '').trim());
  private readonly cleanExchange = computed(() => (this.exchange() ?? '').trim());

  protected readonly source = computed<string | null>(() => {
    const attempt = this.attempt();
    const direct = (this.src() ?? '').trim();
    if (direct) {
      return (attempt === 0) ? direct : null;
    }
    const symbol = this.cleanSymbol();
    const exchange = this.cleanExchange();
    if (!symbol || !exchange) {
      return null;
    }
    const base = 'https://eodhd.com/img/logos/' + exchange.toUpperCase() + '/';
    if (attempt === 0) {
      return base + symbol.toUpperCase() + '.png';
    }
    if (attempt === 1 && symbol.toLowerCase() !== symbol.toUpperCase()) {
      return base + symbol.toLowerCase() + '.png';
    }
    return null;
  });

  protected readonly radius = computed(() => Math.max(2, Math.round(this.size() * 0.18)));

  protected readonly initials = computed(() => {
    const letters = this.cleanSymbol().replace(/[^a-zA-Z0-9]/g, '');
    return letters.slice(0, this.size() >= 28 ? 2 : 1) || '?';
  });

  protected readonly fontSize = computed(() =>
    Math.max(8, Math.round(this.size() * (this.initials().length > 1 ? 0.42 : 0.56))));

  protected readonly background = computed(() => {
    const key = this.cleanSymbol().toUpperCase() || '?';
    let hash = 0;
    for (let i = 0; i < key.length; i++) {
      hash = (hash * 31 + key.charCodeAt(i)) % 100000;
    }
    return 'hsl(' + TickerLogoComponent.HUES[hash % TickerLogoComponent.HUES.length] + ' 42% 46%)';
  });

  protected onError(): void {
    this.attempt.update(value => value + 1);
  }
}
