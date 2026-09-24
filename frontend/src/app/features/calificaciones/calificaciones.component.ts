import { Component, ElementRef, inject, signal, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EvaluacionService } from '../../core/services/evaluacion.service';
import { AcademicoService } from '../../core/services/academico.service';
import { AsignacionDocenteResponseDto, CompetenciaResponseDto, PeriodoResponseDto } from '../../core/models/academico.model';

type NotaCNEB = 'AD' | 'A' | 'B' | 'C' | '-';

export interface EvaluacionAlumno {
  matriculaId: number;
  numeroOrden: number;
  estudiante: string;
  c1: NotaCNEB;
  c2: NotaCNEB;
  c3: NotaCNEB;
  conclusionDescriptiva: string;
  requiereRefuerzo: boolean;
  competenciaId: number;
}

@Component({
  selector: 'app-calificaciones',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="space-y-6">
      <!-- Header -->
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 class="text-2xl sm:text-3xl font-extrabold tracking-tight text-white">Matriz de Calificaciones CNEB</h2>
          <p class="text-sm text-slate-400 mt-1">
            Registro y evaluación por competencias conectada con la base de datos oficial.
          </p>
        </div>
        <div class="flex flex-wrap items-center justify-end gap-3">
          @if (guardadoExitoso()) {
            <span class="text-xs font-semibold text-emerald-400 flex items-center gap-1.5 animate-fade-in">
              <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd" />
              </svg>
              Calificaciones guardadas en BD
            </span>
          }
          @if (error()) { <span class="text-xs text-red-400">{{ error() }}</span> }
          <button
            (click)="guardarCalificaciones()"
            [disabled]="alumnos().length === 0 || !asignacionId || !competenciaId"
            class="px-4 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-semibold text-xs sm:text-sm shadow-lg shadow-emerald-600/30 transition flex items-center gap-2 cursor-pointer disabled:opacity-50"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-3m-1 4l-3 3m0 0l-3-3m3 3V4" />
            </svg>
            <span>Guardar en BD</span>
          </button>
        </div>
      </div>

      <!-- Selectors Panel -->
      <div class="p-4 rounded-2xl bg-slate-900 border border-slate-800 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div>
          <label class="block text-xs font-semibold text-slate-400 mb-1">Periodo Académico</label>
          <select
            [(ngModel)]="periodoId"
            (change)="cargarCalificaciones()"
            [disabled]="!periodos().length"
            class="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs focus:outline-none focus:border-blue-500"
          >
            @if (!periodos().length) { <option [ngValue]="0">Sin periodos disponibles</option> } @for (periodo of periodos(); track periodo.id) { <option [ngValue]="periodo.id">{{ periodo.nombre }}</option> }
          </select>
        </div>

        <div>
          <label class="block text-xs font-semibold text-slate-400 mb-1">Asignación docente y sección</label>
          <select
            [(ngModel)]="asignacionId"
            (change)="cambiarAsignacion()"
            [disabled]="!asignaciones().length"
            class="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs focus:outline-none focus:border-blue-500"
          >
            @if (!asignaciones().length) { <option [ngValue]="0">Sin asignaciones disponibles</option> } @for (a of asignaciones(); track a.id) { <option [ngValue]="a.id">Sección {{ a.seccionId }} · Área {{ a.areaCurricularId }} · Asignación {{ a.id }}</option> }
          </select>
        </div>

        <div>
          <label class="block text-xs font-semibold text-slate-400 mb-1">Competencia</label>
          <select
            [(ngModel)]="competenciaId"
            (change)="cargarCalificaciones()"
            [disabled]="!competencias().length"
            class="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs focus:outline-none focus:border-blue-500"
          >
            @if (!competencias().length) { <option [ngValue]="0">Sin competencias disponibles</option> } @for (c of competencias(); track c.id) { <option [ngValue]="c.id">{{ c.nombre }}</option> }
          </select>
        </div>

        <div class="flex items-end">
          <button
            (click)="cargarCalificaciones()"
            [disabled]="!asignacionId || !periodoId || !competenciaId"
            class="w-full py-2 px-3 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold transition cursor-pointer flex items-center justify-center gap-1.5"
          >
            <span>Consultar Registros</span>
          </button>
        </div>
      </div>

      <!-- CNEB Evaluation Matrix Table -->
      <div class="rounded-2xl bg-slate-900 border border-slate-800 overflow-hidden shadow-sm">
        <div class="px-4 py-4 sm:px-6 bg-slate-950/50 border-b border-slate-800 flex flex-wrap items-center justify-between gap-3 text-xs">
          <div class="flex flex-wrap items-center gap-3">
            <span class="font-bold text-white uppercase tracking-wider">Escala CNEB:</span>
            <span class="px-2 py-0.5 rounded bg-blue-950 text-blue-300 border border-blue-800 font-bold">AD</span>
            <span class="px-2 py-0.5 rounded bg-emerald-950 text-emerald-300 border border-emerald-800 font-bold">A</span>
            <span class="px-2 py-0.5 rounded bg-amber-950 text-amber-300 border border-amber-800 font-bold">B</span>
            <span class="px-2 py-0.5 rounded bg-red-950 text-red-400 border border-red-800 font-bold">C</span>
          </div>
          <div class="text-slate-400">
            Total en matriz: <span class="text-white font-bold">{{ alumnos().length }}</span>
          </div>
        </div>

        <div class="overflow-x-auto">
          <table class="w-full text-left text-xs">
            <thead class="bg-slate-950/70 border-b border-slate-800 text-slate-400 uppercase tracking-wider font-semibold">
              <tr>
                <th class="px-4 py-3.5 w-12 text-center">N°</th>
                <th class="px-5 py-3.5">Estudiante / Matrícula</th>
                <th class="px-4 py-3.5 text-center">Calificación</th>
                <th class="px-4 py-3.5 text-center">Conclusión</th>
                <th class="px-4 py-3.5 text-center">Refuerzo</th>
                <th class="px-4 py-3.5 text-right">Acciones</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-800/60 text-slate-300">
              @if (cargando()) {
                <tr>
                  <td colspan="6" class="text-center py-10 text-slate-400">
                    <span class="inline-block w-5 h-5 border-2 border-emerald-500/30 border-t-emerald-500 rounded-full animate-spin mb-2"></span>
                    <p>Consultando calificaciones en la base de datos...</p>
                  </td>
                </tr>
              } @else {
                @for (alumno of alumnos(); track alumno.matriculaId) {
                  <tr class="hover:bg-slate-800/40 transition">
                    <td class="px-4 py-3 text-center font-mono text-slate-400">{{ alumno.numeroOrden }}</td>
                    <td class="px-5 py-3 font-bold text-white">{{ alumno.estudiante }}</td>
                    <td class="px-4 py-3 text-center font-bold">{{ alumno.c1 === '-' ? 'Sin calificar' : alumno.c1 }}</td>
                    <td class="px-4 py-3 max-w-xs truncate" [title]="alumno.conclusionDescriptiva || 'Sin conclusión'">{{ alumno.conclusionDescriptiva || 'Sin conclusión' }}</td>
                    <td class="px-4 py-3 text-center">
                      @if (alumno.requiereRefuerzo) {
                        <span class="px-2 py-0.5 rounded text-[10px] font-bold bg-red-950 text-red-400 border border-red-800">CNEB C</span>
                      } @else {
                        <span class="text-slate-500 text-[10px]">Normal</span>
                      }
                    </td>
                    <td class="px-4 py-3 text-right"><button type="button" (click)="abrirEdicion(alumno)" class="rounded-lg border border-blue-200 bg-blue-50 px-3 py-2 font-semibold text-blue-700 hover:bg-blue-100">Editar</button></td>
                  </tr>
                } @empty {
                  <tr>
                    <td colspan="6" class="text-center py-12 text-slate-500 space-y-2">
                      <div class="text-2xl">📝</div>
                      <p class="font-medium text-slate-400">No se encontraron calificaciones registradas en la base de datos.</p>
                      <p class="text-[11px] text-slate-500">Seleccione un periodo y sección con matrículas asignadas para calificar.</p>
                    </td>
                  </tr>
                }
              }
            </tbody>
          </table>
        </div>
      </div>

      <dialog #edicionDialog class="school-native-dialog" aria-labelledby="calificacion-dialog-title">
        <div class="school-dialog-shell"><header class="school-dialog-header"><div><h3 id="calificacion-dialog-title">Editar evaluación</h3><p>{{ alumnoEditando()?.estudiante }}</p></div><button type="button" class="school-dialog-cancel !min-h-9 !px-3" (click)="cerrarEdicion()" aria-label="Cerrar">✕</button></header>
          <form (ngSubmit)="aplicarEdicion()" class="school-dialog-form"><div class="school-dialog-body space-y-4">
            <label class="school-dialog-field">Calificación CNEB *<select [(ngModel)]="notaEditada" name="nota" required><option value="-" disabled>Seleccione</option><option value="AD">AD · Logro destacado</option><option value="A">A · Logro esperado</option><option value="B">B · En proceso</option><option value="C">C · En inicio</option></select></label>
            <label class="school-dialog-field">Conclusión descriptiva<textarea [(ngModel)]="conclusionEditada" name="conclusion" maxlength="4000" rows="5" placeholder="Describe los avances y aspectos por reforzar"></textarea></label>
            @if (notaEditada === 'C') { <p class="text-xs font-medium text-amber-700">Esta calificación marcará al estudiante para refuerzo.</p> }
          </div><footer class="school-dialog-footer"><button type="button" class="school-dialog-cancel" (click)="cerrarEdicion()">Cancelar</button><button type="submit" [disabled]="notaEditada === '-'" class="school-dialog-submit">Aplicar cambios</button></footer></form>
        </div>
      </dialog>
    </div>
  `
})
export class CalificacionesComponent implements OnInit {
  private readonly evaluacionService = inject(EvaluacionService);
  private readonly academicoService = inject(AcademicoService);
  @ViewChild('edicionDialog') private edicionDialog!: ElementRef<HTMLDialogElement>;
  readonly alumnoEditando = signal<EvaluacionAlumno | null>(null);
  notaEditada: NotaCNEB = '-';
  conclusionEditada = '';

  periodoId = 0;
  asignacionId = 0;
  competenciaId = 0;
  readonly asignaciones = signal<AsignacionDocenteResponseDto[]>([]);
  readonly competencias = signal<CompetenciaResponseDto[]>([]);
  readonly periodos = signal<PeriodoResponseDto[]>([]);
  readonly error = signal<string | null>(null);
  guardadoExitoso = signal(false);
  cargando = signal(false);

  readonly alumnos = signal<EvaluacionAlumno[]>([]);

  abrirEdicion(alumno: EvaluacionAlumno): void {
    this.alumnoEditando.set(alumno);
    this.notaEditada = alumno.c1;
    this.conclusionEditada = alumno.conclusionDescriptiva;
    this.edicionDialog.nativeElement.showModal();
  }

  cerrarEdicion(): void { this.edicionDialog.nativeElement.close(); this.alumnoEditando.set(null); }

  aplicarEdicion(): void {
    const seleccionado = this.alumnoEditando();
    if (!seleccionado || this.notaEditada === '-') return;
    this.alumnos.update(lista => lista.map(alumno => alumno.matriculaId === seleccionado.matriculaId
      ? { ...alumno, c1: this.notaEditada, conclusionDescriptiva: this.conclusionEditada.trim(), requiereRefuerzo: this.notaEditada === 'C' }
      : alumno));
    this.cerrarEdicion();
    this.guardadoExitoso.set(false);
  }

  ngOnInit(): void {
    this.academicoService.listarAsignaciones().subscribe({
      next: asignaciones => {
        this.asignaciones.set(asignaciones);
        this.asignacionId = asignaciones[0]?.id ?? 0;
        this.cambiarAsignacion();
      },
      error: () => this.error.set('No se pudieron consultar las asignaciones docentes.')
    });
  }

  cambiarAsignacion(): void {
    const asignacion = this.asignaciones().find(a => a.id === Number(this.asignacionId));
    if (!asignacion) return;
    this.periodoId = 0;
    this.competenciaId = 0;
    this.alumnos.set([]);
    this.error.set(null);
    this.academicoService.listarCompetencias(asignacion.areaCurricularId).subscribe({
      next: competencias => { this.competencias.set(competencias); this.competenciaId = competencias[0]?.id ?? 0; this.cargarCalificaciones(); },
      error: () => this.error.set('No se pudieron consultar las competencias.')
    });
    this.academicoService.listarPeriodos(asignacion.anioLectivoId).subscribe({
      next: periodos => { this.periodos.set(periodos); this.periodoId = periodos[0]?.id ?? 0; this.cargarCalificaciones(); },
      error: () => this.error.set('No se pudieron consultar los periodos.')
    });
  }

  cargarCalificaciones(): void {
    this.cargando.set(true);
    if (!this.asignacionId || !this.periodoId || !this.competenciaId) { this.cargando.set(false); return; }
    this.evaluacionService.listarMatriz(Number(this.asignacionId), Number(this.periodoId), Number(this.competenciaId)).subscribe({
      next: (res) => {
        this.cargando.set(false);
        if (Array.isArray(res)) {
          this.alumnos.set(
            res.map((c, idx) => ({
              matriculaId: c.matriculaId,
              numeroOrden: idx + 1,
              estudiante: `Matrícula #${c.matriculaId}`,
              c1: c.calificacionCualitativa ?? '-',
              c2: '-',
              c3: '-',
              conclusionDescriptiva: c.conclusionDescriptiva || '',
              requiereRefuerzo: c.requiereRefuerzo ?? false,
              competenciaId: c.competenciaId
            }))
          );
        } else {
          this.alumnos.set([]);
        }
      },
      error: (err) => {
        this.cargando.set(false);
        this.alumnos.set([]);
        this.error.set(err?.error?.message ?? 'No se pudo consultar la matriz de calificaciones.');
      }
    });
  }

  guardarCalificaciones(): void {
    if (this.alumnos().length === 0 || !this.asignacionId || !this.competenciaId || !this.periodoId) return;
    if (this.alumnos().some(a => a.c1 === '-')) { this.error.set('Seleccione una calificación para cada estudiante.'); return; }
    this.error.set(null);
    const payload = {
      asignacionDocenteId: Number(this.asignacionId),
      periodoAcademicoId: Number(this.periodoId),
      calificaciones: this.alumnos().map(a => ({
        matriculaId: a.matriculaId,
        competenciaId: a.competenciaId,
        calificacionCualitativa: a.c1 as 'AD' | 'A' | 'B' | 'C',
        conclusionDescriptiva: a.conclusionDescriptiva,
        sugerenciaIaUtilizada: false
      }))
    };

    this.evaluacionService.registrarCalificaciones(payload).subscribe({
      next: () => {
        this.guardadoExitoso.set(true);
        this.cargarCalificaciones();
        setTimeout(() => this.guardadoExitoso.set(false), 3000);
      },
      error: (err) => {
        this.guardadoExitoso.set(false);
        this.error.set(err?.error?.message ?? 'No se pudieron guardar las calificaciones.');
      }
    });
  }
}
