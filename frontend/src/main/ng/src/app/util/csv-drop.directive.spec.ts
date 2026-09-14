import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { CsvDropDirective } from './csv-drop.directive';

@Component({
  imports: [CsvDropDirective],
  template: `<div appCsvDrop (csvDropped)="received.push($event.files)"><span class="child"></span></div>`
})
class HostComponent {
  received: File[][] = [];
}

describe('CsvDropDirective', () => {
  let fixture: ComponentFixture<HostComponent>;
  let host: HostComponent;
  let element: HTMLElement;
  let directive: CsvDropDirective;

  const dragEvent = (type: string, files: File[] = [], types: string[] = ['Files'], relatedTarget: Node | null = null): Event => {
    const event = new Event(type, { bubbles: true, cancelable: true });
    Object.defineProperty(event, 'dataTransfer', { value: { types, files, dropEffect: 'none' } });
    Object.defineProperty(event, 'relatedTarget', { value: relatedTarget });
    return event;
  };

  const file = (name: string): File => new File(['a,b'], name);

  const isActive = (): boolean => {
    fixture.detectChanges();
    return element.classList.contains('csv-drop-active');
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [HostComponent] }).compileComponents();
    fixture = TestBed.createComponent(HostComponent);
    host = fixture.componentInstance;
    const debugElement = fixture.debugElement.query(By.directive(CsvDropDirective));
    element = debugElement.nativeElement;
    directive = debugElement.injector.get(CsvDropDirective);
    fixture.detectChanges();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('shows the overlay while files are dragged over', () => {
    element.dispatchEvent(dragEvent('dragenter'));
    expect(isActive()).toBe(true);
  });

  it('keeps the overlay when the drag moves onto a child', () => {
    element.dispatchEvent(dragEvent('dragenter'));
    element.dispatchEvent(dragEvent('dragleave', [], ['Files'], element.querySelector('.child')));
    expect(isActive()).toBe(true);
  });

  it('hides the overlay when the drag leaves the zone', () => {
    element.dispatchEvent(dragEvent('dragenter'));
    element.dispatchEvent(dragEvent('dragleave', [], ['Files'], document.body));
    expect(isActive()).toBe(false);
  });

  it('hides the overlay when the drag is cancelled without a leave event', () => {
    vi.useFakeTimers();
    element.dispatchEvent(dragEvent('dragenter'));
    element.dispatchEvent(dragEvent('dragover'));
    expect(isActive()).toBe(true);

    vi.advanceTimersByTime(CsvDropDirective.idleTimeout);
    expect(isActive()).toBe(false);
  });

  it('ignores drags that carry no files', () => {
    const event = dragEvent('dragenter', [], ['text/plain']);
    element.dispatchEvent(event);
    expect(isActive()).toBe(false);
    expect(event.defaultPrevented).toBe(false);
  });

  it('emits only csv files and hides the overlay', () => {
    const csv = file('sample.csv');
    element.dispatchEvent(dragEvent('dragenter'));
    element.dispatchEvent(dragEvent('drop', [csv, file('notes.txt')]));
    expect(host.received).toEqual([[csv]]);
    expect(isActive()).toBe(false);
  });

  it('emits nothing when no csv file is dropped', () => {
    element.dispatchEvent(dragEvent('drop', [file('notes.txt')]));
    expect(host.received).toEqual([]);
  });

  it('keeps only the first csv unless multiple is enabled', () => {
    const first = file('first.CSV');
    const second = file('second.csv');

    element.dispatchEvent(dragEvent('drop', [first, second]));
    expect(host.received).toEqual([[first]]);

    directive.multiple.set(true);
    element.dispatchEvent(dragEvent('drop', [first, second]));
    expect(host.received).toEqual([[first], [first, second]]);
  });
});
