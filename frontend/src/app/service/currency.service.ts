import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { shareReplay, tap } from 'rxjs/operators';
import { Currency } from '../model/currency';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class CurrencyService {

  private currencyUrl = `${environment.API_URL}/api/v1/currency`;

  private cachedCurrencies$?: Observable<Currency[]>;

  constructor(private http: HttpClient) { }

  getCurrencies(): Observable<Currency[]> {
    if (!this.cachedCurrencies$) {
      this.cachedCurrencies$ = this.http.get<Currency[]>(this.currencyUrl).pipe(
        tap({ error: () => this.cachedCurrencies$ = undefined }),
        shareReplay(1)
      );
    }
    return this.cachedCurrencies$;
  }
}
