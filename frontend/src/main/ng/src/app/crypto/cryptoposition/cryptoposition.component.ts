import { Component, OnInit } from '@angular/core';
import { Transaction } from '../../model/transaction';
import { CryptoService } from '../../service/crypto.service';
import { Globals } from '../../util/global';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-cryptoposition',
  templateUrl: './cryptoposition.component.html',
  styleUrls: ['./cryptoposition.component.css']
})
export class CryptopositionComponent implements OnInit {

  transactions: Transaction[] = [];
  myMath = Math;
  assetUrl: string;

  constructor(private cryptoService: CryptoService) {
    this.assetUrl = environment.assets_url;
    this.fetchData();
  }

  ngOnInit(): void {
  }

  private fetchData(): void {
    this.cryptoService.getPositions().subscribe({
      next: (data) => {
        this.transactions = data;
      },
      error: (error) => {
        console.log(error);
      }
    });
  }
}
