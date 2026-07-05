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

    private readonly fallbackImage = 'data:image/svg+xml;utf8,' +
        encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24"><rect width="24" height="24" rx="4" fill="#d8d3c4"/></svg>');

    errorHandler(event: any) {
        const target = event.target as HTMLImageElement;
        if (target.dataset['fallbackApplied']) {
            return;
        }
        target.dataset['fallbackApplied'] = 'true';
        target.src = this.fallbackImage;
    }
}