import { Component, OnInit, ViewChild } from '@angular/core';
import { Investment } from '../../model/investment';
import { StockService } from '../../service/stock.service';
import { Globals } from '../../util/global';

@Component({
  selector: 'app-investment',
  templateUrl: './investment.component.html',
  styleUrls: ['./investment.component.css']
})
export class InvestmentComponent implements OnInit {

  investments: Investment[] = [];
  currencies: any[];
  statuses: any[];
  selectedInvestments: Investment[] = [];
  investmentDialog: boolean = false;
  investment: Investment = {} as Investment;
  @ViewChild('fileUpload') fileUpload: any;
  globals: Globals;

  constructor(private stockService: StockService, globals: Globals) {
    this.globals = globals;
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
    this.stockService.getInvestments().subscribe({
      next: (data) => {
        this.investments = data;
      },
      error: (error) => {
        console.log(error);
      }
    });
  }

  openNew() {
    this.investment = {} as Investment;
    this.investmentDialog = true;
  }

  hideDialog() {
    this.investmentDialog = false;
  }

  editInvestment(investment: Investment) {
    this.investment = { ...investment };
    this.investmentDialog = true;
  }

  deleteClicked() {
    let ids = '';
    this.selectedInvestments.forEach(t => {
      ids += t.investmentId + ',';
    });
    this.deleteByIds(ids);
  }

  deleteByIds(ids: string) {
    this.stockService.deleteByIds(ids).subscribe({
      next: () => {
        this.selectedInvestments = [];
        this.fetchData();
      }
    });
  }

  download() {
    this.stockService.download().subscribe({
      next: (data) => {
        let fileName = 'investments.csv';
        let a = document.createElement('a');
        a.href = window.URL.createObjectURL(data as Blob);
        a.download = fileName;
        a.click();
      }
    });
  }

  saveInvestment() {
    if (this.investment.investmentId === undefined) {
      this.stockService.create(this.investment).subscribe({
        next: () => {
          this.fetchData();
          this.investmentDialog = false;
        }
      });
    } else {
      this.stockService.update(this.investment).subscribe({
        next: () => {
          this.fetchData();
          this.investmentDialog = false;
        }
      });
    }
  }

  onUpload(event: any) {
    for (let file of event.files) {
      this.stockService.uploadCSV(file).subscribe({
        next: () => {
          this.fetchData();
          this.fileUpload.clear();
        }
      });
    }
  }
}
