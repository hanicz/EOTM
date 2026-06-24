import { Component } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { NavigationStart } from '@angular/router';
import { WatchlistComponent } from './watchlist/watchlist.component';

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.css'],
    imports: [RouterOutlet, WatchlistComponent]
})
export class AppComponent {
  router: Router;
  title = 'Eye OTM';
  routeUrl = "";

  constructor(router: Router) {
    this.router = router;
    this.router.events.subscribe(routerEvent => {
      if (routerEvent instanceof NavigationStart) {
        if (this.routeUrl != routerEvent.url) {
          this.routeUrl = routerEvent.url;
          this.router.navigateByUrl(routerEvent.url, { skipLocationChange: true });
        }
      }
    });
  }
}
