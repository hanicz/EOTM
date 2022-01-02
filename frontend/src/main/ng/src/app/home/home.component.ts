import { Component, OnInit } from '@angular/core';
import { News } from '../model/news';
import { NewsService } from '../service/news.service';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {

  generalNews: News[] = [];
  forexNews: News[] = [];
  cryptoNews: News[] = [];

  constructor(private newsService: NewsService) {
    this.fetchData();
  }

  ngOnInit(): void {
  }

  fetchData() {
    this.newsService.getNews("general").subscribe({
      next: (data) => {
        this.generalNews = data;
      }
    });
    this.newsService.getNews("forex").subscribe({
      next: (data) => {
        this.forexNews = data;
      }
    });
    this.newsService.getNews("crypto").subscribe({
      next: (data) => {
        this.cryptoNews = data;
      }
    });
  }
}
