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
import { Symbol } from '../model/symbol';
import { Exchange } from '../model/exchange';

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
  symbols: Symbol[] = [];
  exchanges: Exchange[] = [];
  subscription: Subscription;
  globals: Globals;
  display: boolean = false;
  assetUrl: string;
  fromForex: string = '';
  toForex: string = '';
  selectedStock: Symbol = {} as Symbol;
  selectedExchange: Exchange = {} as Exchange;

  forexLoading: boolean = false;
  stockLoading: boolean = false;
  cryptoLoading: boolean = false;
  exchangesLoading: boolean = true;
  stocksLoading: boolean = false;


  constructor(private watchlistService: WatchlistService,
    globals: Globals,
    private router: Router,
    private stockService: StockService,
    private cryptoService: CryptoService) {

    this.assetUrl = environment.assets_url;
    this.globals = globals;
    this.fetchData();

    this.stockService.getAllExchanges().subscribe({
      next: (data) => {
        this.exchangesLoading = false;
        this.exchanges = data;
      }
    });
    

    const interv = interval(60000);
    this.subscription = interv.subscribe(this.fetchData);
  }

  ngOnInit(): void {
    this.globals.stockWatchEvent.subscribe(e => {
      this.fetchData();
    });
  }

  private fetchData = () => {
    this.forexLoading = this.cryptoLoading = this.stockLoading = true;
    this.fetchCryptoWatchList();
    this.fetchStockWatchList();
    this.watchlistService.getForexWatchList().subscribe({
      next: (data) => {
        this.forexLoading = false;
        this.forexWatchList = data;
      }
    });
  }

  private fetchStockWatchList() {
    this.watchlistService.getStockWatchList().subscribe({
      next: (data) => {
        this.stockLoading = false;
        this.globals.stockWatchList = data;
      }
    });
  }

  private fetchCryptoWatchList() {
    this.watchlistService.getCryptoWatchList("EUR").subscribe({
      next: (data) => {
        this.cryptoLoading = false;
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

  checkStockContain() {
    return this.globals.stockWatchList.some(s => s.stockShortName === this.selectedStock.Code)
  }

  checkCryptoContain(name: string) {
    return this.cryptoWatchList.some(c => c.name === name)
  }

  checkForexContain() {
    return this.forexWatchList.some(f => f.fromCurrencyId === this.fromForex && f.toCurrencyId === this.toForex)
  }


  createStockWatch() {
    this.watchlistService.createNewStockWatch(this.selectedStock, this.selectedExchange).subscribe({
      next: () => {
        this.display = false;
        this.fetchData();
      }
    });
  }

  createCryptoWatch(id: string) {
    this.createWatch(`/crypto/${id}`);
  }

  createForexWatch() {
    this.watchlistService.createNewForexWatch(this.fromForex, this.toForex).subscribe({
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

  deleteForexWatch() {
    let id = this.forexWatchList.find(f => f.fromCurrencyId === this.fromForex && f.toCurrencyId === this.toForex)
    this.deleteWatch(`/forex/${id?.forexWatchID}`);
  }

  deleteStockWatch() {
    let id = this.globals.stockWatchList.find(s => s.stockShortName === this.selectedStock.Code);
    this.deleteWatch(`/stock/${id?.tickerWatchId}`);
  }
  
  deleteCryptoWatch(name: string) {
    let id = this.cryptoWatchList.find(c => c.name === name);
    this.deleteWatch(`/crypto/${id?.cryptoWatchId}`);
  }

  deleteWatch(path: string) {
    this.watchlistService.deleteWatch(path).subscribe({
      next: () => {
        this.display = false;
        this.fetchData();
      }
    });
  }

  exchangeChanged(event: any) {
    this.stocksLoading = true;
    this.stockService.getAllSymbols(this.selectedExchange.Code).subscribe({
      next: (data) => {
        this.stocksLoading = false;
        this.symbols = data;
      }
    });
  }
}
