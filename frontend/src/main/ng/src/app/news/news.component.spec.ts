import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MessageService } from 'primeng/api';

import { NewsComponent } from './news.component';
import { News } from '../model/news';
import { environment } from '../../environments/environment';

describe('NewsComponent', () => {
  let component: NewsComponent;
  let fixture: ComponentFixture<NewsComponent>;
  let http: HttpTestingController;

  const newsUrl = `${environment.API_URL}/api/v1/news`;

  const story = (overrides: Partial<News>): News => ({
    id: 1,
    category: 'company',
    datetime: Date.parse('2026-03-14T09:30:00Z') / 1000,
    headline: 'Quarterly results beat estimates',
    image: '',
    source: 'Example Wire',
    summary: 'A made up summary.',
    url: 'https://news.example.com/1',
    ...overrides
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NewsComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations(), MessageService]
    })
      .compileComponents();

    fixture = TestBed.createComponent(NewsComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  const load = (type: string, browse: boolean, items: News[]) => {
    component.type = type;
    component.browse = browse;
    component.ngOnChanges();
    http.expectOne(`${newsUrl}/${type}`).flush(items);
  };

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('requests the portfolio feed', () => {
    load('portfolio', true, [story({})]);

    expect(component.news.length).toBe(1);
    expect(component.filteredNews.length).toBe(1);
    expect(component.newsLoading).toBeFalsy();
    expect(component.loadError).toBeFalsy();
  });

  it('stops loading and flags the error when the request fails', () => {
    component.type = 'portfolio';
    component.ngOnChanges();
    http.expectOne(`${newsUrl}/portfolio`)
      .flush(null, { status: 500, statusText: 'Server Error' });

    expect(component.newsLoading).toBeFalsy();
    expect(component.loadError).toBeTruthy();
    expect(component.filteredNews.length).toBe(0);
    expect(component.emptyText).toBe('News could not be loaded right now.');
  });

  it('narrows by search text across headline, source and symbol', () => {
    load('portfolio', true, [
      story({ url: 'https://news.example.com/1', headline: 'Quarterly results beat estimates' }),
      story({ url: 'https://news.example.com/2', headline: 'Factory opens', symbol: 'BBBB' })
    ]);

    component.search = 'bbbb';
    component.filterChanged();

    expect(component.filteredNews.length).toBe(1);
    expect(component.filteredNews[0].url).toBe('https://news.example.com/2');
  });

  it('narrows by source', () => {
    load('portfolio', true, [
      story({ url: 'https://news.example.com/1', source: 'Example Wire' }),
      story({ url: 'https://news.example.com/2', source: 'Sample Press' })
    ]);

    expect(component.sourceOptions.map(option => option.value)).toEqual(['Example Wire', 'Sample Press']);

    component.sourceFilter = 'Sample Press';
    component.filterChanged();

    expect(component.filteredNews.length).toBe(1);
    expect(component.filteredNews[0].source).toBe('Sample Press');
  });

  it('narrows by date range', () => {
    load('portfolio', true, [
      story({ url: 'https://news.example.com/1', datetime: Date.parse('2026-03-01T09:00:00Z') / 1000 }),
      story({ url: 'https://news.example.com/2', datetime: Date.parse('2026-03-20T09:00:00Z') / 1000 })
    ]);

    component.fromDate = '2026-03-10';
    component.filterChanged();

    expect(component.filteredNews.length).toBe(1);
    expect(component.filteredNews[0].url).toBe('https://news.example.com/2');

    component.toDate = '2026-03-15';
    component.filterChanged();

    expect(component.filteredNews.length).toBe(0);
    expect(component.emptyText).toBe('No news matches these filters.');
  });

  it('clears every filter', () => {
    load('portfolio', true, [story({})]);

    component.search = 'nothing matches this';
    component.sourceFilter = 'Example Wire';
    component.fromDate = '2026-01-01';
    component.filterChanged();
    expect(component.hasFilters).toBeTruthy();

    component.clearFilters();

    expect(component.hasFilters).toBeFalsy();
    expect(component.filteredNews.length).toBe(1);
  });

  it('does not filter when it is embedded without controls', () => {
    load('company/AAAA', false, [story({}), story({ url: 'https://news.example.com/2' })]);

    component.search = 'nothing matches this';
    component.filterChanged();

    expect(component.filteredNews.length).toBe(2);
    expect(component.emptyText).toBe('No recent news for this ticker.');
  });
});
