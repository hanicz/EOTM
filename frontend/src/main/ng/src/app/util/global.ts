import { Injectable, EventEmitter, Output } from '@angular/core';
import { Subject } from 'rxjs';
import { StockWatch } from '../model/stockwatch';

@Injectable()
export class Globals {

    selectedStock = '';
    @Output() stockSelectedEvent = new EventEmitter<any>();

    cryptoCurrency: string = 'EUR';
    cryptoCurrencyChange: Subject<string> = new Subject<string>();

    stockCurrency: string = 'USD';
    stockCurrencyChange: Subject<string> = new Subject<string>();

    etfCurrency: string = 'EUR';
    etfCurrencyChange: Subject<string> = new Subject<string>();

    stockWatchList: StockWatch[] = [];
    @Output() stockWatchEvent = new EventEmitter<any>();

    currencies = [
        { label: 'Euro', value: 'EUR' },
        { label: 'USD', value: 'USD' },
        { label: 'Forint', value: 'HUF' }
    ];

    statuses = [
        { label: 'BUY', value: 'B' },
        { label: 'SELL', value: 'S' },
        { label: 'REDEMPTION', value: 'R' },
        { label: 'EARN', value: 'E' }
      ];

    constructor() {
        this.stockCurrencyChange.subscribe((value) => {
            this.stockCurrency = value;
        });

        this.cryptoCurrencyChange.subscribe((value) => {
            this.cryptoCurrency = value;
        });

        this.etfCurrencyChange.subscribe((value) => {
            this.etfCurrency = value;
        });
    }

    changeStockCurrency(value: string) {
        this.stockCurrencyChange.next(value);
    }

    changeCryptoCurrency(value: string) {
        this.cryptoCurrencyChange.next(value);
    }

    changeETFCurrency(value: string) {
        this.etfCurrencyChange.next(value);
    }
}