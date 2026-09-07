import { ChangeDetectionStrategy, Component, OnInit, computed, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { Skeleton } from 'primeng/skeleton';
import { Tooltip } from 'primeng/tooltip';
import { EquityService } from '../../service/equity.service';
import { RSUGrant } from '../../model/equity';
import { SOON_IN_DAYS, dueInLabel } from '../../util/upcomingpayments';
import { buildUpcomingVests } from '../../util/upcomingvests';

const SKELETON_ROWS = 3;

@Component({
  selector: 'app-upcoming-vest',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, DecimalPipe, Skeleton, Tooltip],
  templateUrl: './upcoming-vest.component.html',
  styleUrls: ['./upcoming-vest.component.css']
})
export class UpcomingVestComponent implements OnInit {

  readonly loading = signal(true);
  readonly skeletonRows = new Array(SKELETON_ROWS).fill({});

  private readonly grants = signal<RSUGrant[]>([]);

  readonly vests = computed(() => buildUpcomingVests(this.grants()));

  constructor(private equityService: EquityService) { }

  ngOnInit(): void {
    this.equityService.getGrants().subscribe({
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

  soonLabel(days: number): string {
    return dueInLabel(days);
  }
}
