import { Component, OnInit, Output, EventEmitter, ChangeDetectorRef  } from '@angular/core';
import { Transaction } from '../../model/transaction';
import { CryptoService } from '../../service/crypto.service';
import { environment } from '../../../environments/environment';
import { Bind } from 'primeng/bind';
import { TableModule } from 'primeng/table';
import { PrimeTemplate } from 'primeng/api';
import { Skeleton } from 'primeng/skeleton';
import { Image } from 'primeng/image';
import { Tag } from 'primeng/tag';
import { DecimalPipe, CurrencyPipe } from '@angular/common';

@Component({
    selector: 'app-cryptoholding',
    templateUrl: './cryptoholding.component.html',
    styleUrls: ['./cryptoholding.component.css'],
    imports: [Bind, TableModule, PrimeTemplate, Skeleton, Image, Tag, DecimalPipe, CurrencyPipe]
})
export class CryptoholdingComponent implements OnInit {

  transactions: Transaction[] = [];
  @Output() dataLoaded = new EventEmitter<Transaction[]>();
  assetUrl: string;
  transactionsLoading: boolean = true;

  constructor(private cryptoService: CryptoService, private cdr: ChangeDetectorRef) {
    this.assetUrl = environment.assets_url;
    this.fetchData();
  }

  ngOnInit(): void {
  }

  refresh(): void {
    this.transactionsLoading = true;
    this.fetchData();
  }

  markForCheck(): void {
    this.cdr.markForCheck();
  }

  private fetchData(): void {
    this.cryptoService.getHoldings().subscribe({
      next: (data) => {
        this.transactionsLoading = false;
        this.transactions = data;
        this.dataLoaded.emit(this.transactions);
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.log(error);
      }
    });
  }
}
