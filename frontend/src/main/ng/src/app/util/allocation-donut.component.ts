import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { DEFAULT_CURRENCY } from '../model/currency';
import { AllocationItem, AllocationSlice, DONUT_RADIUS, buildAllocation } from './allocation';

@Component({
  selector: 'app-allocation-donut',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DecimalPipe],
  templateUrl: './allocation-donut.component.html',
  styleUrls: ['./allocation-donut.component.css']
})
export class AllocationDonutComponent {

  readonly items = input<AllocationItem[]>([]);
  readonly currency = input<string>(DEFAULT_CURRENCY);
  readonly centerLabel = input<string>('Total worth');
  readonly total = input<number | null>(null);
  readonly maxSlices = input<number>(8);
  readonly colors = input<{ [label: string]: string }>({});
  readonly compact = input<boolean>(false);
  readonly stacked = input<boolean>(false);
  readonly clickable = input<boolean>(false);
  readonly sliceClick = output<string>();

  readonly radius = DONUT_RADIUS;
  readonly hoveredLabel = signal<string | null>(null);

  readonly slices = computed<AllocationSlice[]>(() => buildAllocation(this.items(), this.maxSlices(), this.colors()));

  readonly shownTotal = computed(() => this.total() ?? this.slices().reduce((sum, slice) => sum + slice.value, 0));

  readonly holdingCount = computed(() => new Set(this.items().filter(item => item.value > 0).map(item => item.label)).size);

  readonly hovered = computed(() => this.slices().find(slice => slice.label === this.hoveredLabel()) ?? null);

  hover(label: string | null): void {
    this.hoveredLabel.set(label);
  }

  select(label: string): void {
    if (this.clickable()) {
      this.sliceClick.emit(label);
    }
  }

  formatCompact(value: number): string {
    try {
      return new Intl.NumberFormat(undefined, {
        style: 'currency',
        currency: this.currency(),
        notation: 'compact',
        maximumFractionDigits: 1
      }).format(value);
    } catch {
      return value.toFixed(0);
    }
  }
}
