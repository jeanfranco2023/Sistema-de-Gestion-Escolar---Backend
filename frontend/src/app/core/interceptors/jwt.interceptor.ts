import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, finalize, map, Observable, shareReplay, switchMap, tap, throwError } from 'rxjs';
import { AuthStore } from '../state/auth.store';
import { AuthService } from '../services/auth.service';

let refreshInFlight: Observable<string> | null = null;

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const store = inject(AuthStore);
  const auth = inject(AuthService);

  // Skip auth header for authentication endpoints
  if (req.url.includes('/api/v1/auth/')) {
    return next(req);
  }

  const token = store.accessToken();
  const authReq = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401) return throwError(() => error);
      const refreshToken = localStorage.getItem('refreshToken');
      if (!refreshToken) { store.logout(); return throwError(() => error); }
      if (!refreshInFlight) {
        refreshInFlight = auth.refreshToken({ refreshToken }).pipe(
          tap(response => store.setSession(response.data)),
          map(response => response.data.accessToken),
          catchError(refreshError => { store.logout(); return throwError(() => refreshError); }),
          finalize(() => { refreshInFlight = null; }),
          shareReplay(1)
        );
      }
      return refreshInFlight.pipe(
        switchMap(newToken => next(req.clone({ setHeaders: { Authorization: `Bearer ${newToken}` } }))),
        catchError(retryError => {
          if (retryError.status === 401) store.logout();
          return throwError(() => retryError);
        })
      );
    })
  );
};
