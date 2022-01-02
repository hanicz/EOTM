import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { StockWatch } from '../model/stockwatch';
import { ResourceHelper } from '../util/servicehelper';
import { ForexWatch } from '../model/forexwatch';
import { CryptoWatch } from '../model/cryptowatch';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class WatchlistService {

  private helper = new ResourceHelper();

  private watchListUrl = `${environment.API_URL}/watchlist`;

  constructor(private http: HttpClient) { }

  getStockWatchList() {
    const url = `${this.watchListUrl}/stock`;
    return this.http.get<StockWatch[]>(url, {
      headers: this.helper.getHeadersWithToken()
    });
  };

  getForexWatchList() {
    const url = `${this.watchListUrl}/forex`;
    return this.http.get<ForexWatch[]>(url, {
      headers: this.helper.getHeadersWithToken()
    });
  };

  getCryptoWatchList(currency: string) {
    const url = `${this.watchListUrl}/crypto/${currency}`;
    return this.http.get<CryptoWatch[]>(url, {
      headers: this.helper.getHeadersWithToken()
    });
  };
}
