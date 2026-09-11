import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { User } from '../model/user';
import { UserProfile } from '../model/userprofile';
import { DEFAULT_CURRENCY } from '../model/currency';
import { map, tap, shareReplay } from 'rxjs/operators';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class UserService {

  private userUrl = `${environment.API_URL}/api/v1/user`;

  private cachedUser$?: Observable<UserProfile>;

  constructor(private http: HttpClient) { }

  loginUser(user: User) {
    const url = `${environment.API_URL}/login`;
    return this.http.post(url, JSON.stringify(user), {
      withCredentials: true,
      observe: 'response'
    }).pipe(tap(response => {
      localStorage.setItem('token', <string>response.headers.get('token'));
      this.cachedUser$ = undefined;
    }));
  }

  getCurrentUser(): Observable<UserProfile> {
    if (!this.cachedUser$) {
      const url = `${this.userUrl}/me`;
      this.cachedUser$ = this.http.get<UserProfile>(url).pipe(
        tap({ error: () => this.cachedUser$ = undefined }),
        shareReplay(1)
      );
    }
    return this.cachedUser$;
  };

  getPreferredCurrency(): Observable<string> {
    return this.getCurrentUser().pipe(map(user => user.preferredCurrency ?? DEFAULT_CURRENCY));
  }

  updatePreferences(preferredCurrency: string) {
    const url = `${this.userUrl}/preferences`;
    return this.http.put<UserProfile>(url, JSON.stringify({ preferredCurrency })).pipe(
      tap(user => this.cachedUser$ = of(user))
    );
  }

  clearUserCache(): void {
    this.cachedUser$ = undefined;
  }

  validateToken() {
    return this.http.get(this.userUrl);
  }

  exportAccount() {
    return this.http.get(`${this.userUrl}/export`, {
      responseType: 'blob'
    });
  }

  changePassword(oldPassword: string, newPassword: string) {
    const url = `${this.userUrl}/password`;
    return this.http.put(url, JSON.stringify({ oldPassword, newPassword }));
  }
}
