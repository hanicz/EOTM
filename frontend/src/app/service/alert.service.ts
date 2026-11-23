import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { StockAlert } from '../model/stockalert';
import { CryptoAlert } from '../model/cryptoalert';

@Injectable({
  providedIn: 'root'
})
export class AlertService {

  private watchListUrl = `${environment.API_URL}/api/v1/alert`;

  constructor(private http: HttpClient) { }

  getStockAlerts() {
    const url = `${this.watchListUrl}/stock`;
    return this.http.get<StockAlert[]>(url);
  };

  deleteStockAlert(id: number) {
    const url = `${this.watchListUrl}/stock/${id}`;
    return this.http.delete(url);
  };

  createNewStockAlert(data: any) {
    const url = `${this.watchListUrl}/stock`;
    return this.http.post(url, data);
  };

  getCryptoAlerts() {
    const url = `${this.watchListUrl}/crypto`;
    return this.http.get<CryptoAlert[]>(url);
  };

  deleteCryptoAlert(id: number) {
    const url = `${this.watchListUrl}/crypto/${id}`;
    return this.http.delete(url);
  };

  createNewCryptoAlert(data: any) {
    const url = `${this.watchListUrl}/crypto`;
    return this.http.post(url, data);
  };
}
