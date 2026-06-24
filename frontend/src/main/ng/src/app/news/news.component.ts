import { Component, OnInit, Input, OnChanges, ChangeDetectorRef } from '@angular/core';
import { NewsService } from '../service/news.service';
import { News } from '../model/news';
import { Bind } from 'primeng/bind';
import { Skeleton } from 'primeng/skeleton';
import { Image } from 'primeng/image';
import { SlicePipe, DatePipe } from '@angular/common';

@Component({
    selector: 'app-news',
    templateUrl: './news.component.html',
    styleUrls: ['./news.component.css'],
    imports: [Bind, Skeleton, Image, SlicePipe, DatePipe]
})
export class NewsComponent implements OnInit {

  news: News[] = [];
  @Input() type = '';

  newsLoading: boolean = true;

  constructor(private newsService: NewsService, private cdr: ChangeDetectorRef) {
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
          this.cdr.markForCheck();
        }
      });
    }
  }

  isRedditImage(image: string): boolean {
    return image.includes('redd.it') || image.includes('reddit.com') || image.includes('redditmedia.com') || image.includes('redditstatic.com');
  }
}
