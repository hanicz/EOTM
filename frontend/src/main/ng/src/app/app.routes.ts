import { Routes } from '@angular/router';
import { authGuard } from './util/auth.guard';
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
import { SettingsComponent } from './settings/settings.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { SecurityComponent } from './security/security.component';
import { TaxComponent } from './tax/tax.component';
import { FireComponent } from './fire/fire.component';
import { FinancialComponent } from './financial/financial.component';
import { CashComponent } from './cash/cash.component';
import { PensionComponent } from './pension/pension.component';
import { SalaryComponent } from './salary/salary.component';
import { EquityComponent } from './equity/equity.component';

export const routes: Routes = [
  { path: '', component: LandingComponent },
  { path: 'login', component: LoginComponent },
  { path: 'dashboard', component: DashboardComponent, canActivate: [authGuard] },
  { path: 'news', component: HomeComponent, canActivate: [authGuard] },
  { path: 'stock', component: StockComponent, canActivate: [authGuard] },
  { path: 'crypto', component: CryptoComponent, canActivate: [authGuard] },
  { path: 'watchlist', component: WatchlistComponent, canActivate: [authGuard] },
  { path: 'search', component: SearchComponent, canActivate: [authGuard] },
  { path: 'forex', component: ForexComponent, canActivate: [authGuard] },
  { path: 'security', component: SecurityComponent, canActivate: [authGuard] },
  { path: 'etf', component: EtfComponent, canActivate: [authGuard] },
  { path: 'cash', component: CashComponent, canActivate: [authGuard] },
    { path: 'pension', component: PensionComponent, canActivate: [authGuard] },
  { path: 'alert', component: AlertComponent, canActivate: [authGuard] },
  { path: 'tax', component: TaxComponent, canActivate: [authGuard] },
  { path: 'fire', component: FireComponent, canActivate: [authGuard] },
  { path: 'financial', component: FinancialComponent, canActivate: [authGuard] },
  { path: 'salary', component: SalaryComponent, canActivate: [authGuard] },
  { path: 'equity', component: EquityComponent, canActivate: [authGuard] },
  { path: 'settings', component: SettingsComponent, canActivate: [authGuard] }
];
