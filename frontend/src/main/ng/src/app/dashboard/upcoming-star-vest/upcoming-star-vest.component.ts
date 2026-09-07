import { ChangeDetectionStrategy, Component, OnInit, computed, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { Skeleton } from 'primeng/skeleton';
import { EquityService } from '../../service/equity.service';
import { STARGrant } from '../../model/equity';
import { SOON_IN_DAYS, dueInLabel } from '../../util/upcomingpayments';
import { buildUpcomingStarVests } from '../../util/upcomingstarvests';

const SKELETON_ROWS = 3;

@Component({
  selector: 'app-upcoming-star-vest',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, DecimalPipe, Skeleton],
  templateUrl: './upcoming-star-vest.component.html',
  styleUrls: ['./upcoming-star-vest.component.css']
})
export class UpcomingStarVestComponent implements OnInit {

  readonly loading = signal(true);
  readonly skeletonRows = new Array(SKELETON_ROWS).fill({});

  private readonly grants = signal<STARGrant[]>([]);

  readonly vests = computed(() => buildUpcomingStarVests(this.grants()));

  constructor(private equityService: EquityService) { }

  ngOnInit(): void {
    this.equityService.getStarGrants().subscribe({
      next: data => {
        this.grants.set(data.items);
        this.loading.set(false);
      },
      error: error => {
        console.log(error);
        this.loading.set(false);
      }
    });
  }

  isSoon(days: number): boolean {
    return days <= SOON_IN_DAYS;
  }

  daysLabel(days: number): string {
    return dueInLabel(days);
  }
}
