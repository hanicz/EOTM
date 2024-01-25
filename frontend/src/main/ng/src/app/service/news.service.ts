import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { News } from '../model/news';
import { ResourceHelper } from '../util/servicehelper';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class NewsService {

  private helper = new ResourceHelper();

  private newsUrl = `${environment.API_URL}/api/v1/news`;

  constructor(private http: HttpClient) { }

  getNews(category: string) {
    const url = `${this.newsUrl}/${category}`;
    return this.http.get<News[]>(url, {
      headers: this.helper.getHeadersWithToken()
    });
  };

  getCompanyNews(symbol: string) {
    const url = `${this.newsUrl}/company/${symbol}`;
    return this.http.get<News[]>(url, {
      headers: this.helper.getHeadersWithToken()
    });
  };
}
