import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { Account } from '../model/account';

@Injectable({
  providedIn: 'root'
})
export class AccountService {

  private accountUrl = `${environment.API_URL}/api/v1/account`;

  constructor(private http: HttpClient) { }

  getAccounts() {
    const url = `${this.accountUrl}`;
    return this.http.get<Account[]>(url);
  };

  deleteAccount(id: number) {
    const url = `${this.accountUrl}/${id}`;
    return this.http.delete(url);
  }

  createAccount(account: Account) {
    const url = `${this.accountUrl}`;
    return this.http.post<Account>(url, { accountName: account.accountName, creationDate: account.creationDate });
  }

  updateAccount(id: number, account: Account) {
    const url = `${this.accountUrl}/${id}`;
    return this.http.put<Account>(url, { accountName: account.accountName, creationDate: account.creationDate });
  }
}
