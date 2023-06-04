import { Component, OnInit, Output, EventEmitter } from '@angular/core';
import { Globals } from '../../util/global';
import { ForexTransaction } from 'src/app/model/forextransaction';
import { ForexService } from 'src/app/service/forex.service';

@Component({
  selector: 'app-forexholding',
  templateUrl: './forexholding.component.html',
  styleUrls: ['./forexholding.component.css']
})
export class ForexholdingComponent {

  forexTransactions: ForexTransaction[] = [];
  @Output() dataLoaded = new EventEmitter<ForexTransaction[]>();
  globals: Globals;

  constructor(private forexService: ForexService, globals: Globals) {
    this.globals = globals;

    this.fetchData();
    globals.stockCurrencyChange.subscribe(value => {
      this.fetchData();
    });
  }

  ngOnInit(): void {
  }

  private fetchData(): void {
    this.forexService.getHolding().subscribe({
      next: (data) => {
        this.forexTransactions = data;
        this.dataLoaded.emit(this.forexTransactions);
      },
      error: (error) => {
        console.log(error);
      }
    });
  }

}
