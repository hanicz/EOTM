import { Component, OnInit, Output, EventEmitter  } from '@angular/core';
import { Transaction } from '../../model/transaction';
import { CryptoService } from '../../service/crypto.service';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-cryptoholding',
  templateUrl: './cryptoholding.component.html',
  styleUrls: ['./cryptoholding.component.css']
})
export class CryptoholdingComponent implements OnInit {

  transactions: Transaction[] = [];
  @Output() dataLoaded = new EventEmitter<Transaction[]>();
  assetUrl: string;
  transactionsLoading: boolean = true;

  constructor(private cryptoService: CryptoService) {
    this.assetUrl = environment.assets_url;
    this.fetchData();
  }

  ngOnInit(): void {
  }

  private fetchData(): void {
    this.cryptoService.getHoldings().subscribe({
      next: (data) => {
        this.transactionsLoading = false;
        this.transactions = data;
        this.dataLoaded.emit(this.transactions);
      },
      error: (error) => {
        console.log(error);
      }
    });
  }
}
