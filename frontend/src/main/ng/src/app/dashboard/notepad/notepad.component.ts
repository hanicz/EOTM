import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, merge, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';
import { Skeleton } from 'primeng/skeleton';
import { Textarea } from 'primeng/textarea';
import { NoteService } from '../../service/note.service';
import { Note } from '../../model/note';

const AUTOSAVE_DELAY_MS = 800;

@Component({
  selector: 'app-notepad',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, FormsModule, Skeleton, Textarea],
  templateUrl: './notepad.component.html',
  styleUrls: ['./notepad.component.css']
})
export class NotepadComponent implements OnInit, OnDestroy {

  readonly loading = signal(true);
  readonly content = signal('');
  readonly savedAt = signal<Date | null>(null);
  readonly failed = signal(false);

  private readonly typed = new Subject<string>();
  private readonly flushed = new Subject<string>();
  private persisted = '';

  constructor(private noteService: NoteService) {
    merge(this.typed.pipe(debounceTime(AUTOSAVE_DELAY_MS)), this.flushed).pipe(
      distinctUntilChanged(),
      switchMap(content => this.noteService.update({ content: content, updatedAt: null }).pipe(
        catchError(error => {
          console.log(error);
          return of(null);
        })
      ))
    ).subscribe(note => this.applySaved(note));
  }

  ngOnInit(): void {
    this.noteService.getNote().subscribe({
      next: note => {
        this.persisted = note.content ?? '';
        this.content.set(this.persisted);
        this.savedAt.set(note.updatedAt ? new Date(note.updatedAt) : null);
        this.loading.set(false);
      },
      error: error => {
        console.log(error);
        this.loading.set(false);
      }
    });
  }

  ngOnDestroy(): void {
    this.typed.complete();
    this.flushed.complete();
  }

  onInput(value: string): void {
    this.content.set(value);
    this.typed.next(value);
  }

  onBlur(): void {
    if (this.content() !== this.persisted) {
      this.flushed.next(this.content());
    }
  }

  private applySaved(note: Note | null): void {
    if (note === null) {
      this.failed.set(true);
      return;
    }
    this.persisted = note.content ?? '';
    this.savedAt.set(note.updatedAt ? new Date(note.updatedAt) : new Date());
    this.failed.set(false);
  }
}
