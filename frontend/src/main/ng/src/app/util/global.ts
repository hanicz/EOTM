import { Injectable, EventEmitter, Output } from '@angular/core';
import { StockWatch } from '../model/stockwatch';
import { environment } from '../../environments/environment';

@Injectable()
export class Globals {

    selectedStock = '';
    @Output() stockSelectedEvent = new EventEmitter<any>();

    selectedExchange = '';
    @Output() exchangeSelectedEvent = new EventEmitter<any>();

    stockWatchList: StockWatch[] = [];
    @Output() stockWatchEvent = new EventEmitter<any>();

    assetUrl: string;

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
        this.assetUrl = environment.assets_url;
    }

    errorHandler(event: any) {
        event.target.src = this.assetUrl + 'logo-placeholder.png';
    }
}