import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { StockWatch } from '../model/stockwatch';
import { ForexWatch } from '../model/forexwatch';
import { CryptoWatch } from '../model/cryptowatch';
import { WatchGroup } from '../model/watchgroup';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class WatchlistService {

  private watchListUrl = `${environment.API_URL}/api/v1/watchlist`;

  constructor(private http: HttpClient) { }

  getStockWatchList() {
    const url = `${this.watchListUrl}/stock`;
    return this.http.get<StockWatch[]>(url);
  };

  getForexWatchList() {
    const url = `${this.watchListUrl}/forex`;
    return this.http.get<ForexWatch[]>(url);
  };

  getCryptoWatchList(currency: string) {
    const url = `${this.watchListUrl}/crypto/${currency}`;
    return this.http.get<CryptoWatch[]>(url);
  };

  deleteWatch(path: string) {
    const url = `${this.watchListUrl}${path}`;
    return this.http.delete(url);
  };

  createWatch(path: string) {
    const url = `${this.watchListUrl}${path}`;
    return this.http.post(url, {});
  };

  createNewStockWatch(shortName: string, name: string, exchange: string, groupId?: number | null) {
    const url = groupId == null ? `${this.watchListUrl}/stock` : `${this.watchListUrl}/stock?groupId=${groupId}`;
    let data = {shortName: shortName, name: name, exchange: exchange}
    return this.http.post(url, data);
  };

  setStockWatchGroup(tickerWatchId: number, groupId: number | null) {
    const base = `${this.watchListUrl}/stock/${tickerWatchId}/group`;
    const url = groupId == null ? base : `${base}?groupId=${groupId}`;
    return this.http.put<StockWatch>(url, {});
  };

  getGroups() {
    const url = `${this.watchListUrl}/group`;
    return this.http.get<WatchGroup[]>(url);
  };

  createGroup(name: string) {
    const url = `${this.watchListUrl}/group`;
    return this.http.post<WatchGroup>(url, JSON.stringify({ name: name }));
  };

  renameGroup(id: number, name: string) {
    const url = `${this.watchListUrl}/group/${id}`;
    return this.http.put<WatchGroup>(url, JSON.stringify({ name: name }));
  };

  deleteGroup(id: number) {
    const url = `${this.watchListUrl}/group/${id}`;
    return this.http.delete(url);
  };

  createNewForexWatch(from: string, to: string) {
    const url = `${this.watchListUrl}/forex/${from}/${to}`;
    return this.http.post(url, {});
  };
}
