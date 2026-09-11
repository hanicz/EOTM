import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FireProjection, FireProjectionInput, FireSnapshot } from '../model/fire';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class FireService {

  private fireUrl = `${environment.API_URL}/api/v1/fire`;

  constructor(private http: HttpClient) { }

  getSnapshot(currency: string) {
    return this.http.get<FireSnapshot>(`${this.fireUrl}/snapshot?currency=${encodeURIComponent(currency)}`);
  }

  project(input: FireProjectionInput) {
    return this.http.post<FireProjection>(`${this.fireUrl}/projection`, JSON.stringify(input));
  }

  downloadCsv(input: FireProjectionInput) {
    return this.http.post(`${this.fireUrl}/projection/csv`, JSON.stringify(input), {
      responseType: 'blob'
    });
  }
}
