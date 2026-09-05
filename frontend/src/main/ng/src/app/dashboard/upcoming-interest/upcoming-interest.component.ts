import { ChangeDetectionStrategy, Component, OnInit, computed, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { Skeleton } from 'primeng/skeleton';
import { SecurityService } from '../../service/security.service';
import { SecurityTransaction } from '../../model/securityTransaction';
import { buildUpcomingPayments } from '../../util/upcomingpayments';

const PAYMENT_LIMIT = 3;

@Component({
  selector: 'app-upcoming-interest',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CurrencyPipe, DatePipe, Skeleton],
  templateUrl: './upcoming-interest.component.html',
  styleUrls: ['./upcoming-interest.component.css']
})
export class UpcomingInterestComponent implements OnInit {

  readonly loading = signal(true);
  readonly skeletonRows = new Array(PAYMENT_LIMIT).fill({});

  private readonly holdings = signal<SecurityTransaction[]>([]);

  readonly payments = computed(() => buildUpcomingPayments(this.holdings(), PAYMENT_LIMIT));

  constructor(private securityService: SecurityService) { }

  ngOnInit(): void {
    this.securityService.getHolding().subscribe({
      next: data => {
        this.holdings.set(data);
        this.loading.set(false);
      },
      error: error => {
        console.log(error);
        this.loading.set(false);
      }
    });
  }
}
