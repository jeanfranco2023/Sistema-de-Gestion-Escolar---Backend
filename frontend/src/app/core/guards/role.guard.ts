import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStore } from '../state/auth.store';
import { RolNombre } from '../models/auth.model';

export const roleGuard = (allowedRoles: RolNombre[]): CanActivateFn => () => {
  const store = inject(AuthStore);
  const router = inject(Router);

  if (store.hasAnyRole(allowedRoles)) {
    return true;
  }

  return router.parseUrl(store.homeRoute());
};
