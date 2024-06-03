import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ResourceHelper } from '../util/servicehelper';
import { environment } from '../../environments/environment';
import { StockAlert } from '../model/stockalert';
import { CryptoAlert } from '../model/cryptoalert';

@Injectable({
  providedIn: 'root'
})
export class AlertService {

  private helper = new ResourceHelper();

  private watchListUrl = `${environment.API_URL}/api/v1/alert`;

  constructor(private http: HttpClient) { }

  getStockAlerts() {
    const url = `${this.watchListUrl}/stock`;
    return this.http.get<StockAlert[]>(url, {
      headers: this.helper.getHeadersWithToken()
    });
  };

  deleteStockAlert(id: number) {
    const url = `${this.watchListUrl}/stock/${id}`;
    return this.http.delete(url, {
      headers: this.helper.getHeadersWithToken()
    });
  };

  createNewStockAlert(data: any) {
    const url = `${this.watchListUrl}/stock`;
    return this.http.post(url, data,{
      headers: this.helper.getHeadersWithToken()
    });
  };

  getCryptoAlerts() {
    const url = `${this.watchListUrl}/crypto`;
    return this.http.get<CryptoAlert[]>(url, {
      headers: this.helper.getHeadersWithToken()
    });
  };

  deleteCryptoAlert(id: number) {
    const url = `${this.watchListUrl}/crypto/${id}`;
    return this.http.delete(url, {
      headers: this.helper.getHeadersWithToken()
    });
  };

  createNewCryptoAlert(data: any) {
    const url = `${this.watchListUrl}/crypto`;
    return this.http.post(url, data,{
      headers: this.helper.getHeadersWithToken()
    });
  };
}
