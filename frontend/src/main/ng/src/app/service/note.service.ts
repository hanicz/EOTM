import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Note } from '../model/note';
import { ResourceHelper } from '../util/servicehelper';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class NoteService {

  private helper = new ResourceHelper();

  private noteUrl = `${environment.API_URL}/api/v1/note`;

  constructor(private http: HttpClient) { }

  getNote() {
    return this.http.get<Note>(this.noteUrl, {
      headers: this.helper.getHeadersWithToken()
    });
  }

  update(note: Note) {
    return this.http.put<Note>(this.noteUrl, JSON.stringify(note), {
      headers: this.helper.getHeadersWithToken()
    });
  }
}
