import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { AuthStore } from '../../core/state/auth.store';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatButtonModule, MatMenuModule],
  template: `
    <div class="min-h-screen bg-[#f4f6fb] text-slate-900 lg:flex">
      <aside class="sidebar-shell hidden w-[268px] shrink-0 flex-col lg:sticky lg:top-0 lg:flex lg:h-screen">
        <a routerLink="/admin/dashboard" class="flex items-center gap-3 px-6 py-7" aria-label="Inicio institucional">
          <span class="brand-crest">SK</span>
          <span class="min-w-0">
            <span class="block truncate text-sm font-bold tracking-tight text-white">Shuji Kitamura</span>
            <span class="mt-0.5 block text-[10px] font-medium tracking-[0.12em] text-slate-400">GESTIÓN ESCOLAR</span>
          </span>
        </a>

        <nav class="flex-1 space-y-6 overflow-y-auto px-4 pb-5" aria-label="Navegación principal">
          @if (store.hasAnyRole(['DIRECCION', 'SECRETARIA'])) {
            <section>
              <p class="nav-section-label">Principal</p>
              <a routerLink="/admin/dashboard" routerLinkActive="nav-link-active" class="nav-link">
                <span class="nav-link-icon">01</span><span>Panel general</span>
              </a>
            </section>
          }

          <section>
            <p class="nav-section-label">Vida académica</p>
            @if (store.hasAnyRole(['DIRECCION', 'SECRETARIA'])) {
              <a routerLink="/admin/matriculas" routerLinkActive="nav-link-active" class="nav-link"><span class="nav-link-icon">02</span><span>Matrículas</span></a>
              <a routerLink="/admin/academico" routerLinkActive="nav-link-active" class="nav-link"><span class="nav-link-icon">03</span><span>Aulas y horarios</span></a>
            }
            @if (store.hasAnyRole(['DIRECCION', 'SECRETARIA', 'DOCENTE'])) {
              <a routerLink="/admin/calificaciones" routerLinkActive="nav-link-active" class="nav-link"><span class="nav-link-icon">04</span><span>Calificaciones</span></a>
            }
            @if (store.hasAnyRole(['DIRECCION', 'SECRETARIA', 'AUXILIAR', 'TUTOR'])) {
              <a routerLink="/admin/asistencia" routerLinkActive="nav-link-active" class="nav-link"><span class="nav-link-icon">05</span><span>Asistencia</span></a>
              <a routerLink="/admin/convivencia" routerLinkActive="nav-link-active" class="nav-link"><span class="nav-link-icon">06</span><span>Convivencia</span></a>
            }
          </section>

          <section>
            <p class="nav-section-label">Comunidad y gestión</p>
            @if (store.hasAnyRole(['DIRECCION', 'SECRETARIA', 'APODERADO'])) {
              <a routerLink="/admin/tesoreria" routerLinkActive="nav-link-active" class="nav-link"><span class="nav-link-icon">07</span><span>Tesorería y pagos</span></a>
              <a routerLink="/admin/comunicados" routerLinkActive="nav-link-active" class="nav-link"><span class="nav-link-icon">08</span><span>Comunicados</span></a>
            }
            @if (store.hasAnyRole(['DIRECCION', 'SECRETARIA'])) {
              <a routerLink="/admin/usuarios" routerLinkActive="nav-link-active" class="nav-link"><span class="nav-link-icon">09</span><span>Usuarios y permisos</span></a>
            }
            <a routerLink="/admin/perfil" routerLinkActive="nav-link-active" class="nav-link"><span class="nav-link-icon">10</span><span>Mi perfil</span></a>
          </section>
        </nav>

        <div class="mx-4 mb-4 rounded-2xl border border-white/10 bg-white/[0.04] p-3.5">
          <div class="flex items-center gap-3">
            <span class="user-avatar">{{ (store.user()?.username || 'U').substring(0, 1).toUpperCase() }}</span>
            <span class="min-w-0 flex-1">
              <span class="block truncate text-xs font-semibold text-white">{{ store.user()?.username }}</span>
              <span class="mt-0.5 block truncate text-[10px] text-slate-400">{{ store.roles().join(' · ') }}</span>
            </span>
          </div>
          <button mat-button type="button" (click)="store.logout()" class="logout-button mt-3 w-full !justify-start !rounded-xl !text-slate-300">
            <svg class="mr-2 h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M10 17l5-5-5-5m5 5H3m9-9h6a2 2 0 012 2v14a2 2 0 01-2 2h-6"/></svg>
            Cerrar sesión
          </button>
        </div>
      </aside>

      <main class="min-w-0 flex-1">
        <header class="app-topbar sticky top-0 z-40 flex h-[72px] items-center justify-between px-4 sm:px-7 xl:px-10">
          <div class="flex items-center gap-3">
            <button mat-icon-button type="button" [matMenuTriggerFor]="mobileNav" class="!flex lg:!hidden" aria-label="Abrir navegación">
              <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M4 6h16M4 12h16M4 18h16"/></svg>
            </button>
            <mat-menu #mobileNav="matMenu">
              @if (store.hasAnyRole(['DIRECCION', 'SECRETARIA'])) { <a mat-menu-item routerLink="/admin/dashboard">Panel general</a> }
              @if (store.hasAnyRole(['DIRECCION', 'SECRETARIA'])) { <a mat-menu-item routerLink="/admin/matriculas">Matrículas</a> }
              @if (store.hasAnyRole(['DIRECCION', 'SECRETARIA', 'DOCENTE'])) { <a mat-menu-item routerLink="/admin/calificaciones">Calificaciones</a> }
              @if (store.hasAnyRole(['DIRECCION', 'SECRETARIA', 'AUXILIAR', 'TUTOR'])) { <a mat-menu-item routerLink="/admin/asistencia">Asistencia</a> }
              @if (store.hasAnyRole(['DIRECCION', 'SECRETARIA'])) { <a mat-menu-item routerLink="/admin/academico">Aulas y horarios</a> }
              @if (store.hasAnyRole(['DIRECCION', 'SECRETARIA', 'APODERADO'])) { <a mat-menu-item routerLink="/admin/tesoreria">Tesorería y pagos</a> }
              @if (store.hasAnyRole(['DIRECCION', 'SECRETARIA', 'AUXILIAR', 'TUTOR'])) { <a mat-menu-item routerLink="/admin/convivencia">Convivencia</a> }
              @if (store.hasAnyRole(['DIRECCION', 'SECRETARIA', 'APODERADO'])) { <a mat-menu-item routerLink="/admin/comunicados">Comunicados</a> }
              @if (store.hasAnyRole(['DIRECCION', 'SECRETARIA'])) { <a mat-menu-item routerLink="/admin/usuarios">Usuarios y permisos</a> }
              <a mat-menu-item routerLink="/admin/perfil">Mi perfil</a>
              <button mat-menu-item type="button" (click)="store.logout()">Cerrar sesión</button>
            </mat-menu>
            <div>
              <p class="text-[10px] font-bold uppercase tracking-[0.16em] text-slate-500">Portal institucional</p>
              <p class="text-sm font-semibold text-slate-800">Shuji Kitamura <span class="font-normal text-slate-400">/ Gestión escolar</span></p>
            </div>
          </div>

          <a routerLink="/admin/perfil" class="hidden items-center gap-2.5 rounded-full border border-slate-200 bg-white px-3 py-2 text-xs font-medium text-slate-600 shadow-sm transition hover:border-blue-200 hover:text-blue-700 sm:flex">
            <span class="h-2 w-2 rounded-full bg-emerald-500"></span>
            <span class="max-w-[220px] truncate">{{ store.user()?.email }}</span>
            <span class="user-avatar user-avatar-small">{{ (store.user()?.username || 'U').substring(0, 1).toUpperCase() }}</span>
          </a>
        </header>

        <div class="mx-auto w-full max-w-[1560px] p-4 sm:p-6 xl:p-9">
          <div class="app-content"><router-outlet /></div>
        </div>
        <footer class="px-5 pb-5 text-center text-[10px] tracking-wide text-slate-400">© 2026 I.E.P. Shuji Kitamura · Excelencia, disciplina y saber</footer>
      </main>
    </div>
  `
})
export class AdminLayoutComponent {
  readonly store = inject(AuthStore);
}
