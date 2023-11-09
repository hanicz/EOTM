import { Component, OnInit } from '@angular/core';
import { StockAlert } from '../model/stockalert';
import { AlertService } from '../service/alert.service';

@Component({
  selector: 'app-alert',
  templateUrl: './alert.component.html',
  styleUrls: ['./alert.component.css']
})
export class AlertComponent implements OnInit {

  stockAlerts: StockAlert[] = [];
  alertsLoading: boolean = true;

  constructor(private alertService: AlertService) {
    this.fetchData();
  }

  ngOnInit(): void {
  }

  fetchData(): void {
    this.alertService.getAlerts().subscribe({
      next: (data) => {
        this.alertsLoading = false;
        this.stockAlerts = data;
      },
      error: (error) => {
        console.log(error);
      }
    });
  }

  deleteAlert(alert: StockAlert) {
    console.log('itten');
    this.alertService.deleteStockAlert(alert.id).subscribe({
      next: () => {
        this.fetchData();
      },
      error: (error) => {
        console.log(error);
      }
    });
  }
}
