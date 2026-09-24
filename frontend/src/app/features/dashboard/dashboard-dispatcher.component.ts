import { Component, inject, signal, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { AuthStore } from '../../core/state/auth.store';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

interface ResumenDashboard {
  anio: number;
  totalEstudiantes: number;
  asistenciaHoy: number | null;
  recaudacionMes: number;
  alumnosEnRiesgo: number;
}

@Component({
  selector: 'app-dashboard-dispatcher',
  standalone: true,
  imports: [CommonModule, RouterLink, MatButtonModule, MatCardModule],
  template: `
    <div class="space-y-8 sm:space-y-10">
      <section class="dashboard-welcome relative overflow-hidden rounded-[28px] px-6 py-7 text-white sm:px-9 sm:py-9 xl:px-11">
        <div class="pointer-events-none absolute -right-20 -top-32 h-96 w-96 rounded-full border border-white/[0.08]"></div>
        <div class="pointer-events-none absolute -right-4 -top-16 h-72 w-72 rounded-full border border-white/[0.08]"></div>
        <div class="pointer-events-none absolute -bottom-32 left-1/2 h-64 w-64 rounded-full bg-[#c49a58]/10 blur-3xl"></div>
        <div class="relative z-10 flex flex-col justify-between gap-7 md:flex-row md:items-end">
          <div class="max-w-2xl">
            <p class="mb-4 inline-flex items-center gap-2 rounded-full border border-white/15 bg-white/[0.07] px-3 py-1.5 text-[10px] font-semibold uppercase tracking-[0.15em] text-blue-100">
              <span class="h-1.5 w-1.5 rounded-full bg-[#d9b16f]"></span>
              Año académico {{ anio() ?? '—' }}
            </p>
            <h1 class="text-3xl font-semibold leading-tight tracking-[-0.04em] text-white sm:text-4xl">
              Hola, {{ (store.user()?.username || 'equipo').split(' ')[0] }}
            </h1>
            <p class="mt-3 max-w-xl text-sm leading-6 text-blue-100/75">
              Este es el resumen de actividad de tu comunidad escolar. Los indicadores se actualizan desde el sistema institucional.
            </p>
          </div>
          <div class="grid grid-cols-1 gap-3 sm:flex sm:flex-wrap sm:items-center">
            <span class="inline-flex items-center justify-center gap-2 rounded-xl border border-white/15 bg-white/[0.07] px-3.5 py-2.5 text-xs font-medium text-blue-50 sm:justify-start">
              <span class="h-2 w-2 animate-pulse rounded-full bg-emerald-400"></span> Sistema en línea
            </span>
            @if (store.hasAnyRole(['DIRECCION', 'SECRETARIA'])) {
              <a mat-flat-button routerLink="/admin/matriculas" class="dashboard-primary-action">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M12 5v14m-7-7h14"/></svg>
                <span>Nueva matrícula</span>
              </a>
            }
          </div>
        </div>
      </section>

      <section aria-labelledby="resumen-title" [attr.aria-busy]="loading()">
        <div class="mb-4 flex items-end justify-between gap-4">
          <div>
            <p class="section-eyebrow">Indicadores clave</p>
            <h2 id="resumen-title" class="mt-1 text-lg font-semibold text-slate-800">Resumen institucional</h2>
          </div>
          <span class="hidden text-[11px] text-slate-400 sm:block">Cifras sincronizadas con la plataforma</span>
        </div>

        @if (loading()) {
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4" role="status" aria-label="Cargando indicadores institucionales">
            @for (skeleton of [1, 2, 3, 4]; track skeleton) {
              <div class="metric-skeleton" aria-hidden="true"></div>
            }
          </div>
        } @else {
        <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <mat-card class="metric-card">
            <mat-card-content class="!p-5 sm:!p-6">
              <div class="flex items-start justify-between">
                <div class="metric-icon metric-icon-blue"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7" d="M16 21v-2a4 4 0 00-4-4H6a4 4 0 00-4 4v2m12-13a4 4 0 11-8 0 4 4 0 018 0zm6 13v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75"/></svg></div>
                <span class="metric-kicker">MATRÍCULA</span>
              </div>
              <p class="mt-5 text-3xl font-semibold tracking-tight text-slate-900">{{ totalEstudiantes() !== null ? totalEstudiantes() : '—' }}</p>
              <p class="mt-1 text-xs text-slate-500">Estudiantes registrados</p>
            </mat-card-content>
          </mat-card>

          <mat-card class="metric-card">
            <mat-card-content class="!p-5 sm:!p-6">
              <div class="flex items-start justify-between">
                <div class="metric-icon metric-icon-green"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/></svg></div>
                <span class="metric-kicker">HOY</span>
              </div>
              <p class="mt-5 text-3xl font-semibold tracking-tight text-slate-900">{{ asistenciaHoy() !== null ? (asistenciaHoy() + '%') : '—' }}</p>
              <p class="mt-1 text-xs text-slate-500">Asistencia registrada</p>
            </mat-card-content>
          </mat-card>

          <mat-card class="metric-card">
            <mat-card-content class="!p-5 sm:!p-6">
              <div class="flex items-start justify-between">
                <div class="metric-icon metric-icon-gold"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7" d="M12 8c-1.7 0-3 .9-3 2s1.3 2 3 2 3 .9 3 2-1.3 2-3 2m0-8c1.1 0 2.1.4 2.6 1M12 8V7m0 1v8m0 0v1m0-1c-1.1 0-2.1-.4-2.6-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg></div>
                <span class="metric-kicker">TESORERÍA</span>
              </div>
              <p class="mt-5 text-3xl font-semibold tracking-tight text-slate-900">{{ recaudacionMes() !== null ? ('S/ ' + (recaudacionMes() | number:'1.2-2')) : '—' }}</p>
              <p class="mt-1 text-xs text-slate-500">Recaudación del mes</p>
            </mat-card-content>
          </mat-card>

          <mat-card class="metric-card">
            <mat-card-content class="!p-5 sm:!p-6">
              <div class="flex items-start justify-between">
                <div class="metric-icon metric-icon-violet"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7" d="M13 10V3L4 14h7v7l9-11h-7z"/></svg></div>
                <span class="metric-kicker">ACOMPAÑAMIENTO</span>
              </div>
              <p class="mt-5 text-3xl font-semibold tracking-tight text-slate-900">{{ alumnosEnRiesgo() !== null ? alumnosEnRiesgo() : '—' }}</p>
              <p class="mt-1 text-xs text-slate-500">Estudiantes que requieren refuerzo</p>
            </mat-card-content>
          </mat-card>
        </div>
        }
      </section>

      <section aria-labelledby="modulos-title">
        <div class="mb-4">
          <p class="section-eyebrow">Accesos rápidos</p>
          <h2 id="modulos-title" class="mt-1 text-lg font-semibold text-slate-800">Continúa con tu trabajo</h2>
        </div>
        <div class="grid grid-cols-1 gap-4 md:grid-cols-3">
          <a routerLink="/admin/matriculas" class="module-card group">
            <span class="module-icon metric-icon-blue"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7" d="M8 5H6a2 2 0 00-2 2v13h14v-2M8 5a2 2 0 012-2h4a2 2 0 012 2v2H8V5zm8 7h6m-3-3v6m-9-2h3m-3 4h6"/></svg></span>
            <span class="mt-5 block text-sm font-semibold text-slate-800">Matrículas y estudiantes</span>
            <span class="mt-2 block text-xs leading-5 text-slate-500">Expedientes, solicitudes y fichas únicas.</span>
            <span class="module-link">Abrir módulo <span aria-hidden="true">→</span></span>
          </a>
          <a routerLink="/admin/calificaciones" class="module-card group">
            <span class="module-icon metric-icon-green"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7" d="M8 4h8a2 2 0 012 2v15H6V6a2 2 0 012-2zm1 5h6m-6 4h6m-6 4h3"/></svg></span>
            <span class="mt-5 block text-sm font-semibold text-slate-800">Calificaciones CNEB</span>
            <span class="mt-2 block text-xs leading-5 text-slate-500">Registro y consulta del progreso académico.</span>
            <span class="module-link">Abrir módulo <span aria-hidden="true">→</span></span>
          </a>
          <a routerLink="/admin/asistencia" class="module-card group">
            <span class="module-icon metric-icon-gold"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" aria-hidden="true"><circle cx="12" cy="12" r="9" stroke-width="1.7"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7" d="M12 7v5l3 2"/></svg></span>
            <span class="mt-5 block text-sm font-semibold text-slate-800">Asistencia y biometría</span>
            <span class="mt-2 block text-xs leading-5 text-slate-500">Control diario e importación de marcas.</span>
            <span class="module-link">Abrir módulo <span aria-hidden="true">→</span></span>
          </a>
        </div>
      </section>
    </div>
  `
})
export class DashboardDispatcherComponent implements OnInit {
  readonly store = inject(AuthStore);
  private readonly http = inject(HttpClient);
  readonly loading = signal(true);
  readonly anio = signal<number | null>(null);
  readonly totalEstudiantes = signal<number | null>(null);
  readonly asistenciaHoy = signal<number | null>(null);
  readonly recaudacionMes = signal<number | null>(null);
  readonly alumnosEnRiesgo = signal<number | null>(null);

  ngOnInit(): void {
    this.cargarDatosReales();
  }

  cargarDatosReales(): void {
    this.http.get<ResumenDashboard>(`${environment.apiUrl}/dashboard/resumen`).subscribe({
      next: res => {
        this.anio.set(res?.anio ?? new Date().getFullYear());
        this.totalEstudiantes.set(res?.totalEstudiantes ?? 0);
        this.asistenciaHoy.set(res?.asistenciaHoy ?? 0);
        this.recaudacionMes.set(res?.recaudacionMes ?? 0);
        this.alumnosEnRiesgo.set(res?.alumnosEnRiesgo ?? 0);
        this.loading.set(false);
      },
      error: () => {
        this.anio.set(new Date().getFullYear());
        this.totalEstudiantes.set(0);
        this.asistenciaHoy.set(0);
        this.recaudacionMes.set(0);
        this.alumnosEnRiesgo.set(0);
        this.loading.set(false);
      }
    });
  }
}
