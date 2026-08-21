import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FireProjection, FireProjectionInput } from '../model/fire';
import { ResourceHelper } from '../util/servicehelper';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class FireService {

  private helper = new ResourceHelper();

  private fireUrl = `${environment.API_URL}/api/v1/fire`;

  constructor(private http: HttpClient) { }

  project(input: FireProjectionInput) {
    return this.http.post<FireProjection>(`${this.fireUrl}/projection`, JSON.stringify(input), {
      headers: this.helper.getHeadersWithToken()
    });
  }

  downloadCsv(input: FireProjectionInput) {
    return this.http.post(`${this.fireUrl}/projection/csv`, JSON.stringify(input), {
      headers: this.helper.getHeadersWithToken(),
      responseType: 'blob'
    });
  }
}
