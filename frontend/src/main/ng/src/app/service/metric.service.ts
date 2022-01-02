import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Profile } from '../model/profile';
import { Metric } from '../model/metric';
import { ResourceHelper } from '../util/servicehelper';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class MetricService {

  private helper = new ResourceHelper();

  private newsUrl = `${environment.API_URL}/metric`;

  constructor(private http: HttpClient) { }

  getProfile(symbol: string) {
    const url = `${this.newsUrl}/profile/${symbol}`;
    return this.http.get<Profile>(url, {
      headers: this.helper.getHeadersWithToken()
    });
  };

  getMetrics(symbol: string) {
    const url = `${this.newsUrl}/metric/${symbol}`;
    return this.http.get<Metric>(url, {
      headers: this.helper.getHeadersWithToken()
    });
  };
}
