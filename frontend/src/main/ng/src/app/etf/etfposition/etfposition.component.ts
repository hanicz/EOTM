import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { EtfService } from 'src/app/service/etf.service';
import { ETFInvestment } from 'src/app/model/etfinvestment';
import { Bind } from 'primeng/bind';
import { TableModule } from 'primeng/table';
import { PrimeTemplate } from 'primeng/api';
import { InputText } from 'primeng/inputtext';
import { DecimalPipe, CurrencyPipe } from '@angular/common';
import { TickerIdentityComponent } from '../../util/ticker-identity.component';
import { DeltaComponent } from '../../util/delta.component';

@Component({
    selector: 'app-etfposition',
    templateUrl: './etfposition.component.html',
    styleUrls: ['./etfposition.component.css'],
    imports: [Bind, TableModule, PrimeTemplate, InputText, DecimalPipe, CurrencyPipe, TickerIdentityComponent, DeltaComponent]
})
export class EtfpositionComponent implements OnInit {

  investments: ETFInvestment[] = [];

  constructor(private etfService: EtfService, private cdr: ChangeDetectorRef) {
    this.fetchData();
  }

  ngOnInit(): void {
  }

  refresh(): void {
    this.fetchData();
  }

  private fetchData(): void {
    this.etfService.getPositions().subscribe({
      next: (data) => {
        this.investments = data;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.log(error);
      }
    });
  }
}
