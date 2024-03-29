import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { StockWatch } from '../model/stockwatch';
import { ResourceHelper } from '../util/servicehelper';
import { ForexWatch } from '../model/forexwatch';
import { CryptoWatch } from '../model/cryptowatch';
import { environment } from '../../environments/environment';
import { Symbol } from '../model/symbol';
import { Exchange } from '../model/exchange';

@Injectable({
  providedIn: 'root'
})
export class WatchlistService {

  private helper = new ResourceHelper();

  private watchListUrl = `${environment.API_URL}/api/v1/watchlist`;

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

  deleteWatch(path: string) {
    const url = `${this.watchListUrl}${path}`;
    return this.http.delete(url, {
      headers: this.helper.getHeadersWithToken()
    });
  };

  createWatch(path: string) {
    const url = `${this.watchListUrl}${path}`;
    return this.http.post(url, {},{
      headers: this.helper.getHeadersWithToken()
    });
  };

  createNewStockWatch(symbol: Symbol, exchange: Exchange) {
    const url = `${this.watchListUrl}/stock`;
    let data = {shortName: symbol.Code, name: symbol.Name, exchange: exchange.Code}
    return this.http.post(url, data,{
      headers: this.helper.getHeadersWithToken()
    });
  };

  createNewForexWatch(from: string, to: string) {
    const url = `${this.watchListUrl}/forex/${from}/${to}`;
    return this.http.post(url, {},{
      headers: this.helper.getHeadersWithToken()
    });
  };
}
