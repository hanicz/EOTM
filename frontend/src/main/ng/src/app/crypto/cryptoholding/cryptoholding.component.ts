import { Component, OnInit, Output, EventEmitter  } from '@angular/core';
import { Transaction } from '../../model/transaction';
import { CryptoService } from '../../service/crypto.service';
import { Globals } from '../../util/global';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-cryptoholding',
  templateUrl: './cryptoholding.component.html',
  styleUrls: ['./cryptoholding.component.css']
})
export class CryptoholdingComponent implements OnInit {

  transactions: Transaction[] = [];
  @Output() dataLoaded = new EventEmitter<Transaction[]>();
  globals: Globals;
  assetUrl: string;
  transactionsLoading: boolean = true;

  constructor(private cryptoService: CryptoService, globals: Globals) {
    this.globals = globals;
    this.assetUrl = environment.assets_url;
    this.fetchData();
    globals.cryptoCurrencyChange.subscribe(value => {
      this.fetchData();
    });
  }

  ngOnInit(): void {
  }

  private fetchData(): void {
    this.cryptoService.getHoldings(this.globals.cryptoCurrency).subscribe({
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
