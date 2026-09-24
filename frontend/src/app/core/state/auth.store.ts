import { computed, inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, tap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { AuthResponseDto, LoginRequestDto, RolNombre, UserResponseDto } from '../models/auth.model';

@Injectable({
  providedIn: 'root'
})
export class AuthStore {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  private getStoredUser(): UserResponseDto | null {
    try {
      const u = localStorage.getItem('user');
      return u ? JSON.parse(u) : null;
    } catch {
      return null;
    }
  }

  readonly user = signal<UserResponseDto | null>(this.getStoredUser());
  readonly accessToken = signal<string | null>(localStorage.getItem('accessToken'));
  readonly isLoading = signal(false);
  readonly error = signal<string | null>(null);

  readonly isAuthenticated = computed(() => !!this.accessToken() && !!this.user());
  readonly roles = computed(() => this.user()?.roles.map(r => r.codigo) ?? []);

  hasRole(role: RolNombre): boolean {
    return this.roles().includes(role);
  }

  hasAnyRole(roles: RolNombre[]): boolean {
    return roles.some(r => this.hasRole(r));
  }

  login(credentials: LoginRequestDto) {
    this.isLoading.set(true);
    this.error.set(null);

    return this.authService.login(credentials).pipe(
      tap((response) => {
        this.setSession(response.data);
        this.isLoading.set(false);
        this.router.navigate([this.homeRoute()]).catch(() => {});
      }),
      catchError((err) => {
        this.isLoading.set(false);
        const msg = err.error?.message ?? 'Credenciales incorrectas o servidor no disponible';
        this.error.set(msg);
        return throwError(() => err);
      })
    );
  }

  setSession(data: AuthResponseDto) {
    this.accessToken.set(data.accessToken);
    this.user.set(data.user);
    if (data.accessToken) {
      localStorage.setItem('accessToken', data.accessToken);
    }
    if (data.user) {
      localStorage.setItem('user', JSON.stringify(data.user));
    }
    if (data.refreshToken) {
      localStorage.setItem('refreshToken', data.refreshToken);
    }
  }

  logout() {
    this.accessToken.set(null);
    this.user.set(null);
    localStorage.removeItem('accessToken');
    localStorage.removeItem('user');
    localStorage.removeItem('refreshToken');
    this.router.navigate(['/auth/login']).catch(() => {});
  }

  homeRoute(): string {
    if (this.hasAnyRole(['DIRECCION', 'SECRETARIA'])) return '/admin/dashboard';
    if (this.hasRole('APODERADO')) return '/admin/tesoreria';
    if (this.hasAnyRole(['AUXILIAR', 'TUTOR'])) return '/admin/asistencia';
    if (this.hasRole('DOCENTE')) return '/admin/calificaciones';
    return '/admin/perfil';
  }
}
