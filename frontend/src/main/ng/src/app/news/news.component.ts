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

  constructor(private newsService: NewsService) {
  }

  ngOnInit(): void {
  }

  ngOnChanges() {
    if (this.type != undefined && this.type != '') {
      this.newsService.getNews(this.type).subscribe({
        next: (data) => {
          this.news = data;
        }
      });
    }
  }
}
