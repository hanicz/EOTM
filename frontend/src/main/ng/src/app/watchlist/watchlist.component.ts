import { Component, OnInit } from '@angular/core';
import { CryptoWatch } from '../model/cryptowatch';
import { ForexWatch } from '../model/forexwatch';
import { StockWatch } from '../model/stockwatch';
import { WatchlistService } from '../service/watchlist.service';
import { interval, Subscription } from 'rxjs';
import { Globals } from '../util/global';
import { Router } from '@angular/router';
import { StockService } from '../service/stock.service';
import { Stock } from '../model/stock';
import { CryptoService } from '../service/crypto.service';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-watchlist',
  templateUrl: './watchlist.component.html',
  styleUrls: ['./watchlist.component.css']
})
export class WatchlistComponent implements OnInit {

  forexWatchList: ForexWatch[] = [];
  cryptoWatchList: CryptoWatch[] = [];
  stocks: Stock[] = [];
  cryptos: Crypto[] = [];
  subscription: Subscription;
  globals: Globals;
  display: boolean = false;
  assetUrl: string;


  constructor(private watchlistService: WatchlistService,
    globals: Globals,
    private router: Router,
    private stockService: StockService,
    private cryptoService: CryptoService) {

    this.assetUrl = environment.assets_url;
    this.globals = globals;
    this.fetchData();

    const interv = interval(60000);
    this.subscription = interv.subscribe(this.fetchData);
  }

  ngOnInit(): void {
    this.globals.stockWatchEvent.subscribe(e => {
      this.fetchData();
    });
  }

  private fetchData = () => {
    this.fetchCryptoWatchList();
    this.fetchStockWatchList();
    this.watchlistService.getForexWatchList().subscribe({
      next: (data) => {
        this.forexWatchList = data;
      }
    });
  }

  private fetchStockWatchList() {
    this.watchlistService.getStockWatchList().subscribe({
      next: (data) => {
        this.globals.stockWatchList = data;
      }
    });
  }

  private fetchCryptoWatchList() {
    this.watchlistService.getCryptoWatchList("EUR").subscribe({
      next: (data) => {
        this.cryptoWatchList = data;
      }
    });
  }

  stockSelected(stock: StockWatch) {
    this.globals.selectedExchange = stock.stockExchange;
    this.globals.selectedStock = stock.stockShortName;
    this.globals.stockSelectedEvent.emit();
    this.router.navigate(['./search']);
  }

  showDialog() {
    this.display = true;

    this.stockService.getAllStocks().subscribe({
      next: (data) => {
        this.stocks = data;
      }
    });

    this.cryptoService.getAllCrypto().subscribe({
      next: (data) => {
        this.cryptos = data;
      }
    });
  }

  checkStockContain(shortName: string) {
    return this.globals.stockWatchList.some(s => s.stockShortName === shortName)
  }

  checkCryptoContain(name: string) {
    return this.cryptoWatchList.some(c => c.name === name)
  }

  deleteStockWatch(shortName: string) {
    let id = this.globals.stockWatchList.find(s => s.stockShortName === shortName);
    this.deleteWatch(`/stock/${id?.tickerWatchId}`);
  }

  createStockWatch(id: string) {
    this.createWatch(`/stock/${id}`);
  }

  deleteCryptoWatch(name: string) {
    let id = this.cryptoWatchList.find(c => c.name === name);
    this.deleteWatch(`/crypto/${id?.cryptoWatchId}`);
  }

  createCryptoWatch(id: string) {
    this.createWatch(`/crypto/${id}`);
  }

  deleteWatch(path: string) {
    this.watchlistService.deleteWatch(path).subscribe({
      next: () => {
        this.display = false;
        this.fetchData();
      }
    });
  }

  createWatch(path: string) {
    this.watchlistService.createWatch(path).subscribe({
      next: () => {
        this.display = false;
        this.fetchData();
      }
    });
  }
}
