import { DestroyRef, Directive, ElementRef, afterNextRender, inject, input } from '@angular/core';

@Directive({
  selector: '[appAlignToTable]'
})
export class AlignToTableDirective {

  readonly appAlignToTable = input.required<HTMLElement>();
  private readonly element: HTMLElement = inject(ElementRef).nativeElement;
  private observer: ResizeObserver | undefined;

  constructor() {
    afterNextRender(() => {
      const main = this.appAlignToTable();
      this.observer = new ResizeObserver(() => this.align());
      this.observer.observe(main);
      this.observer.observe(this.element);
      this.align();
    });
    inject(DestroyRef).onDestroy(() => this.observer?.disconnect());
  }

  align(): void {
    const main = this.appAlignToTable();
    const table = main.querySelector('.p-datatable-table-container');
    const mainBox = main.getBoundingClientRect();
    const beside = this.element.getBoundingClientRect().left >= mainBox.right;
    const offset = beside && table ? Math.max(table.getBoundingClientRect().top - mainBox.top, 0) : 0;
    this.element.style.marginTop = `${Math.round(offset)}px`;
  }
}
