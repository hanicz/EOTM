import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { HomeComponent } from './home/home.component';
import { StockComponent } from './stock/stock.component';
import { CryptoComponent } from './crypto/crypto.component';
import { WatchlistComponent } from './watchlist/watchlist.component';
import { SearchComponent } from './search/search.component';
import { ForexComponent } from './forex/forex.component';
import { LandingComponent } from './landing/landing.component';
import { EtfComponent } from './etf/etf.component';
import { AlertComponent } from './alert/alert.component';

const routes: Routes = [
  { path: '', component: LandingComponent },
  { path: 'login', component: LoginComponent },
  { path: 'home', component: HomeComponent },
  { path: 'stock', component: StockComponent },
  { path: 'crypto', component: CryptoComponent },
  { path: 'watchlist', component: WatchlistComponent },
  { path: 'search', component: SearchComponent },
  { path: 'forex', component: ForexComponent },
  { path: 'etf', component: EtfComponent },
  { path: 'alert', component: AlertComponent },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
