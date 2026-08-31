import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { StockWatch } from '../model/stockwatch';
import { ResourceHelper } from '../util/servicehelper';
import { ForexWatch } from '../model/forexwatch';
import { CryptoWatch } from '../model/cryptowatch';
import { WatchGroup } from '../model/watchgroup';
import { environment } from '../../environments/environment';

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

  createNewStockWatch(shortName: string, name: string, exchange: string, groupId?: number | null) {
    const url = groupId == null ? `${this.watchListUrl}/stock` : `${this.watchListUrl}/stock?groupId=${groupId}`;
    let data = {shortName: shortName, name: name, exchange: exchange}
    return this.http.post(url, data,{
      headers: this.helper.getHeadersWithToken()
    });
  };

  setStockWatchGroup(tickerWatchId: number, groupId: number | null) {
    const base = `${this.watchListUrl}/stock/${tickerWatchId}/group`;
    const url = groupId == null ? base : `${base}?groupId=${groupId}`;
    return this.http.put<StockWatch>(url, {}, {
      headers: this.helper.getHeadersWithToken()
    });
  };

  getGroups() {
    const url = `${this.watchListUrl}/group`;
    return this.http.get<WatchGroup[]>(url, {
      headers: this.helper.getHeadersWithToken()
    });
  };

  createGroup(name: string) {
    const url = `${this.watchListUrl}/group`;
    return this.http.post<WatchGroup>(url, JSON.stringify({ name: name }), {
      headers: this.helper.getHeadersWithToken()
    });
  };

  renameGroup(id: number, name: string) {
    const url = `${this.watchListUrl}/group/${id}`;
    return this.http.put<WatchGroup>(url, JSON.stringify({ name: name }), {
      headers: this.helper.getHeadersWithToken()
    });
  };

  deleteGroup(id: number) {
    const url = `${this.watchListUrl}/group/${id}`;
    return this.http.delete(url, {
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
