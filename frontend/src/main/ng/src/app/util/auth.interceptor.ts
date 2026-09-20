import { inject } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { Router } from '@angular/router';
import { throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

const LOGIN_ENDPOINT = '/login';

const AUTH_ENDPOINTS = '/api/v1/auth/';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const setHeaders: Record<string, string> = {};

  const preAuth = req.url.endsWith(LOGIN_ENDPOINT) || req.url.includes(AUTH_ENDPOINTS);
  const token = localStorage.getItem('token');
  if (token && !preAuth) {
    setHeaders['Authorization'] = token;
  }

  if (!(req.body instanceof FormData)) {
    setHeaders['Content-Type'] = 'application/json';
    setHeaders['Accept'] = 'application/json';
  }

  return next(req.clone({ setHeaders })).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !preAuth) {
        localStorage.removeItem('token');
        router.navigate([LOGIN_ENDPOINT]);
      }
      return throwError(() => error);
    })
  );
};
