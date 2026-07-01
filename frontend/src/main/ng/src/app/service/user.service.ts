import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { User } from '../model/user';
import { ResourceHelper } from '../util/servicehelper';
import { tap, shareReplay } from 'rxjs/operators';
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

  private userUrl = `${environment.API_URL}/api/v1/user`;

  private cachedUser$?: Observable<User>;

  constructor(private http: HttpClient) { }

  loginUser(user: User) {
    const url = `${environment.API_URL}/login`;
    return this.http.post(url, JSON.stringify(user), {
      headers: this.headers,
      withCredentials: true,
      observe: 'response'
    }).pipe(tap(response => {
      console.log(<string>response.headers.get('token'));
      localStorage.setItem('token', <string>response.headers.get('token'));
      this.cachedUser$ = undefined;
    }));
  }

  getUserEmail() {
    if (!this.cachedUser$) {
      const url = `${this.userUrl}/me`;
      this.cachedUser$ = this.http.get<User>(url, {
        headers: this.helper.getHeadersWithToken()
      }).pipe(
        tap({ error: () => this.cachedUser$ = undefined }),
        shareReplay(1)
      );
    }
    return this.cachedUser$;
  };

  clearUserCache(): void {
    this.cachedUser$ = undefined;
  }

  validateToken() {
    return this.http.get(this.userUrl, {
      headers: this.helper.getHeadersWithToken(),
    });
  }

  changePassword(oldPassword: string, newPassword: string) {
    const url = `${this.userUrl}/password`;
    console.log(JSON.stringify({ oldPassword, newPassword }));
    return this.http.put(url, JSON.stringify({ oldPassword, newPassword }), {
      headers: this.helper.getHeadersWithToken()
    });
  }
}