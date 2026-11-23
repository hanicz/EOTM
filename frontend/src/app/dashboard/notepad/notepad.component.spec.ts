import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { NotepadComponent } from './notepad.component';
import { environment } from '../../../environments/environment';

describe('NotepadComponent', () => {
  let component: NotepadComponent;
  let fixture: ComponentFixture<NotepadComponent>;
  let http: HttpTestingController;

  const noteUrl = `${environment.API_URL}/api/v1/note`;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NotepadComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations()]
    })
      .compileComponents();

    fixture = TestBed.createComponent(NotepadComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('loads the stored note', () => {
    fixture.detectChanges();
    http.expectOne(noteUrl).flush({ content: 'Rebalance in October', updatedAt: '2026-03-14T09:30:00' });

    expect(component.content()).toBe('Rebalance in October');
    expect(component.savedAt()).toEqual(new Date('2026-03-14T09:30:00'));
    expect(component.loading()).toBeFalsy();
  });

  it('starts empty when nothing has been written yet', () => {
    fixture.detectChanges();
    http.expectOne(noteUrl).flush({ content: '', updatedAt: null });

    expect(component.content()).toBe('');
    expect(component.savedAt()).toBeNull();
  });

  it('saves what was typed when the field loses focus', () => {
    fixture.detectChanges();
    http.expectOne(noteUrl).flush({ content: '', updatedAt: null });

    component.onInput('Check the dividend date');
    component.onBlur();

    const request = http.expectOne(noteUrl);
    expect(request.request.method).toBe('PUT');
    expect(JSON.parse(request.request.body)).toEqual({ content: 'Check the dividend date', updatedAt: null });

    request.flush({ content: 'Check the dividend date', updatedAt: '2026-03-14T10:00:00' });
    expect(component.savedAt()).toEqual(new Date('2026-03-14T10:00:00'));
    expect(component.failed()).toBeFalsy();
  });

  it('does not save on blur when nothing changed', () => {
    fixture.detectChanges();
    http.expectOne(noteUrl).flush({ content: 'Rebalance in October', updatedAt: '2026-03-14T09:30:00' });

    component.onBlur();

    http.expectNone(noteUrl);
  });

  it('reports a failed save and keeps accepting later edits', () => {
    fixture.detectChanges();
    http.expectOne(noteUrl).flush({ content: '', updatedAt: null });

    component.onInput('First attempt');
    component.onBlur();
    http.expectOne(noteUrl).flush('nope', { status: 500, statusText: 'Server Error' });

    expect(component.failed()).toBeTruthy();

    component.onInput('Second attempt');
    component.onBlur();
    const retry = http.expectOne(noteUrl);
    retry.flush({ content: 'Second attempt', updatedAt: '2026-03-14T10:05:00' });

    expect(component.failed()).toBeFalsy();
    expect(component.savedAt()).toEqual(new Date('2026-03-14T10:05:00'));
  });
});
