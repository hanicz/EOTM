import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MessageService } from 'primeng/api';

import { WatchlistComponent } from './watchlist.component';
import { Globals } from '../util/global';
import { StockWatch } from '../model/stockwatch';
import { environment } from '../../environments/environment';

describe('WatchlistComponent', () => {
  let component: WatchlistComponent;
  let fixture: ComponentFixture<WatchlistComponent>;
  let http: HttpTestingController;

  const watchlistUrl = `${environment.API_URL}/api/v1/watchlist`;

  const stock = (id: number, shortName: string, groupId: number | null, groupName: string | null): StockWatch => ({
    tickerWatchId: id,
    stockName: `${shortName} Inc.`,
    stockShortName: shortName,
    liveValue: 10,
    currencyId: 'USD',
    change: 0,
    pchange: 0,
    stockExchange: 'US',
    groupId: groupId,
    groupName: groupName
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WatchlistComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations(),
        provideRouter([]), MessageService, Globals]
    })
      .compileComponents();

    fixture = TestBed.createComponent(WatchlistComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('buckets stocks under their group and puts the ungrouped ones last', () => {
    component.groups = [{ id: 1, name: 'Europe' }, { id: 2, name: 'Tech' }];
    component.globals.stockWatchList = [
      stock(10, 'AAA', 2, 'Tech'),
      stock(11, 'BBB', null, null),
      stock(12, 'CCC', 1, 'Europe')
    ];

    const buckets = component.stockBuckets;

    expect(buckets.map(b => b.name)).toEqual(['Europe', 'Tech', 'Ungrouped']);
    expect(buckets[0].stocks.map(s => s.stockShortName)).toEqual(['CCC']);
    expect(buckets[2].stocks.map(s => s.stockShortName)).toEqual(['BBB']);
  });

  it('keeps an empty group visible but leaves out an empty ungrouped bucket', () => {
    component.groups = [{ id: 1, name: 'Europe' }];
    component.globals.stockWatchList = [stock(10, 'AAA', 1, 'Europe')];

    const buckets = component.stockBuckets;

    expect(buckets.length).toBe(1);
    expect(buckets[0].name).toBe('Europe');

    component.globals.stockWatchList = [];
    expect(component.stockBuckets[0].stocks.length).toBe(0);
  });

  it('toggles a group collapsed and back', () => {
    expect(component.isCollapsed(1)).toBeFalsy();

    component.toggleGroup(1);
    expect(component.isCollapsed(1)).toBeTruthy();

    component.toggleGroup(1);
    expect(component.isCollapsed(1)).toBeFalsy();
  });

  it('moves a stock to a group without touching the other fields', () => {
    http.match(() => true).forEach(request => request.flush([]));

    component.movedStock = stock(10, 'AAA', null, null);
    component.movedGroupId = 3;
    component.saveMove();

    const request = http.expectOne(`${watchlistUrl}/stock/10/group?groupId=3`);
    expect(request.request.method).toBe('PUT');
    request.flush(stock(10, 'AAA', 3, 'Tech'));

    expect(component.moveDialog).toBeFalsy();
  });

  it('clears the group when moving a stock out of every group', () => {
    http.match(() => true).forEach(request => request.flush([]));

    component.movedStock = stock(10, 'AAA', 3, 'Tech');
    component.movedGroupId = null;
    component.saveMove();

    const request = http.expectOne(`${watchlistUrl}/stock/10/group`);
    expect(request.request.method).toBe('PUT');
  });
});
