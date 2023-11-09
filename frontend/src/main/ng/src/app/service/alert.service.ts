import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ResourceHelper } from '../util/servicehelper';
import { environment } from '../../environments/environment';
import { StockAlert } from '../model/stockalert';
import { Symbol } from '../model/symbol';
import { Exchange } from '../model/exchange';

@Injectable({
  providedIn: 'root'
})
export class AlertService {

  private helper = new ResourceHelper();

  private watchListUrl = `${environment.API_URL}/alert`;

  constructor(private http: HttpClient) { }

  getAlerts() {
    const url = `${this.watchListUrl}`;
    return this.http.get<StockAlert[]>(url, {
      headers: this.helper.getHeadersWithToken()
    });
  };

  deleteStockAlert(id: number) {
    const url = `${this.watchListUrl}/${id}`;
    return this.http.delete(url, {
      headers: this.helper.getHeadersWithToken()
    });
  };

  createNewStockAlert(symbol: Symbol, exchange: Exchange) {
    const url = `${this.watchListUrl}`;
    let data = {shortName: symbol.Code, name: symbol.Name, exchange: exchange.Code}
    return this.http.post(url, data,{
      headers: this.helper.getHeadersWithToken()
    });
  };
}
