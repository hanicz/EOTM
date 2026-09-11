import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Pension } from '../model/pension';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class PensionService {

  private pensionUrl = `${environment.API_URL}/api/v1/pension`;

  constructor(private http: HttpClient) { }

  getPension() {
    return this.http.get<Pension>(this.pensionUrl);
  }

  update(pension: Pension) {
    return this.http.put<Pension>(this.pensionUrl, JSON.stringify(pension));
  }
}
