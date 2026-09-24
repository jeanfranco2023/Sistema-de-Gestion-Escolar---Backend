import { inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AuthService } from './auth.service';
import { AuthStore } from '../state/auth.store';

/**
 * App Initializer executed within Angular's EnvironmentInjector during bootstrap.
 * Restores user session if a valid refresh token exists in localStorage.
 * Always resolves so application bootstrap is never blocked or frozen.
 */
export function initAuthApp(): Promise<void> {
  const authService = inject(AuthService);
  const authStore = inject(AuthStore);
  const refreshToken = localStorage.getItem('refreshToken');

  if (!refreshToken) {
    return Promise.resolve();
  }

  return firstValueFrom(authService.refreshToken({ refreshToken }))
    .then((response) => {
      if (response && response.data) {
        authStore.setSession(response.data);
      }
    })
    .catch(() => {
      localStorage.removeItem('refreshToken');
      return Promise.resolve();
    });
}
