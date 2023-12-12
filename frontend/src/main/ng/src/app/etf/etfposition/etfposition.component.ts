import { Component, OnInit } from '@angular/core';
import { EtfService } from 'src/app/service/etf.service';
import { ETFInvestment } from 'src/app/model/etfinvestment';
import { Globals } from '../../util/global';

@Component({
  selector: 'app-etfposition',
  templateUrl: './etfposition.component.html',
  styleUrls: ['./etfposition.component.css']
})
export class EtfpositionComponent implements OnInit {

  investments: ETFInvestment[] = [];
  myMath = Math;
  globals: Globals;

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
    this.etfService.getPositions().subscribe({
      next: (data) => {
        this.investments = data;
      },
      error: (error) => {
        console.log(error);
      }
    });
  }
}
