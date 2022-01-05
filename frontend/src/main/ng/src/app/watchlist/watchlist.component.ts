import { Component, OnInit } from '@angular/core';
import { CryptoWatch } from '../model/cryptowatch';
import { ForexWatch } from '../model/forexwatch';
import { StockWatch } from '../model/stockwatch';
import { WatchlistService } from '../service/watchlist.service';
import { interval, Subscription } from 'rxjs';
import { Globals } from '../util/global';
import { Router } from '@angular/router';

@Component({
  selector: 'app-watchlist',
  templateUrl: './watchlist.component.html',
  styleUrls: ['./watchlist.component.css']
})
export class WatchlistComponent implements OnInit {

  stockWatchList: StockWatch[] = [];
  forexWatchList: ForexWatch[] = [];
  cryptoWatchList: CryptoWatch[] = [];
  subscription: Subscription;
  globals: Globals;

  constructor(private watchlistService: WatchlistService, globals: Globals, private router: Router) {
    this.globals = globals;
    this.fetchData();

    const interv = interval(60000);
    this.subscription = interv.subscribe(this.fetchData);
  }

  ngOnInit(): void {
  }

  private fetchData = () => {
    this.watchlistService.getStockWatchList().subscribe({
      next: (data) => {
        this.stockWatchList = data;
      }
    });
    this.watchlistService.getForexWatchList().subscribe({
      next: (data) => {
        this.forexWatchList = data;
      }
    });
    this.watchlistService.getCryptoWatchList("EUR").subscribe({
      next: (data) => {
        this.cryptoWatchList = data;
      }
    });
  }

  stockSelected(stock: string) {
    this.globals.selectedStock = stock;
    this.router.navigate(['./search']);
  }
}
