import { DestroyRef, Directive, ElementRef, inject, output, signal } from '@angular/core';

@Directive({
  selector: '[appCsvDrop]',
  host: {
    class: 'csv-drop-zone',
    '[class.csv-drop-active]': 'dragging()',
    '(dragenter)': 'onDragOver($event)',
    '(dragover)': 'onDragOver($event)',
    '(dragleave)': 'onDragLeave($event)',
    '(drop)': 'onDrop($event)'
  }
})
export class CsvDropDirective {

  static readonly idleTimeout = 150;

  readonly multiple = signal(false);
  readonly dragging = signal(false);
  readonly csvDropped = output<{ files: File[] }>();
  private readonly element: HTMLElement = inject(ElementRef).nativeElement;
  private idleTimer: ReturnType<typeof setTimeout> | undefined;

  constructor() {
    inject(DestroyRef).onDestroy(() => clearTimeout(this.idleTimer));
  }

  onDragOver(event: DragEvent): void {
    if (!this.hasFiles(event)) return;
    event.preventDefault();
    event.dataTransfer!.dropEffect = 'copy';
    this.dragging.set(true);
    clearTimeout(this.idleTimer);
    this.idleTimer = setTimeout(() => this.stop(), CsvDropDirective.idleTimeout);
  }

  onDragLeave(event: DragEvent): void {
    const target = event.relatedTarget as Node | null;
    if (!target || !this.element.contains(target)) this.stop();
  }

  onDrop(event: DragEvent): void {
    if (!this.hasFiles(event)) return;
    event.preventDefault();
    this.stop();
    const csvFiles = Array.from(event.dataTransfer!.files)
      .filter(file => file.name.toLowerCase().endsWith('.csv'));
    const files = this.multiple() ? csvFiles : csvFiles.slice(0, 1);
    if (files.length) this.csvDropped.emit({ files });
  }

  private stop(): void {
    clearTimeout(this.idleTimer);
    this.dragging.set(false);
  }

  private hasFiles(event: DragEvent): boolean {
    return Array.from(event.dataTransfer?.types ?? []).includes('Files');
  }
}
