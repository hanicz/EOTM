import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { EtfService } from 'src/app/service/etf.service';
import { ETFInvestment } from 'src/app/model/etfinvestment';
import { Bind } from 'primeng/bind';
import { TableModule } from 'primeng/table';
import { PrimeTemplate } from 'primeng/api';
import { Tag } from 'primeng/tag';
import { DecimalPipe, CurrencyPipe } from '@angular/common';

@Component({
    selector: 'app-etfposition',
    templateUrl: './etfposition.component.html',
    styleUrls: ['./etfposition.component.css'],
    imports: [Bind, TableModule, PrimeTemplate, Tag, DecimalPipe, CurrencyPipe]
})
export class EtfpositionComponent implements OnInit {

  investments: ETFInvestment[] = [];
  myMath = Math;

  constructor(private etfService: EtfService, private cdr: ChangeDetectorRef) {
    this.fetchData();
  }

  ngOnInit(): void {
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
