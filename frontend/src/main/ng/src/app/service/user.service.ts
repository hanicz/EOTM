import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { User } from '../model/user';
import { ResourceHelper } from '../util/servicehelper';
import { tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class UserService {

  private helper = new ResourceHelper();

  private headers = new HttpHeaders({
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  });

  private userUrl = `${environment.API_URL}/user`;

  constructor(private http: HttpClient) { }

  loginUser(user: User) {
    const url = `${environment.API_URL}/login`;
    return this.http.post(url, JSON.stringify(user), {
      headers: this.headers,
      withCredentials: true,
      observe: 'response'
    }).pipe(tap(response => {
      localStorage.setItem('token', <string>response.headers.get('token'));
    }));
  }

  validateToken() {
    return this.http.get(this.userUrl, {
      headers: this.helper.getHeadersWithToken(),
    });
  }
}