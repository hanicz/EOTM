import { Component, OnInit, ViewChild, ChangeDetectorRef } from '@angular/core';
import { ForexTransaction } from 'src/app/model/forextransaction';
import { ForexService } from 'src/app/service/forex.service';
import { Globals } from '../../util/global';
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
    selector: 'app-forextransaction',
    templateUrl: './forextransaction.component.html',
    styleUrls: ['./forextransaction.component.css'],
    imports: [Bind, Toolbar, PrimeTemplate, ButtonDirective, Ripple, FileUpload, TableModule, InputText, Select, FormsModule, Tag, Dialog, CurrencyPipe, DatePipe]
})
export class ForextransactionComponent {

  forexTransactions: ForexTransaction[] = [];
  currencies: any[];
  statuses: any[];
  selectedForexTransactions: ForexTransaction[] = [];
  forexDialog: boolean = false;
  forexTransaction: ForexTransaction = {} as ForexTransaction;
  @ViewChild('fileUpload') fileUpload: any;

  constructor(private forexService: ForexService, globals: Globals, private cdr: ChangeDetectorRef) {
    this.currencies = globals.currencies;

    this.statuses = [
      { label: 'BUY', value: 'B' },
      { label: 'SELL', value: 'S' }
    ];

    this.fetchData();
  }

  ngOnInit(): void {
  }

  refresh(): void {
    this.fetchData();
  }

  private fetchData(): void {
    this.forexService.getTransactions().subscribe({
      next: (data) => {
        this.forexTransactions = data;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.log(error);
      }
    });
  }

  openNew() {
    this.forexTransaction = {} as ForexTransaction;
    this.forexDialog = true;
  }

  hideDialog() {
    this.forexDialog = false;
  }

  editForexTransaction(forexTransaction: ForexTransaction) {
    this.forexTransaction = { ...forexTransaction };
    this.forexDialog = true;
  }

  deleteClicked() {
    let ids = '';
    this.selectedForexTransactions.forEach(t => {
      ids += t.forexTransactionId + ',';
    });
    this.deleteByIds(ids);
  }

  deleteByIds(ids: string) {
    this.forexService.deleteByIds(ids).subscribe({
      next: () => {
        this.selectedForexTransactions = [];
        this.fetchData();
      }
    });
  }

  download() {
    this.forexService.download().subscribe({
      next: (data) => {
        let fileName = 'forexTransactions.csv';
        let a = document.createElement('a');
        a.href = window.URL.createObjectURL(data as Blob);
        a.download = fileName;
        a.click();
      }
    });
  }

  saveForexTransaction() {
    if (this.forexTransaction.forexTransactionId === undefined) {
      this.forexService.create(this.forexTransaction).subscribe({
        next: () => {
          this.fetchData();
          this.forexDialog = false;
        }
      });
    } else {
      this.forexService.update(this.forexTransaction).subscribe({
        next: () => {
          this.fetchData();
          this.forexDialog = false;
        }
      });
    }
  }

  onUpload(event: any) {
    for (let file of event.files) {
      this.forexService.uploadCSV(file).subscribe({
        next: () => {
          this.fetchData();
          this.fileUpload.clear();
        }
      });
    }
  }

}
