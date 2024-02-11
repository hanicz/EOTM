import { Component, OnInit, ViewChild } from '@angular/core';
import { ForexTransaction } from 'src/app/model/forextransaction';
import { ForexService } from 'src/app/service/forex.service';
import { Globals } from '../../util/global';

@Component({
  selector: 'app-forextransaction',
  templateUrl: './forextransaction.component.html',
  styleUrls: ['./forextransaction.component.css']
})
export class ForextransactionComponent {

  forexTransactions: ForexTransaction[] = [];
  currencies: any[];
  statuses: any[];
  selectedForexTransactions: ForexTransaction[] = [];
  forexDialog: boolean = false;
  forexTransaction: ForexTransaction = {} as ForexTransaction;
  @ViewChild('fileUpload') fileUpload: any;

  constructor(private forexService: ForexService, globals: Globals) {
    this.currencies = globals.currencies;

    this.statuses = [
      { label: 'BUY', value: 'B' },
      { label: 'SELL', value: 'S' }
    ];

    this.fetchData();
  }

  ngOnInit(): void {
  }

  private fetchData(): void {
    this.forexService.getTransactions().subscribe({
      next: (data) => {
        this.forexTransactions = data;
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
