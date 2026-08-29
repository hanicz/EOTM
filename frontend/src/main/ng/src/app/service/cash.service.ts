import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Cash } from '../model/cash';
import { ResourceHelper } from '../util/servicehelper';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class CashService {

  private helper = new ResourceHelper();

  private cashUrl = `${environment.API_URL}/api/v1/cash`;

  constructor(private http: HttpClient) { }

  getCash() {
    return this.http.get<Cash>(this.cashUrl, {
      headers: this.helper.getHeadersWithToken()
    });
  }

  update(cash: Cash) {
    return this.http.put<Cash>(this.cashUrl, JSON.stringify(cash), {
      headers: this.helper.getHeadersWithToken()
    });
  }
}
