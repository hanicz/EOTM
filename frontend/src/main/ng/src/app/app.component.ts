import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { PrimeNGConfig } from 'primeng/api';
import { NavigationStart } from '@angular/router';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  router: Router;
  title = 'Eye OTM';
  routeUrl = "";

  constructor(private primengConfig: PrimeNGConfig, router: Router) {
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

  ngOnInit() {
    this.primengConfig.ripple = true;
  }
}
