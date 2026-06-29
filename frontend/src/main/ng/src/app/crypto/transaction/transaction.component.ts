import { Component, OnInit, ViewChild, ChangeDetectorRef } from '@angular/core';
import { Transaction } from '../../model/transaction';
import { CryptoService } from '../../service/crypto.service';
import { Globals } from '../../util/global';
import { environment } from '../../../environments/environment';
import { Bind } from 'primeng/bind';
import { Toolbar } from 'primeng/toolbar';
import { PrimeTemplate } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { Ripple } from 'primeng/ripple';
import { FileUpload } from 'primeng/fileupload';
import { TableModule } from 'primeng/table';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { FormsModule } from '@angular/forms';
import { Tag } from 'primeng/tag';
import { Image } from 'primeng/image';
import { Dialog } from 'primeng/dialog';
import { DecimalPipe, CurrencyPipe, DatePipe } from '@angular/common';

@Component({
    selector: 'app-transaction',
    templateUrl: './transaction.component.html',
    styleUrls: ['./transaction.component.css'],
    imports: [Bind, Toolbar, PrimeTemplate, ButtonDirective, Ripple, FileUpload, TableModule, InputText, Select, FormsModule, Tag, Image, Dialog, DecimalPipe, CurrencyPipe, DatePipe]
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

  constructor(private cryptoService: CryptoService, globals: Globals, private cdr: ChangeDetectorRef) {
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

  refresh(): void {
    this.fetchData();
  }

  private fetchData(): void {
    this.cryptoService.getTransactions().subscribe({
      next: (data) => {
        this.transactions = data;
        this.cdr.markForCheck();
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
