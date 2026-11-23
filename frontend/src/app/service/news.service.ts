import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { News } from '../model/news';
import { environment } from '../../environments/environment';
import { Subreddit } from '../model/subreddit';

@Injectable({
  providedIn: 'root'
})
export class NewsService {

  private newsUrl = `${environment.API_URL}/api/v1/news`;

  constructor(private http: HttpClient) { }

  getNews(category: string) {
    const url = `${this.newsUrl}/${category}`;
    return this.http.get<News[]>(url);
  };

  getSubreddits() {
    const url = `${this.newsUrl}/reddit`;
    return this.http.get<Subreddit[]>(url);
  }

  deleteSubReddit(id: number) {
    const url = `${this.newsUrl}/reddit/${id}`;
    return this.http.delete(url);
  }

  addSubreddit(subReddit: string, description: string) {
    const url = `${this.newsUrl}/reddit`;
    return this.http.post(url, JSON.stringify({ subReddit, description }));
  }
}
