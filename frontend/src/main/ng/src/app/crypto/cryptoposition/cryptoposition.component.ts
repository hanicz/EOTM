import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { Transaction } from '../../model/transaction';
import { CryptoService } from '../../service/crypto.service';
import { Globals } from '../../util/global';
import { environment } from '../../../environments/environment';
import { Bind } from 'primeng/bind';
import { TableModule } from 'primeng/table';
import { PrimeTemplate } from 'primeng/api';
import { Image } from 'primeng/image';
import { Tag } from 'primeng/tag';
import { DecimalPipe, CurrencyPipe } from '@angular/common';

@Component({
    selector: 'app-cryptoposition',
    templateUrl: './cryptoposition.component.html',
    styleUrls: ['./cryptoposition.component.css'],
    imports: [Bind, TableModule, PrimeTemplate, Image, Tag, DecimalPipe, CurrencyPipe]
})
export class CryptopositionComponent implements OnInit {

  transactions: Transaction[] = [];
  myMath = Math;
  assetUrl: string;

  constructor(private cryptoService: CryptoService, private cdr: ChangeDetectorRef) {
    this.assetUrl = environment.assets_url;
    this.fetchData();
  }

  ngOnInit(): void {
  }

  private fetchData(): void {
    this.cryptoService.getPositions().subscribe({
      next: (data) => {
        this.transactions = data;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.log(error);
      }
    });
  }
}
