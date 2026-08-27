import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { finalize, shareReplay } from 'rxjs/operators';
import { ResourceHelper } from '../util/servicehelper';
import { environment } from '../../environments/environment';
import { MarketExchange } from '../model/market';

@Injectable({
  providedIn: 'root'
})
export class MarketService {

  private helper = new ResourceHelper();

  private marketUrl = `${environment.API_URL}/api/v1/market`;

  private exchangesRequest$: Observable<MarketExchange[]> | null = null;

  constructor(private http: HttpClient) { }

  getExchanges() {
    if (!this.exchangesRequest$) {
      this.exchangesRequest$ = this.http.get<MarketExchange[]>(this.marketUrl, {
        headers: this.helper.getHeadersWithToken()
      }).pipe(
        shareReplay(1),
        finalize(() => this.exchangesRequest$ = null)
      );
    }
    return this.exchangesRequest$;
  };
}
