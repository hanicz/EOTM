import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Investment } from '../model/investment';
import { ResourceHelper } from '../util/servicehelper';
import { environment } from '../../environments/environment';
import { Stock } from '../model/stock';
import { Candle } from '../model/candle';

@Injectable({
  providedIn: 'root'
})
export class StockService {

  private helper = new ResourceHelper();

  private investmentUrl = `${environment.API_URL}/investment`;

  constructor(private http: HttpClient) { }

  getAllStocks() {
    const url = `${environment.API_URL}/stock`;
    return this.http.get<Stock[]>(url, {
      headers: this.helper.getHeadersWithToken()
    });
  }

  getCandleData(shortName: string, month: number) {
    const url = `${environment.API_URL}/stock/candle/${shortName}/${month}`;
    return this.http.get<Candle>(url, {
      headers: this.helper.getHeadersWithToken()
    });
  }

  getHolding(currency: string) {
    var data = { "currency": currency }
    const url = `${this.investmentUrl}/holding`;
    return this.http.post<Investment[]>(url, JSON.stringify(data), {
      headers: this.helper.getHeadersWithToken()
    });
  };

  getPositions(currency: string) {
    var data = { "currency": currency }
    const url = `${this.investmentUrl}/position`;
    return this.http.post<Investment[]>(url, JSON.stringify(data), {
      headers: this.helper.getHeadersWithToken()
    });
  };

  getInvestments() {
    const url = `${this.investmentUrl}`;
    return this.http.get<Investment[]>(url, {
      headers: this.helper.getHeadersWithToken()
    });
  };

  deleteByIds(ids: string) {
    const url = `${this.investmentUrl}?ids=${ids}`;
    return this.http.delete(url, {
      headers: this.helper.getHeadersWithToken()
    });
  }

  download() {
    const url = `${this.investmentUrl}/csv`;
    return this.http.get(url, {
      headers: this.helper.getHeadersWithToken(),
      responseType: 'blob'
    });
  }

  create(investment: Investment) {
    const url = `${this.investmentUrl}`;
    return this.http.post<Investment>(url, JSON.stringify(investment), {
      headers: this.helper.getHeadersWithToken(),
    });
  }

  update(investment: Investment) {
    const url = `${this.investmentUrl}`;
    return this.http.put<Investment>(url, JSON.stringify(investment), {
      headers: this.helper.getHeadersWithToken(),
    });
  }

  uploadCSV(file: File) {
    const formData = new FormData();
    formData.append('file', file, 'file.csv')
    const url = `${this.investmentUrl}/process/csv`;
    return this.http.post<any>(url, formData, {
      headers: this.helper.getAuthHeaders(),
    });
  }
}
