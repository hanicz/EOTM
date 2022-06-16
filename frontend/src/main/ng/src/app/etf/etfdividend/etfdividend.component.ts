import { Component, OnInit, ViewChild } from '@angular/core';
import { ETFDividend } from 'src/app/model/etfdividend';
import { EtfdividendService } from 'src/app/service/etfdividend.service';
import { Globals } from '../../util/global';

@Component({
  selector: 'app-etfdividend',
  templateUrl: './etfdividend.component.html',
  styleUrls: ['./etfdividend.component.css']
})
export class EtfdividendComponent implements OnInit {

  dividends: ETFDividend[] = [];
  currencies: any[];
  selectedDividends: ETFDividend[] = [];
  dividendDialog: boolean = false;
  dividend: ETFDividend = {} as ETFDividend;
  @ViewChild('fileUpload') fileUpload: any;

  constructor(private etfDividendService: EtfdividendService, globals: Globals) {
    this.currencies = globals.currencies;

    this.fetchData();
  }

  ngOnInit(): void {
  }

  private fetchData(): void {
    this.etfDividendService.getAllDividends().subscribe({
      next: (data) => {
        this.dividends = data;
      },
      error: (error) => {
        console.log(error);
      }
    });
  }

  openNew() {
    this.dividend = {} as ETFDividend;
    this.dividendDialog = true;
  }

  hideDialog() {
    this.dividendDialog = false;
  }

  editDividend(dividend: ETFDividend) {
    this.dividend = { ...dividend };
    this.dividendDialog = true;
  }

  deleteClicked() {
    let ids = '';
    this.selectedDividends.forEach(d => {
      ids += d.id + ',';
    });
    this.deleteByIds(ids);
  }

  deleteByIds(ids: string) {
    this.etfDividendService.deleteByIds(ids).subscribe({
      next: () => {
        this.selectedDividends = [];
        this.fetchData();
      }
    });
  }

  download() {
    this.etfDividendService.download().subscribe({
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
    if (this.dividend.id === undefined) {
      this.etfDividendService.create(this.dividend).subscribe({
        next: () => {
          this.fetchData();
          this.dividendDialog = false;
        }
      });
    } else {
      this.etfDividendService.update(this.dividend).subscribe({
        next: () => {
          this.fetchData();
          this.dividendDialog = false;
        }
      });
    }
  }

  onUpload(event: any) {
    for (let file of event.files) {
      this.etfDividendService.uploadCSV(file).subscribe({
        next: () => {
          this.fetchData();
          this.fileUpload.clear();
        }
      });
    }
  }

}
