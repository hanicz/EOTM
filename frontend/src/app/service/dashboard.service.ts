import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

export interface DashboardRates {
  rates: { [currency: string]: number };
}

@Injectable({
  providedIn: 'root'
})
export class DashboardService {

  private dashboardUrl = `${environment.API_URL}/api/v1/dashboard`;

  constructor(private http: HttpClient) { }

  getRates(currencies: string[], refresh = false) {
    const params = currencies.map(c => `currencies=${encodeURIComponent(c)}`).join('&');
    const query = [params, refresh ? 'refresh=true' : ''].filter(p => p).join('&');
    const url = `${this.dashboardUrl}/rates${query ? '?' + query : ''}`;
    return this.http.get<DashboardRates>(url);
  }
}
