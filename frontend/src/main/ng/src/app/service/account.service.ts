import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ResourceHelper } from '../util/servicehelper';
import { environment } from '../../environments/environment';
import { Account } from '../model/account';

@Injectable({
  providedIn: 'root'
})
export class AccountService {

  private helper = new ResourceHelper();

  private accountUrl = `${environment.API_URL}/api/v1/account`;

  constructor(private http: HttpClient) { }

  getAccounts() {
    const url = `${this.accountUrl}`;
    return this.http.get<Account[]>(url, {
      headers: this.helper.getHeadersWithToken()
    });
  };

  deleteAccount(id: number) {
    const url = `${this.accountUrl}/${id}`;
    return this.http.delete(url, {
      headers: this.helper.getHeadersWithToken()
    });
  }

  createAccount(account: Account) {
    const url = `${this.accountUrl}`;
    return this.http.post(url, account, {
      headers: this.helper.getHeadersWithToken()
    });
  }
}
