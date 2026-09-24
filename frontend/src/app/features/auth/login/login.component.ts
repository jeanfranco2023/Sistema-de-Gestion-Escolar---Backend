import { Component, TemplateRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthStore } from '../../../core/state/auth.store';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    FormsModule,
    RouterLink,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule
  ],
  template: `
    <main class="grid min-h-screen bg-[#f4f6fb] lg:grid-cols-[1.05fr_0.95fr]">
      <section class="login-brand-panel relative hidden min-h-screen overflow-hidden px-10 py-12 text-white lg:flex lg:flex-col lg:justify-between xl:px-16 xl:py-14">
        <div class="absolute -right-28 top-1/4 h-[28rem] w-[28rem] rounded-full border border-white/[0.08]"></div>
        <div class="absolute -right-10 top-[29%] h-[22rem] w-[22rem] rounded-full border border-white/[0.08]"></div>
        <div class="absolute -bottom-36 -left-28 h-[28rem] w-[28rem] rounded-full bg-blue-400/[0.08] blur-3xl"></div>

        <a routerLink="/auth/login" class="relative z-10 flex w-fit items-center gap-3" aria-label="I.E.P. Shuji Kitamura">
          <span class="brand-crest !h-12 !w-12 !text-base">SK</span>
          <span>
            <span class="block text-sm font-bold tracking-wide">I.E.P. Shuji Kitamura</span>
            <span class="mt-0.5 block text-[10px] font-medium tracking-[0.18em] text-blue-100/70">PORTAL INSTITUCIONAL</span>
          </span>
        </a>

        <div class="relative z-10 my-14 max-w-xl">
          <p class="mb-5 inline-flex items-center gap-2 rounded-full border border-white/15 bg-white/[0.06] px-3.5 py-2 text-[10px] font-semibold uppercase tracking-[0.16em] text-blue-100">
            <span class="h-1.5 w-1.5 rounded-full bg-[#d3ad70]"></span> Gestión educativa, con propósito
          </p>
          <h1 class="max-w-lg text-4xl font-semibold leading-[1.12] tracking-[-0.04em] text-white xl:text-5xl">
            Un espacio para aprender, crecer y avanzar.
          </h1>
          <p class="mt-5 max-w-md text-sm leading-7 text-blue-100/75">
            Accede a las herramientas académicas y administrativas de nuestra comunidad educativa desde un único lugar.
          </p>
          <div class="mt-10 flex items-center gap-4">
            <span class="h-px w-12 bg-[#c49a58]"></span>
            <p class="text-xs font-medium tracking-wide text-blue-100/70">Excelencia · Disciplina · Saber</p>
          </div>
        </div>

        <p class="relative z-10 text-[10px] tracking-wide text-blue-100/45">© 2026 I.E.P. Shuji Kitamura · Todos los derechos reservados</p>
      </section>

      <section class="flex min-h-screen min-w-0 items-center justify-center px-5 py-10 sm:px-8 lg:px-12">
        <div class="w-full min-w-0 max-w-[440px]">
          <a routerLink="/auth/login" class="mb-8 flex items-center gap-3 lg:hidden">
            <span class="brand-crest">SK</span>
            <span>
              <span class="block text-sm font-bold text-slate-800">I.E.P. Shuji Kitamura</span>
              <span class="mt-0.5 block text-[10px] font-semibold tracking-[0.15em] text-slate-500">PORTAL INSTITUCIONAL</span>
            </span>
          </a>

          <div class="w-full min-w-0 rounded-[28px] border border-white bg-white p-5 shadow-[0_24px_80px_rgba(23,43,77,0.10)] sm:p-9">
            <div class="mb-8">
              <p class="text-[10px] font-bold uppercase tracking-[0.18em] text-[#9a743d]">Acceso seguro</p>
              <h2 class="mt-2 text-3xl font-semibold tracking-[-0.04em] text-[#14213d]">Bienvenido de nuevo</h2>
              <p class="mt-2 text-sm leading-6 text-slate-500">Ingresa tus credenciales para continuar al portal escolar.</p>
            </div>

            @if (store.error()) {
              <div role="alert" aria-live="polite" class="mb-5 flex gap-3 rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-800">
                <svg class="mt-0.5 h-5 w-5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M12 9v3m0 4h.01M10.3 3.86L1.82 18.5A2 2 0 003.55 21h16.9a2 2 0 001.73-3L13.7 3.86a2 2 0 00-3.4 0z"/></svg>
                <span><strong class="block text-xs font-bold">No se pudo iniciar sesión</strong><span class="mt-1 block text-xs leading-5">{{ store.error() }}</span></span>
              </div>
            }

            <form (ngSubmit)="submit()" class="space-y-2">
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>Usuario o correo institucional</mat-label>
                <input matInput id="username" type="text" name="username" [(ngModel)]="username" placeholder="usuario@shuji.edu.pe" required autocomplete="username" />
              </mat-form-field>

              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>Contraseña</mat-label>
                <input matInput id="password" [type]="showPassword() ? 'text' : 'password'" name="password" [(ngModel)]="password" required autocomplete="current-password" />
                <button mat-icon-button matSuffix type="button" (click)="togglePasswordVisibility()" [attr.aria-label]="showPassword() ? 'Ocultar contraseña' : 'Mostrar contraseña'">
                  @if (showPassword()) {
                    <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7" d="M3 3l18 18M10.6 10.6a2 2 0 002.8 2.8M9.9 5.2A10.8 10.8 0 0112 5c4.5 0 8.2 2.9 9.5 7a10.8 10.8 0 01-3 4.5M6.2 6.2A10.6 10.6 0 002.5 12c1.3 4.1 5 7 9.5 7 1 0 1.9-.1 2.8-.4"/></svg>
                  } @else {
                    <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7" d="M2.5 12s3.4-7 9.5-7 9.5 7 9.5 7-3.4 7-9.5 7-9.5-7-9.5-7z"/><circle cx="12" cy="12" r="3" stroke-width="1.7"/></svg>
                  }
                </button>
              </mat-form-field>

              <div class="flex flex-col items-start gap-2 pb-3 pt-1 sm:flex-row sm:items-center sm:justify-between sm:gap-3">
                <mat-checkbox [(ngModel)]="rememberMe" name="rememberMe" color="primary" class="text-xs">Recordar este equipo</mat-checkbox>
                <button type="button" (click)="openPasswordHelp(passwordHelp)" class="min-h-11 shrink-0 text-xs font-semibold text-blue-700 transition hover:text-blue-900">¿Olvidaste tu clave?</button>
              </div>

              <button mat-flat-button color="primary" type="submit" [disabled]="store.isLoading() || !username || !password" [attr.aria-busy]="store.isLoading()" class="!h-12 !w-full !rounded-xl !text-sm !font-semibold !shadow-md">
                @if (store.isLoading()) {
                  <mat-spinner diameter="18" class="mr-2" aria-label="Validando acceso"></mat-spinner>
                  <span>Validando acceso…</span>
                } @else {
                  <span>Ingresar al portal</span>
                }
              </button>
            </form>

            <div class="mt-7 flex items-start gap-3 rounded-2xl bg-[#f6f8fc] p-4">
              <span class="grid h-8 w-8 shrink-0 place-items-center rounded-xl bg-blue-50 text-blue-700">
                <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M12 3l8 4v5c0 5-3.4 8-8 9-4.6-1-8-4-8-9V7l8-4z"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M9 12l2 2 4-4"/></svg>
              </span>
              <p class="text-[11px] leading-5 text-slate-600"><strong class="block text-xs font-semibold text-slate-800">Tu información está protegida</strong>El acceso está reservado a usuarios autorizados de la comunidad educativa.</p>
            </div>
          </div>

          <p class="mt-6 text-center text-[10px] tracking-wide text-slate-400">© 2026 I.E.P. Shuji Kitamura · Excelencia, disciplina y saber</p>
        </div>
      </section>

      <ng-template #passwordHelp>
        <h2 mat-dialog-title class="!text-xl !font-semibold !tracking-tight !text-[#14213d]">Recuperación de contraseña</h2>
        <mat-dialog-content class="!text-sm !leading-6 !text-slate-600">
          Por políticas de seguridad institucional, solicita el restablecimiento de tu acceso en Secretaría o mediante la Dirección Académica.
        </mat-dialog-content>
        <mat-dialog-actions align="end" class="!px-6 !pb-5">
          <button mat-flat-button color="primary" mat-dialog-close>Entendido</button>
        </mat-dialog-actions>
      </ng-template>
    </main>
  `
})
export class LoginComponent {
  readonly store = inject(AuthStore);
  private readonly dialog = inject(MatDialog);

  username = '';
  password = '';
  rememberMe = true;
  showPassword = signal(false);

  togglePasswordVisibility(): void {
    this.showPassword.update(value => !value);
  }

  openPasswordHelp(template: TemplateRef<unknown>): void {
    this.dialog.open(template, {
      width: 'min(460px, calc(100vw - 32px))',
      maxWidth: '100vw',
      autoFocus: false
    });
  }

  submit(): void {
    if (!this.username || !this.password) return;

    this.store.login({
      username: this.username.trim(),
      password: this.password
    }).subscribe({
      next: () => {},
      error: () => {}
    });
  }
}
