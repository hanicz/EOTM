import { Component, OnInit, Output, EventEmitter } from '@angular/core';
import { Globals } from '../../util/global';
import { ETFInvestment } from 'src/app/model/etfinvestment';
import { EtfService } from 'src/app/service/etf.service';

@Component({
  selector: 'app-etfholding',
  templateUrl: './etfholding.component.html',
  styleUrls: ['./etfholding.component.css']
})
export class EtfholdingComponent implements OnInit {

  investments: ETFInvestment[] = [];
  @Output() dataLoaded = new EventEmitter<ETFInvestment[]>();
  globals: Globals;

  investmentsLoading: boolean = true;

  constructor(private etfService: EtfService, globals: Globals) {
    this.globals = globals;

    this.fetchData();
    globals.stockCurrencyChange.subscribe(value => {
      this.fetchData();
    });
  }

  ngOnInit(): void {
  }

  private fetchData(): void {
    this.etfService.getHolding(this.globals.etfCurrency).subscribe({
      next: (data) => {
        this.investmentsLoading = false;
        this.investments = data;
        this.dataLoaded.emit(this.investments);
      },
      error: (error) => {
        console.log(error);
      }
    });
  }
}
