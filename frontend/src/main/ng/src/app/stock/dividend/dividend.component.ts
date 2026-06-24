import { Component, OnInit, ViewChild, ChangeDetectorRef } from '@angular/core';
import { Dividend } from 'src/app/model/dividend';
import { DividendService } from 'src/app/service/dividend.service';
import { Globals } from '../../util/global';
import { Bind } from 'primeng/bind';
import { Toolbar } from 'primeng/toolbar';
import { PrimeTemplate } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { Ripple } from 'primeng/ripple';
import { FileUpload } from 'primeng/fileupload';
import { TableModule } from 'primeng/table';
import { InputText } from 'primeng/inputtext';
import { Dialog } from 'primeng/dialog';
import { FormsModule } from '@angular/forms';
import { Select } from 'primeng/select';
import { Image } from 'primeng/image';
import { CurrencyPipe, DatePipe } from '@angular/common';

@Component({
    selector: 'app-dividend',
    templateUrl: './dividend.component.html',
    styleUrls: ['./dividend.component.css'],
    imports: [Bind, Toolbar, PrimeTemplate, ButtonDirective, Ripple, FileUpload, TableModule, InputText, Dialog, FormsModule, Select, Image, CurrencyPipe, DatePipe]
})
export class DividendComponent implements OnInit {

  dividends: Dividend[] = [];
  currencies: any[];
  selectedDividends: Dividend[] = [];
  dividendDialog: boolean = false;
  dividend: Dividend = {} as Dividend;
  @ViewChild('fileUpload') fileUpload: any;
  globals: Globals;

  constructor(private dividendService: DividendService, globals: Globals, private cdr: ChangeDetectorRef) {
    this.globals = globals;
    this.currencies = globals.currencies;

    this.fetchData();
  }

  ngOnInit(): void {
  }

  private fetchData(): void {
    this.dividendService.getAllDividends().subscribe({
      next: (data) => {
        this.dividends = data;
        this.cdr.markForCheck();
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
