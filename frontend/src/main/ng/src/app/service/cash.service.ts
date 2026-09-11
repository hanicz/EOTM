import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Cash } from '../model/cash';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class CashService {

  private cashUrl = `${environment.API_URL}/api/v1/cash`;

  constructor(private http: HttpClient) { }

  getCash() {
    return this.http.get<Cash>(this.cashUrl);
  }

  update(cash: Cash) {
    return this.http.put<Cash>(this.cashUrl, JSON.stringify(cash));
  }
}
