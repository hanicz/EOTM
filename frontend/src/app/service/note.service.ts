import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Note } from '../model/note';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class NoteService {

  private noteUrl = `${environment.API_URL}/api/v1/note`;

  constructor(private http: HttpClient) { }

  getNote() {
    return this.http.get<Note>(this.noteUrl);
  }

  update(note: Note) {
    return this.http.put<Note>(this.noteUrl, JSON.stringify(note));
  }
}
