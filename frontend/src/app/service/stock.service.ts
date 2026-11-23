import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Investment } from '../model/investment';
import { environment } from '../../environments/environment';
import { Stock } from '../model/stock';
import { Candle } from '../model/candle';
import { Symbol } from '../model/symbol';
import { Exchange } from '../model/exchange';
import { SignalResult } from '../model/signal';

@Injectable({
  providedIn: 'root'
})
export class StockService {

  private investmentUrl = `${environment.API_URL}/api/v1/investment`;

  constructor(private http: HttpClient) { }

  getAllStocks() {
    const url = `${environment.API_URL}/api/v1/stock`;
    return this.http.get<Stock[]>(url);
  };

  getAllSymbols(exchange: string) {
    const url = `${environment.API_URL}/api/v1/stock/symbols/${exchange}`;
    return this.http.get<Symbol[]>(url);
  };

  getAllExchanges() {
    const url = `${environment.API_URL}/api/v1/stock/exchanges`;
    return this.http.get<Exchange[]>(url);
  };

  getCandleData(shortName: string, exchange: string, month: number) {
    const url = `${environment.API_URL}/api/v1/stock/candle/${shortName}.${exchange}/${month}`;
    return this.http.get<Candle>(url);
  };

  getSignal(shortName: string, exchange: string) {
    const url = `${environment.API_URL}/api/v1/stock/${shortName}.${exchange}/signal`;
    return this.http.get<SignalResult>(url);
  };

  getHolding(refresh = false) {
    const url = `${this.investmentUrl}/holding${refresh ? '?refresh=true' : ''}`;
    return this.http.get<Investment[]>(url);
  };

  getPositions() {
    const url = `${this.investmentUrl}/position`;
    return this.http.get<Investment[]>(url);
  };

  getInvestments() {
    const url = `${this.investmentUrl}`;
    return this.http.get<Investment[]>(url);
  };

  deleteByIds(ids: string) {
    const url = `${this.investmentUrl}?ids=${ids}`;
    return this.http.delete(url);
  }

  setRSU(ids: string, rsu: boolean) {
    const url = `${this.investmentUrl}/rsu?ids=${ids}&rsu=${rsu}`;
    return this.http.put(url, null);
  }

  download() {
    const url = `${this.investmentUrl}/csv`;
    return this.http.get(url, {
      responseType: 'blob'
    });
  }

  create(investment: Investment) {
    const url = `${this.investmentUrl}`;
    return this.http.post<Investment>(url, JSON.stringify(investment));
  }

  update(investment: Investment) {
    const url = `${this.investmentUrl}`;
    return this.http.put<Investment>(url, JSON.stringify(investment));
  }

  uploadCSV(file: File) {
    const formData = new FormData();
    formData.append('file', file, 'file.csv')
    const url = `${this.investmentUrl}/process/csv`;
    return this.http.post<any>(url, formData);
  }
}
