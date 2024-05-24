import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { News } from '../model/news';
import { ResourceHelper } from '../util/servicehelper';
import { environment } from '../../environments/environment';
import { Subreddit } from '../model/subreddit';

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

  getSubreddits() {
    const url = `${this.newsUrl}/reddit`;
    return this.http.get<Subreddit[]>(url, {
      headers: this.helper.getHeadersWithToken()
    });
  }

  deleteSubReddit(id: number) {
    const url = `${this.newsUrl}/reddit/${id}`;
    return this.http.delete(url, {
      headers: this.helper.getHeadersWithToken()
    });
  }

  addSubreddit(subReddit: string, description: string) {
    const url = `${this.newsUrl}/reddit`;
    return this.http.post(url, JSON.stringify({ subReddit, description }), {
      headers: this.helper.getHeadersWithToken()
    });
  }
}
