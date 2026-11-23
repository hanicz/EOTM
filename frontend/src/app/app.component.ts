import { Component } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { NavigationStart } from '@angular/router';
import { filter } from 'rxjs/operators';
import { WatchlistComponent } from './watchlist/watchlist.component';
import { Globals } from './util/global';

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.css'],
    imports: [RouterOutlet, WatchlistComponent]
})
export class AppComponent {
  router: Router;
  title = 'Eye OTM';
  watchlistOpen = false;

  constructor(router: Router, private globals: Globals) {
    this.router = router;
    this.router.events
      .pipe(filter(event => event instanceof NavigationStart))
      .subscribe(() => this.setWatchlistOpen(false));

    this.globals.watchlistToggleEvent.subscribe(() => this.setWatchlistOpen(!this.watchlistOpen));
  }

  get showWatchlist(): boolean {
    return !(this.router.url == '/login' || this.router.url == '/');
  }

  closeWatchlist(): void {
    this.setWatchlistOpen(false);
  }

  private setWatchlistOpen(open: boolean): void {
    this.watchlistOpen = open;
    // Only bites below lg, where the watchlist is an overlay; above that the
    // class is set but harmless because the page never opens the panel.
    document.body.classList.toggle('no-scroll', open);
  }
}
