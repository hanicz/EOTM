import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Profile } from '../model/profile';
import { Metric } from '../model/metric';
import { environment } from '../../environments/environment';
import { Recommendation } from '../model/recommendation';

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

  getRecommendations(symbol: string) {
    const url = `${this.newsUrl}/recommendation/${symbol}`;
    return this.http.get<Recommendation[]>(url);
  }
}
