import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { NetWorth, NetWorthHistory } from '../model/networth';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class NetWorthService {

  private netWorthUrl = `${environment.API_URL}/api/v1/networth`;

  constructor(private http: HttpClient) { }

  getNetWorth(currency: string, refresh = false) {
    return this.http.get<NetWorth>(`${this.netWorthUrl}?currency=${encodeURIComponent(currency)}${refresh ? '&refresh=true' : ''}`);
  }

  getHistory(refresh = false) {
    return this.http.get<NetWorthHistory>(`${this.netWorthUrl}/history${refresh ? '?refresh=true' : ''}`);
  }
}
