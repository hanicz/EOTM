import { Component, OnInit, ViewChild, ChangeDetectorRef } from '@angular/core';
import { SecurityTransaction } from '../../model/securityTransaction';
import { SecurityService } from '../../service/security.service';
import { Globals } from '../../util/global';
import { Security } from 'src/app/model/security';
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
import { Dialog } from 'primeng/dialog';
import { CurrencyPipe, DatePipe } from '@angular/common';

@Component({
    selector: 'app-security-transaction',
    templateUrl: './transaction.component.html',
    styleUrls: ['./transaction.component.css'],
    imports: [Bind, Toolbar, PrimeTemplate, ButtonDirective, Ripple, FileUpload, TableModule, InputText, Select, FormsModule, Tag, Dialog, CurrencyPipe, DatePipe]
})
export class TransactionComponent implements OnInit {

  transactions: SecurityTransaction[] = [];
  currencies: any[];
  statuses: any[];
  selectedTransactions: SecurityTransaction[] = [];
  transactionDialog: boolean = false;
  transaction: SecurityTransaction = {} as SecurityTransaction;
  @ViewChild('fileUpload') fileUpload: any;
  globals: Globals;
  securities: Security[] = [];
  selectedExistingSecurity: Security | null = null;

  constructor(private securityService: SecurityService, globals: Globals, private cdr: ChangeDetectorRef) {
    this.globals = globals;
    this.currencies = globals.currencies;

    this.statuses = [
      { label: 'BUY', value: 'B' },
      { label: 'SELL', value: 'S' }
    ];

    this.securityService.getAllSecurities().subscribe({
      next: (data) => {
        this.securities = data;
        this.cdr.markForCheck();
      }
    });

    this.fetchData();
  }

  ngOnInit(): void {
  }

  refresh(): void {
    this.fetchData();
  }

  private fetchData(): void {
    this.securityService.getTransactions().subscribe({
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
    this.transaction = {} as SecurityTransaction;
    this.selectedExistingSecurity = null;
    this.transactionDialog = true;
  }

  hideDialog() {
    this.transactionDialog = false;
  }

  editTransaction(transaction: SecurityTransaction) {
    this.transaction = { ...transaction };
    this.selectedExistingSecurity = this.securities.find(s => s.id === transaction.securityId) ?? null;
    this.transactionDialog = true;
  }

  existingSecurityChanged(): void {
    if (this.selectedExistingSecurity) {
      this.transaction.securityId = this.selectedExistingSecurity.id;
      this.transaction.securityName = this.selectedExistingSecurity.name;
    }
  }

  deleteClicked() {
    let ids = '';
    this.selectedTransactions.forEach(t => {
      ids += t.transactionId + ',';
    });
    this.deleteByIds(ids);
  }

  deleteByIds(ids: string) {
    this.securityService.deleteByIds(ids).subscribe({
      next: () => {
        this.selectedTransactions = [];
        this.fetchData();
      }
    });
  }

  download() {
    this.securityService.download().subscribe({
      next: (data) => {
        let fileName = 'security_transactions.csv';
        let a = document.createElement('a');
        a.href = window.URL.createObjectURL(data as Blob);
        a.download = fileName;
        a.click();
      }
    });
  }

  saveTransaction() {
    if (this.transaction.transactionId === undefined) {
      this.securityService.create(this.transaction).subscribe({
        next: () => {
          this.fetchData();
          this.transactionDialog = false;
        }
      });
    } else {
      this.securityService.update(this.transaction).subscribe({
        next: () => {
          this.fetchData();
          this.transactionDialog = false;
        }
      });
    }
  }

  onUpload(event: any) {
    for (let file of event.files) {
      this.securityService.uploadCSV(file).subscribe({
        next: () => {
          this.fetchData();
          this.fileUpload.clear();
        }
      });
    }
  }
}
