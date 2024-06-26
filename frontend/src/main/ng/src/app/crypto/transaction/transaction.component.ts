import { Component, OnInit, ViewChild } from '@angular/core';
import { Transaction } from '../../model/transaction';
import { CryptoService } from '../../service/crypto.service';
import { Globals } from '../../util/global';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-transaction',
  templateUrl: './transaction.component.html',
  styleUrls: ['./transaction.component.css']
})
export class TransactionComponent implements OnInit {

  transactions: Transaction[] = [];
  currencies: any[];
  statuses: any[];
  selectedTransactions: Transaction[] = [];
  transactionDialog: boolean = false;
  transaction: Transaction = {} as Transaction;
  @ViewChild('fileUpload') fileUpload: any;
  assetUrl: string;

  constructor(private cryptoService: CryptoService, globals: Globals) {
    this.currencies = globals.currencies;
    this.statuses = globals.statuses;
    this.assetUrl = environment.assets_url;
    this.fetchData();
  }

  ngOnInit(): void {
  }

  onChange(event: any): void {
    this.fetchData();
  }

  private fetchData(): void {
    this.cryptoService.getTransactions().subscribe({
      next: (data) => {
        this.transactions = data;
      },
      error: (error) => {
        console.log(error);
      }
    });
  }

  openNew() {
    this.transaction = {} as Transaction;
    this.transactionDialog = true;
  }

  hideDialog() {
    this.transactionDialog = false;
  }

  editTransaction(transaction: Transaction) {
    this.transaction = { ...transaction };
    this.transactionDialog = true;
  }

  deleteClicked() {
    let ids = '';
    this.selectedTransactions.forEach(t => {
      ids += t.id + ',';
    });
    this.deleteByIds(ids);
  }

  deleteByIds(ids: string) {
    this.cryptoService.deleteByIds(ids).subscribe({
      next: () => {
        this.selectedTransactions = [];
        this.fetchData();
      }
    });
  }

  download() {
    this.cryptoService.download().subscribe({
      next: (data) => {
        let fileName = 'transactions.csv';
        let a = document.createElement('a');
        a.href = window.URL.createObjectURL(data as Blob);
        a.download = fileName;
        a.click();
      }
    });
  }

  saveTransaction() {
    if (this.transaction.id === undefined) {
      this.cryptoService.create(this.transaction).subscribe({
        next: () => {
          this.fetchData();
          this.transactionDialog = false;
        }
      });
    } else {
      this.cryptoService.update(this.transaction).subscribe({
        next: () => {
          this.fetchData();
          this.transactionDialog = false;
        }
      });
    }
  }

  onUpload(event: any) {
    for (let file of event.files) {
      this.cryptoService.uploadCSV(file).subscribe({
        next: () => {
          this.fetchData();
          this.fileUpload.clear();
        }
      });
    }
  }
}
