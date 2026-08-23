import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { NetWorth } from '../model/networth';
import { ResourceHelper } from '../util/servicehelper';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class NetWorthService {

  private helper = new ResourceHelper();

  private netWorthUrl = `${environment.API_URL}/api/v1/networth`;

  constructor(private http: HttpClient) { }

  getNetWorth(currency: string, refresh = false) {
    return this.http.get<NetWorth>(`${this.netWorthUrl}?currency=${encodeURIComponent(currency)}${refresh ? '&refresh=true' : ''}`, {
      headers: this.helper.getHeadersWithToken()
    });
  }
}
