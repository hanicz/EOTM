import { inject } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { Router } from '@angular/router';
import { throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

const LOGIN_ENDPOINT = '/login';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const setHeaders: Record<string, string> = {};

  const token = localStorage.getItem('token');
  if (token && !req.url.endsWith(LOGIN_ENDPOINT)) {
    setHeaders['Authorization'] = token;
  }

  if (!(req.body instanceof FormData)) {
    setHeaders['Content-Type'] = 'application/json';
    setHeaders['Accept'] = 'application/json';
  }

  return next(req.clone({ setHeaders })).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        localStorage.removeItem('token');
        router.navigate([LOGIN_ENDPOINT]);
      }
      return throwError(() => error);
    })
  );
};
