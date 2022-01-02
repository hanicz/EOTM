import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

@Injectable()
export class Globals {
    cryptoCurrency: string = 'EUR';
    cryptoCurrencyChange: Subject<string> = new Subject<string>();

    stockCurrency: string = 'USD';
    stockCurrencyChange: Subject<string> = new Subject<string>();

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
    }

    changeStockCurrency(value: string) {
        this.stockCurrencyChange.next(value);
    }

    changeCryptoCurrency(value: string) {
        this.cryptoCurrencyChange.next(value);
    }
}