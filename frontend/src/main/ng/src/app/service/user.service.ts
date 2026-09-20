import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { User } from '../model/user';
import { UserProfile } from '../model/userprofile';
import { LoginResult } from '../model/loginresult';
import { TotpSetup } from '../model/totpsetup';
import { TotpStatus } from '../model/totpstatus';
import { DEFAULT_CURRENCY } from '../model/currency';
import { map, tap, shareReplay } from 'rxjs/operators';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class UserService {

  private userUrl = `${environment.API_URL}/api/v1/user`;

  private authUrl = `${environment.API_URL}/api/v1/auth`;

  private cachedUser$?: Observable<UserProfile>;

  constructor(private http: HttpClient) { }

  loginUser(user: User): Observable<LoginResult> {
    const url = `${environment.API_URL}/login`;
    return this.http.post<LoginResult>(url, JSON.stringify(user), {
      withCredentials: true,
      observe: 'response'
    }).pipe(
      tap(response => this.storeSession(response)),
      map(response => response.body ?? { mfaRequired: false })
    );
  }

  verifyTotp(mfaToken: string, code: string): Observable<void> {
    const url = `${this.authUrl}/2fa/verify`;
    return this.http.post(url, JSON.stringify({ mfaToken, code }), {
      observe: 'response'
    }).pipe(
      tap(response => this.storeSession(response)),
      map(() => undefined)
    );
  }

  getTwoFactorStatus(): Observable<TotpStatus> {
    return this.http.get<TotpStatus>(`${this.userUrl}/2fa`);
  }

  startTwoFactorSetup(): Observable<TotpSetup> {
    return this.http.post<TotpSetup>(`${this.userUrl}/2fa/setup`, null);
  }

  confirmTwoFactor(code: string) {
    return this.http.post(`${this.userUrl}/2fa/confirm`, JSON.stringify({ code }));
  }

  disableTwoFactor(password: string) {
    return this.http.delete(`${this.userUrl}/2fa`, { body: JSON.stringify({ password }) });
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

  private storeSession(response: HttpResponse<unknown>): void {
    const token = response.headers.get('token');
    if (!token) {
      return;
    }
    localStorage.setItem('token', token);
    this.cachedUser$ = undefined;
  }
}
