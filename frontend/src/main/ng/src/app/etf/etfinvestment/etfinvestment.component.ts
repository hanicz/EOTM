import { Component, OnInit, ViewChild, ChangeDetectorRef } from '@angular/core';
import { ETFInvestment } from 'src/app/model/etfinvestment';
import { EtfService } from 'src/app/service/etf.service';
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
    selector: 'app-etfinvestment',
    templateUrl: './etfinvestment.component.html',
    styleUrls: ['./etfinvestment.component.css'],
    imports: [Bind, Toolbar, PrimeTemplate, ButtonDirective, Ripple, FileUpload, TableModule, InputText, Select, FormsModule, Tag, Dialog, CurrencyPipe, DatePipe]
})
export class EtfinvestmentComponent implements OnInit {

  investments: ETFInvestment[] = [];
  currencies: any[];
  statuses: any[];
  selectedInvestments: ETFInvestment[] = [];
  investmentDialog: boolean = false;
  investment: ETFInvestment = {} as ETFInvestment;
  @ViewChild('fileUpload') fileUpload: any;

  constructor(private etfService: EtfService, globals: Globals, private cdr: ChangeDetectorRef) {
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
    this.etfService.getInvestments().subscribe({
      next: (data) => {
        this.investments = data;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.log(error);
      }
    });
  }

  openNew() {
    this.investment = {} as ETFInvestment;
    this.investmentDialog = true;
  }

  hideDialog() {
    this.investmentDialog = false;
  }

  editInvestment(investment: ETFInvestment) {
    this.investment = { ...investment };
    this.investmentDialog = true;
  }

  deleteClicked() {
    let ids = '';
    this.selectedInvestments.forEach(t => {
      ids += t.id + ',';
    });
    this.deleteByIds(ids);
  }

  deleteByIds(ids: string) {
    this.etfService.deleteByIds(ids).subscribe({
      next: () => {
        this.selectedInvestments = [];
        this.fetchData();
      }
    });
  }

  download() {
    this.etfService.download().subscribe({
      next: (data) => {
        let fileName = 'etfinvestments.csv';
        let a = document.createElement('a');
        a.href = window.URL.createObjectURL(data as Blob);
        a.download = fileName;
        a.click();
      }
    });
  }

  saveInvestment() {
    if (this.investment.id === undefined) {
      this.etfService.create(this.investment).subscribe({
        next: () => {
          this.fetchData();
          this.investmentDialog = false;
        }
      });
    } else {
      this.etfService.update(this.investment).subscribe({
        next: () => {
          this.fetchData();
          this.investmentDialog = false;
        }
      });
    }
  }

  onUpload(event: any) {
    for (let file of event.files) {
      this.etfService.uploadCSV(file).subscribe({
        next: () => {
          this.fetchData();
          this.fileUpload.clear();
        }
      });
    }
  }
}
