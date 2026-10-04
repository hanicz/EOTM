import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Profile } from '../model/profile';
import { Metric } from '../model/metric';
import { environment } from '../../environments/environment';
import { Recommendation } from '../model/recommendation';
import { EtfProfile } from '../model/etf-profile';

@Injectable({
  providedIn: 'root'
})
export class MetricService {

  private newsUrl = `${environment.API_URL}/api/v1/metric`;

  constructor(private http: HttpClient) { }

  getProfile(symbol: string) {
    const url = `${this.newsUrl}/profile/${symbol}`;
    return this.http.get<Profile>(url);
  };

  getMetrics(symbol: string) {
    const url = `${this.newsUrl}/metric/${symbol}`;
    return this.http.get<Metric>(url);
  };

  getEtfProfile(isin: string) {
    const url = `${this.newsUrl}/etf/${encodeURIComponent(isin)}`;
    return this.http.get<EtfProfile>(url);
  }

  getRecommendations(symbol: string) {
    const url = `${this.newsUrl}/recommendation/${symbol}`;
    return this.http.get<Recommendation[]>(url);
  }
}
