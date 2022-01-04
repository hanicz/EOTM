import { Component, OnInit, ViewChild } from '@angular/core';
import { Dividend } from 'src/app/model/dividend';
import { DividendService } from 'src/app/service/dividend.service';
import { Globals } from '../../util/global';

@Component({
  selector: 'app-dividend',
  templateUrl: './dividend.component.html',
  styleUrls: ['./dividend.component.css']
})
export class DividendComponent implements OnInit {

  dividends: Dividend[] = [];
  currencies: any[];
  selectedDividends: Dividend[] = [];
  dividendDialog: boolean = false;
  dividend: Dividend = {} as Dividend;
  @ViewChild('fileUpload') fileUpload: any;

  constructor(private dividendService: DividendService, globals: Globals) {
    this.currencies = globals.currencies;

    this.fetchData();
  }

  ngOnInit(): void {
  }

  private fetchData(): void {
    this.dividendService.getAllDividends().subscribe({
      next: (data) => {
        this.dividends = data;
      },
      error: (error) => {
        console.log(error);
      }
    });
  }

  openNew() {
    this.dividend = {} as Dividend;
    this.dividendDialog = true;
  }

  hideDialog() {
    this.dividendDialog = false;
  }

  editDividend(dividend: Dividend) {
    this.dividend = { ...dividend };
    this.dividendDialog = true;
  }

  deleteClicked() {
    let ids = '';
    this.selectedDividends.forEach(d => {
      ids += d.dividendId + ',';
    });
    this.deleteByIds(ids);
  }

  deleteByIds(ids: string) {
    this.dividendService.deleteByIds(ids).subscribe({
      next: () => {
        this.selectedDividends = [];
        this.fetchData();
      }
    });
  }

  download() {
    this.dividendService.download().subscribe({
      next: (data) => {
        let fileName = 'dividends.csv';
        let a = document.createElement('a');
        a.href = window.URL.createObjectURL(data as Blob);
        a.download = fileName;
        a.click();
      }
    });
  }

  saveDividend() {
    if (this.dividend.dividendId === undefined) {
      this.dividendService.create(this.dividend).subscribe({
        next: () => {
          this.fetchData();
          this.dividendDialog = false;
        }
      });
    } else {
      this.dividendService.update(this.dividend).subscribe({
        next: () => {
          this.fetchData();
          this.dividendDialog = false;
        }
      });
    }
  }

  onUpload(event: any) {
    for (let file of event.files) {
      this.dividendService.uploadCSV(file).subscribe({
        next: () => {
          this.fetchData();
          this.fileUpload.clear();
        }
      });
    }
  }

}
