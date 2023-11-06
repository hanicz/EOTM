import { Component, OnInit, Input, OnChanges } from '@angular/core';
import { NewsService } from '../service/news.service';
import { News } from '../model/news';

@Component({
  selector: 'app-news',
  templateUrl: './news.component.html',
  styleUrls: ['./news.component.css']
})
export class NewsComponent implements OnInit {

  news: News[] = [];
  @Input() type = '';

  newsLoading: boolean = true;

  constructor(private newsService: NewsService) {
  }

  ngOnInit(): void {
  }

  ngOnChanges() {
    this.newsLoading = true;
    if (this.type != undefined && this.type != '') {
      this.newsService.getNews(this.type).subscribe({
        next: (data) => {
          this.newsLoading = false;
          this.news = data;
        }
      });
    }
  }
}
