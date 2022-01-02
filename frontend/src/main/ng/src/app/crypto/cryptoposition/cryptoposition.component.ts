import { Component, OnInit } from '@angular/core';
import { Transaction } from '../../model/transaction';
import { CryptoService } from '../../service/crypto.service';
import { Globals } from '../../util/global';

@Component({
  selector: 'app-cryptoposition',
  templateUrl: './cryptoposition.component.html',
  styleUrls: ['./cryptoposition.component.css']
})
export class CryptopositionComponent implements OnInit {

  transactions: Transaction[] = [];
  myMath = Math;
  globals: Globals;

  constructor(private cryptoService: CryptoService, globals: Globals) {
    this.globals = globals;

    this.fetchData();
    globals.cryptoCurrencyChange.subscribe(value => {
      this.fetchData();
    });
  }

  ngOnInit(): void {
  }

  private fetchData(): void {
    this.cryptoService.getPositions(this.globals.cryptoCurrency).subscribe({
      next: (data) => {
        this.transactions = data;
      },
      error: (error) => {
        console.log(error);
      }
    });
  }
}
