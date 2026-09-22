import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { Input } from '@angular/core';
import { EMPTY_FILTER, PortfolioFilter, filterRows } from '../../util/tablefilter';
import { Transaction } from '../../model/transaction';
import { CryptoService } from '../../service/crypto.service';
import { Globals } from '../../util/global';
import { environment } from '../../../environments/environment';
import { Bind } from 'primeng/bind';
import { TableModule } from 'primeng/table';
import { PrimeTemplate } from 'primeng/api';
import { Image } from 'primeng/image';
import { DecimalPipe, CurrencyPipe, NgClass } from '@angular/common';

@Component({
    selector: 'app-cryptoposition',
    templateUrl: './cryptoposition.component.html',
    imports: [Bind, TableModule, PrimeTemplate, Image, DecimalPipe, CurrencyPipe, NgClass]
})
export class CryptopositionComponent implements OnInit {
  visibleTransactions: Transaction[] = [];
  private activeFilter: PortfolioFilter = EMPTY_FILTER;

  @Input() set filter(value: PortfolioFilter) {
    this.activeFilter = value ?? EMPTY_FILTER;
    this.applyFilter();
  }

  private applyFilter(): void {
    this.visibleTransactions = filterRows(this.transactions, this.activeFilter, ['symbol']);
  }


  transactions: Transaction[] = [];
  myMath = Math;
  assetUrl: string;

  constructor(private cryptoService: CryptoService, private cdr: ChangeDetectorRef) {
    this.assetUrl = environment.assets_url;
    this.fetchData();
  }

  ngOnInit(): void {
  }

  refresh(): void {
    this.fetchData();
  }

  private fetchData(): void {
    this.cryptoService.getPositions().subscribe({
      next: (data) => {
        this.transactions = data;
        this.applyFilter();
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.log(error);
      }
    });
  }
}
