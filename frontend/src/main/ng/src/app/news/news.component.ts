import { Component, OnInit, OnChanges, Input, ChangeDetectorRef } from '@angular/core';
import { NewsService } from '../service/news.service';
import { News } from '../model/news';
import { Bind } from 'primeng/bind';
import { Skeleton } from 'primeng/skeleton';
import { Image } from 'primeng/image';
import { Toast } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { DataView } from 'primeng/dataview';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { ButtonDirective } from 'primeng/button';
import { Ripple } from 'primeng/ripple';
import { Tooltip } from 'primeng/tooltip';
import { FormsModule } from '@angular/forms';
import { SlicePipe, DatePipe } from '@angular/common';

@Component({
    selector: 'app-news',
    templateUrl: './news.component.html',
    styleUrls: ['./news.component.css'],
    imports: [Bind, Skeleton, Image, Toast, DataView, InputText, Select, ButtonDirective, Ripple,
        Tooltip, FormsModule, SlicePipe, DatePipe]
})
export class NewsComponent implements OnInit, OnChanges {

  news: News[] = [];
  filteredNews: News[] = [];
  sourceOptions: { label: string, value: string }[] = [];

  @Input() type = '';
  @Input() browse = false;

  search = '';
  sourceFilter: string | null = null;
  fromDate = '';
  toDate = '';

  newsLoading: boolean = true;
  loadError: boolean = false;

  constructor(private newsService: NewsService, private messageService: MessageService,
    private cdr: ChangeDetectorRef) {
  }

  ngOnInit(): void {
  }

  ngOnChanges() {
    this.loadNews();
  }

  retry(): void {
    this.loadNews();
  }

  get hasFilters(): boolean {
    return !!this.search || !!this.sourceFilter || !!this.fromDate || !!this.toDate;
  }

  get emptyText(): string {
    if (this.loadError) {
      return 'News could not be loaded right now.';
    }
    if (this.hasFilters) {
      return 'No news matches these filters.';
    }
    if (this.type.includes('company/')) {
      return 'No recent news for this ticker.';
    }
    if (this.type.includes('portfolio')) {
      return 'No recent news for the tickers you hold or watch.';
    }
    if (this.type.includes('reddit')) {
      return 'No recent posts. Add subreddits in Settings.';
    }
    return 'No recent news in this category.';
  }

  filterChanged(): void {
    this.applyFilters();
  }

  clearFilters(): void {
    this.search = '';
    this.sourceFilter = null;
    this.fromDate = '';
    this.toDate = '';
    this.applyFilters();
  }

  isRedditSource(item: News): boolean {
    return this.type.includes('reddit') || this.isRedditImage(item.image);
  }

  isRedditImage(image: string): boolean {
    if (!image) {
      return false;
    }
    return image.includes('redd.it') || image.includes('reddit.com') || image.includes('redditmedia.com') || image.includes('redditstatic.com');
  }

  hasValidImage(item: News): boolean {
    return !!item.image && /^https?:\/\//.test(item.image) && !this.isRedditSource(item);
  }

  private loadNews(): void {
    if (this.type == undefined || this.type == '') {
      return;
    }
    this.newsLoading = true;
    this.loadError = false;
    this.newsService.getNews(this.type).subscribe({
      next: (data) => {
        this.newsLoading = false;
        this.news = data;
        this.buildSourceOptions();
        this.applyFilters();
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.newsLoading = false;
        this.loadError = true;
        this.news = [];
        this.filteredNews = [];
        this.sourceOptions = [];
        this.messageService.add({
          key: 'news', severity: 'error', summary: 'Error',
          detail: error.error?.error ?? 'Could not load the news.'
        });
        this.cdr.markForCheck();
      }
    });
  }

  private buildSourceOptions(): void {
    this.sourceOptions = [...new Set(this.news.map(item => item.source).filter(source => !!source))]
      .sort((a, b) => a.localeCompare(b))
      .map(source => ({ label: source, value: source }));
  }

  private applyFilters(): void {
    if (!this.browse) {
      this.filteredNews = this.news;
      return;
    }
    const term = this.search.trim().toLowerCase();
    this.filteredNews = this.news.filter(item => this.matchesTerm(item, term)
      && this.matchesSource(item) && this.matchesDates(item));
  }

  private matchesTerm(item: News, term: string): boolean {
    if (!term) {
      return true;
    }
    return [item.headline, item.summary, item.source, item.symbol]
      .some(field => (field ?? '').toLowerCase().includes(term));
  }

  private matchesSource(item: News): boolean {
    return !this.sourceFilter || item.source === this.sourceFilter;
  }

  private matchesDates(item: News): boolean {
    if (!this.fromDate && !this.toDate) {
      return true;
    }
    const day = new Date(item.datetime * 1000).toISOString().slice(0, 10);
    if (this.fromDate && day < this.fromDate) {
      return false;
    }
    return !(this.toDate && day > this.toDate);
  }
}
