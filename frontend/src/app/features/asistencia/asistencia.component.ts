import { Component, ElementRef, inject, signal, computed, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AsistenciaService } from '../../core/services/asistencia.service';
import { AcademicoService } from '../../core/services/academico.service';
import { AnioLectivoResponseDto } from '../../core/models/academico.model';

export type EstadoAsistencia = 'PRESENTE' | 'TARDANZA' | 'FALTA_JUSTIFICADA' | 'FALTA_INJUSTIFICADA';

export interface AlumnoAsistenciaItem {
  id: number;
  dni: string;
  estudiante: string;
  estado: EstadoAsistencia;
  horaMarcacionPuerta?: string;
}

export interface DiscrepanciaBiometricaItem {
  id: number;
  estudiante: string;
  dni: string;
  horaPuerta: string;
  estadoAula: string;
  tipoDiscrepancia: string;
}

@Component({
  selector: 'app-asistencia',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="space-y-6">
      <!-- Header -->
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 class="text-2xl sm:text-3xl font-extrabold tracking-tight text-white">Control de Asistencia & Biometría</h2>
          <p class="text-sm text-slate-400 mt-1">
            Consulta de marcas diarias y conciliación con molinetes biométricos de la base de datos.
          </p>
        </div>
        <div class="flex flex-wrap items-center gap-2">
          <button (click)="abrirDialogoRegistro()" class="px-4 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-semibold text-xs sm:text-sm cursor-pointer">Registrar asistencia</button>
          <button (click)="abrirDialogoBiometrico()" class="px-4 py-2.5 rounded-xl bg-slate-700 hover:bg-slate-600 text-white font-semibold text-xs sm:text-sm cursor-pointer">Importar marca</button>
          <button
            (click)="ejecutarConciliacion()"
            class="px-4 py-2.5 rounded-xl bg-amber-600 hover:bg-amber-500 text-white font-semibold text-xs sm:text-sm shadow-lg shadow-amber-600/30 transition flex items-center gap-2 cursor-pointer"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            <span>Ejecutar Conciliación en BD</span>
          </button>
        </div>
      </div>
      @if (error()) { <p class="text-red-400 text-sm" role="alert">{{ error() }}</p> }
      @if (mensaje()) { <p class="text-emerald-400 text-sm" role="status">{{ mensaje() }}</p> }

      <dialog #registroDialog class="school-native-dialog" aria-labelledby="registro-dialog-title">
        <div class="school-dialog-shell"><header class="school-dialog-header"><div><h3 id="registro-dialog-title">Registrar asistencia</h3><p>Registra una marca manual para la matrícula seleccionada.</p></div><button type="button" class="school-dialog-cancel !min-h-9 !px-3" (click)="cerrarDialogos()" aria-label="Cerrar">✕</button></header>
          <form (ngSubmit)="registrarAsistencia()" class="school-dialog-form"><div class="school-dialog-body space-y-4">
            @if (error()) { <p class="school-dialog-error" role="alert">{{ error() }}</p> }
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <label class="school-dialog-field">Matrícula ID *<input type="number" [(ngModel)]="matriculaId" name="matriculaId" min="1" required></label>
              <label class="school-dialog-field">Fecha *<input type="date" [(ngModel)]="fechaSesionNueva" name="fechaSesion" required></label>
              <label class="school-dialog-field">Hora *<input type="time" [(ngModel)]="horaRegistro" name="horaRegistro" required></label>
              <label class="school-dialog-field">Estado<select [(ngModel)]="estadoRegistro" name="estado"><option value="PRESENTE">Presente</option><option value="TARDANZA">Tardanza</option><option value="FALTA_INJUSTIFICADA">Falta injustificada</option></select></label>
            </div>
          </div><footer class="school-dialog-footer"><button type="button" class="school-dialog-cancel" (click)="cerrarDialogos()">Cancelar</button><button type="submit" [disabled]="operando()" class="school-dialog-submit">{{ operando() ? 'Guardando…' : 'Guardar asistencia' }}</button></footer></form>
        </div>
      </dialog>

      <dialog #biometricoDialog class="school-native-dialog" aria-labelledby="biometrico-dialog-title">
        <div class="school-dialog-shell"><header class="school-dialog-header"><div><h3 id="biometrico-dialog-title">Importar marca biométrica</h3><p>Registra una lectura de molinete asociada a un DNI.</p></div><button type="button" class="school-dialog-cancel !min-h-9 !px-3" (click)="cerrarDialogos()" aria-label="Cerrar">✕</button></header>
          <form (ngSubmit)="importarMarca()" class="school-dialog-form"><div class="school-dialog-body space-y-4">
            @if (error()) { <p class="school-dialog-error" role="alert">{{ error() }}</p> }
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <label class="school-dialog-field">DNI *<input [(ngModel)]="dniBiometrico" name="dniBiometrico" inputmode="numeric" pattern="[0-9]{8}" maxlength="8" required placeholder="8 dígitos"></label>
              <label class="school-dialog-field">Fecha y hora *<input type="datetime-local" [(ngModel)]="fechaHoraBiometrico" name="fechaHoraBiometrico" required></label>
              <label class="school-dialog-field sm:col-span-2">Código del dispositivo *<input [(ngModel)]="dispositivoCodigo" name="dispositivoCodigo" maxlength="30" required></label>
            </div>
          </div><footer class="school-dialog-footer"><button type="button" class="school-dialog-cancel" (click)="cerrarDialogos()">Cancelar</button><button type="submit" [disabled]="operando()" class="school-dialog-submit">{{ operando() ? 'Importando…' : 'Registrar marca' }}</button></footer></form>
        </div>
      </dialog>

      <dialog #justificacionDialog class="school-native-dialog" aria-labelledby="justificacion-dialog-title">
        <div class="school-dialog-shell"><header class="school-dialog-header"><div><h3 id="justificacion-dialog-title">Justificar inasistencia</h3><p>La justificación quedará asociada a la asistencia seleccionada.</p></div><button type="button" class="school-dialog-cancel !min-h-9 !px-3" (click)="cerrarDialogos()" aria-label="Cerrar">✕</button></header>
          <form (ngSubmit)="justificarSeleccionada()" class="school-dialog-form"><div class="school-dialog-body space-y-4">
            @if (error()) { <p class="school-dialog-error" role="alert">{{ error() }}</p> }
            <label class="school-dialog-field">Motivo de justificación *<textarea [(ngModel)]="motivoJustificacion" name="motivoJustificacion" required maxlength="4000" rows="4" placeholder="Describe brevemente el motivo"></textarea></label>
          </div><footer class="school-dialog-footer"><button type="button" class="school-dialog-cancel" (click)="cerrarDialogos()">Cancelar</button><button type="submit" [disabled]="operando()" class="school-dialog-submit">{{ operando() ? 'Guardando…' : 'Guardar justificación' }}</button></footer></form>
        </div>
      </dialog>

      <!-- Filters & Date Selection -->
      <div class="p-4 rounded-2xl bg-slate-900 border border-slate-800 grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div>
          <label class="block text-xs font-semibold text-slate-400 mb-1">Fecha de Registro</label>
          <input
            type="date"
            [(ngModel)]="fechaSeleccionada"
            (change)="cargarAsistencia()"
            class="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs focus:outline-none focus:border-blue-500"
          />
        </div>

        <div><label class="block text-xs font-semibold text-slate-400 mb-1">Año lectivo para conciliación</label><select [(ngModel)]="anioId" [disabled]="!anios().length" class="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs">@if (!anios().length) { <option [ngValue]="0">Sin años lectivos disponibles</option> } @for (anio of anios(); track anio.id) { <option [ngValue]="anio.id">{{ anio.anio }}</option> }</select></div>

        <div class="flex items-end">
          <button
            (click)="cargarAsistencia()"
            class="w-full py-2 px-3 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold transition cursor-pointer flex items-center justify-center gap-1.5"
          >
            <span>Actualizar de la BD</span>
          </button>
        </div>
      </div>

      <!-- Summary Quick Stats -->
      <div class="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <div class="p-4 rounded-xl bg-emerald-950/30 border border-emerald-800/40">
          <div class="text-[10px] font-bold uppercase tracking-wider text-emerald-400">Presentes</div>
          <div class="text-2xl font-black text-white mt-1">{{ totalPresentes() }}</div>
        </div>
        <div class="p-4 rounded-xl bg-amber-950/30 border border-amber-800/40">
          <div class="text-[10px] font-bold uppercase tracking-wider text-amber-400">Tardanzas</div>
          <div class="text-2xl font-black text-white mt-1">{{ totalTardanzas() }}</div>
        </div>
        <div class="p-4 rounded-xl bg-blue-950/30 border border-blue-800/40">
          <div class="text-[10px] font-bold uppercase tracking-wider text-blue-400">Faltas Justificadas</div>
          <div class="text-2xl font-black text-white mt-1">{{ totalJustificadas() }}</div>
        </div>
        <div class="p-4 rounded-xl bg-red-950/30 border border-red-800/40">
          <div class="text-[10px] font-bold uppercase tracking-wider text-red-400">Faltas Injustificadas</div>
          <div class="text-2xl font-black text-white mt-1">{{ totalInjustificadas() }}</div>
        </div>
      </div>

      <!-- Tabs Navigation -->
      <div class="flex items-center gap-2 border-b border-slate-800">
        <button
          (click)="tabActiva.set('AULA')"
          [class]="tabActiva() === 'AULA' ? 'border-blue-500 text-blue-400 font-bold' : 'border-transparent text-slate-400 hover:text-white'"
          class="px-4 py-2.5 border-b-2 text-xs transition cursor-pointer"
        >
          Pase de Lista de Aula
        </button>
        <button
          (click)="tabActiva.set('CONCILIACION')"
          [class]="tabActiva() === 'CONCILIACION' ? 'border-amber-500 text-amber-400 font-bold' : 'border-transparent text-slate-400 hover:text-white'"
          class="px-4 py-2.5 border-b-2 text-xs transition cursor-pointer flex items-center gap-2"
        >
          <span>Alertas de Conciliación Biométrica</span>
          <span class="px-1.5 py-0.2 rounded-full text-[10px] bg-amber-950 text-amber-400 border border-amber-800">
            {{ discrepancias().length }}
          </span>
        </button>
      </div>

      <!-- Tab Content: Pase de Lista -->
      @if (tabActiva() === 'AULA') {
        <div class="rounded-2xl bg-slate-900 border border-slate-800 overflow-hidden shadow-sm">
          <div class="overflow-x-auto">
            <table class="w-full text-left text-xs">
              <thead class="bg-slate-950/70 border-b border-slate-800 text-slate-400 uppercase tracking-wider font-semibold">
                <tr>
                  <th class="px-5 py-3.5">Estudiante</th>
                  <th class="px-5 py-3.5">DNI</th>
                  <th class="px-5 py-3.5">Molinete Puerta</th>
                  <th class="px-5 py-3.5 text-center">Pase de Lista en Aula</th>
                  <th class="px-5 py-3.5 text-center">Acciones</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-800/60 text-slate-300">
                @if (cargando()) {
                  <tr>
                    <td colspan="5" class="text-center py-10 text-slate-400">
                      <span class="inline-block w-5 h-5 border-2 border-blue-500/30 border-t-blue-500 rounded-full animate-spin mb-2"></span>
                      <p>Consultando marcas de asistencia en la base de datos...</p>
                    </td>
                  </tr>
                } @else {
                  @for (item of alumnos(); track item.id) {
                    <tr class="hover:bg-slate-800/40 transition">
                      <td class="px-5 py-3.5 font-bold text-white">{{ item.estudiante }}</td>
                      <td class="px-5 py-3.5 font-mono text-slate-400">{{ item.dni }}</td>
                      <td class="px-5 py-3.5 font-mono text-emerald-400">{{ item.horaMarcacionPuerta || '-' }}</td>
                      <td class="px-5 py-3.5 text-center font-bold">{{ item.estado }}</td>
                      <td class="px-5 py-3.5 text-center">
                        @if (item.estado === 'FALTA_INJUSTIFICADA') {
                          <button (click)="abrirDialogoJustificacion(item.id)" class="text-blue-600 font-semibold">Justificar</button>
                        }
                      </td>
                    </tr>
                  } @empty {
                    <tr>
                      <td colspan="5" class="text-center py-12 text-slate-500 space-y-2">
                        <div class="text-2xl">🕒</div>
                        <p class="font-medium text-slate-400">No se encontraron marcas de asistencia para la fecha indicada.</p>
                        <p class="text-[11px] text-slate-500">Los pases de lista registrados en el aula y las importaciones biométricas se mostrarán aquí.</p>
                      </td>
                    </tr>
                  }
                }
              </tbody>
            </table>
          </div>
        </div>
      }

      <!-- Tab Content: Conciliación Biométrica -->
      @if (tabActiva() === 'CONCILIACION') {
        <div class="space-y-4">
          <div class="rounded-2xl bg-slate-900 border border-slate-800 overflow-hidden shadow-sm">
            <div class="overflow-x-auto">
            <table class="w-full text-left text-xs">
              <thead class="bg-slate-950/70 border-b border-slate-800 text-slate-400 uppercase tracking-wider font-semibold">
                <tr>
                  <th class="px-5 py-3.5">Estudiante</th>
                  <th class="px-5 py-3.5">DNI</th>
                  <th class="px-5 py-3.5">Portería</th>
                  <th class="px-5 py-3.5">Aula</th>
                  <th class="px-5 py-3.5">Tipo de Discrepancia</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-800/60 text-slate-300">
                @for (d of discrepancias(); track d.id) {
                  <tr class="hover:bg-slate-800/40 transition">
                    <td class="px-5 py-3.5 font-bold text-white">{{ d.estudiante }}</td>
                    <td class="px-5 py-3.5 font-mono text-slate-400">{{ d.dni }}</td>
                    <td class="px-5 py-3.5 font-mono text-emerald-400">{{ d.horaPuerta }}</td>
                    <td class="px-5 py-3.5 font-bold text-red-400">{{ d.estadoAula }}</td>
                    <td class="px-5 py-3.5">{{ d.tipoDiscrepancia }}</td>
                  </tr>
                } @empty {
                  <tr>
                    <td colspan="5" class="text-center py-12 text-slate-500 space-y-2">
                      <div class="text-2xl">✓</div>
                      <p class="font-medium text-slate-400">No hay discrepancias registradas en la base de datos.</p>
                      <p class="text-[11px] text-slate-500">Molinete de portería y pase de lista de aula se encuentran perfectamente conciliados.</p>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
            </div>
          </div>
        </div>
      }
    </div>
  `
})
export class AsistenciaComponent implements OnInit {
  private readonly asistenciaService = inject(AsistenciaService);
  private readonly academicoService = inject(AcademicoService);
  readonly anios = signal<AnioLectivoResponseDto[]>([]);
  @ViewChild('registroDialog') private registroDialog!: ElementRef<HTMLDialogElement>;
  @ViewChild('biometricoDialog') private biometricoDialog!: ElementRef<HTMLDialogElement>;
  @ViewChild('justificacionDialog') private justificacionDialog!: ElementRef<HTMLDialogElement>;

  fechaSeleccionada = new Date().toLocaleDateString('sv-SE');
  fechaSesionNueva = new Date().toLocaleDateString('sv-SE');
  anioId = 0;
  readonly error = signal<string | null>(null);
  readonly mensaje = signal<string | null>(null);
  readonly justificacionId = signal<number | null>(null);
  readonly operando = signal(false);
  tabActiva = signal<'AULA' | 'CONCILIACION'>('AULA');
  cargando = signal(false);

  matriculaId = 0;
  horaRegistro = '08:00';
  estadoRegistro: EstadoAsistencia = 'PRESENTE';
  motivoJustificacion = '';
  dniBiometrico = '';
  fechaHoraBiometrico = '';
  dispositivoCodigo = 'PUERTA-1';

  readonly alumnos = signal<AlumnoAsistenciaItem[]>([]);
  readonly discrepancias = signal<DiscrepanciaBiometricaItem[]>([]);

  readonly totalPresentes = computed(() => this.alumnos().filter(a => a.estado === 'PRESENTE').length);
  readonly totalTardanzas = computed(() => this.alumnos().filter(a => a.estado === 'TARDANZA').length);
  readonly totalJustificadas = computed(() => this.alumnos().filter(a => a.estado === 'FALTA_JUSTIFICADA').length);
  readonly totalInjustificadas = computed(() => this.alumnos().filter(a => a.estado === 'FALTA_INJUSTIFICADA').length);

  ngOnInit(): void {
    this.academicoService.listarAnios().subscribe({
      next: anios => { this.anios.set(anios); this.anioId = anios.find(a => a.abierto)?.id ?? anios[0]?.id ?? 0; },
      error: () => this.error.set('No se pudieron consultar los años lectivos.')
    });
    this.cargarAsistencia();
  }

  cargarAsistencia(): void {
    this.cargando.set(true);
    this.asistenciaService.obtenerReporteDiario(this.fechaSeleccionada).subscribe({
      next: (res) => {
        this.cargando.set(false);
        if (res && Array.isArray(res.asistencias)) {
          this.alumnos.set(
            res.asistencias.map((a, idx) => ({
              id: a.id || idx + 1,
              dni: `EST-${a.matriculaId}`,
              estudiante: `Matrícula #${a.matriculaId}`,
              estado: a.estado as EstadoAsistencia,
              horaMarcacionPuerta: a.horaRegistro || '-'
            }))
          );
        } else {
          this.alumnos.set([]);
        }
      },
      error: () => {
        this.cargando.set(false);
        this.alumnos.set([]);
      }
    });

    this.asistenciaService.listarAlertasConciliacion(this.fechaSeleccionada).subscribe({
      next: (res) => {
        if (Array.isArray(res)) {
          this.discrepancias.set(
            res.map((d, idx) => ({
              id: idx + 1,
              estudiante: d.estudianteNombre || `Estudiante #${d.estudianteId}`,
              dni: d.dni || String(d.estudianteId),
              horaPuerta: d.marcoPorteria ? 'Marcó' : 'Sin marca',
              estadoAula: d.presenteAula ? 'Presente' : 'Ausente',
              tipoDiscrepancia: d.tipoDiscrepancia
            }))
          );
        } else {
          this.discrepancias.set([]);
        }
      },
      error: () => {
        this.discrepancias.set([]);
      }
    });
  }

  registrarAsistencia(): void {
    if (this.matriculaId <= 0 || !this.fechaSesionNueva || !this.horaRegistro) {
      this.error.set('Complete matrícula, fecha y hora.'); return;
    }
    this.error.set(null); this.mensaje.set(null); this.operando.set(true);
    this.asistenciaService.registrarAsistenciaAula({ matriculaId: this.matriculaId, fechaSesion: this.fechaSesionNueva, horaRegistro: this.horaRegistro, estado: this.estadoRegistro }).subscribe({
      next: () => { this.operando.set(false); this.cerrarDialogos(); this.mensaje.set('Asistencia guardada en la base de datos.'); this.cargarAsistencia(); },
      error: err => { this.operando.set(false); this.error.set(err?.error?.message ?? 'No se pudo registrar la asistencia.'); }
    });
  }

  justificar(asistenciaId: number): void {
    if (!this.motivoJustificacion.trim()) { this.error.set('Indique el motivo de justificación.'); return; }
    this.error.set(null); this.mensaje.set(null); this.operando.set(true);
    this.asistenciaService.justificarInasistencia({ asistenciaId, motivo: this.motivoJustificacion.trim() }).subscribe({
      next: () => { this.operando.set(false); this.cerrarDialogos(); this.justificacionId.set(null); this.motivoJustificacion = ''; this.mensaje.set('Falta justificada en la base de datos.'); this.cargarAsistencia(); },
      error: err => { this.operando.set(false); this.error.set(err?.error?.message ?? 'No se pudo justificar la falta.'); }
    });
  }

  importarMarca(): void {
    if (!/^[0-9]{8}$/.test(this.dniBiometrico) || !this.fechaHoraBiometrico || !this.dispositivoCodigo.trim()) {
      this.error.set('Complete DNI, fecha/hora y dispositivo.'); return;
    }
    this.error.set(null); this.mensaje.set(null); this.operando.set(true);
    const fechaHora = `${this.fechaHoraBiometrico}:00-05:00`;
    this.asistenciaService.importarLoteBiometrico({ nombreArchivo: `marca-ui-${this.dniBiometrico}-${Date.now()}.json`, marcas: [{ dniLeido: this.dniBiometrico, fechaHora, dispositivoCodigo: this.dispositivoCodigo.trim() }] }).subscribe({
      next: res => { this.operando.set(false); this.cerrarDialogos(); this.mensaje.set(`Marca importada: ${res.marcasValidas} válida(s), ${res.marcasErroneas} errónea(s).`); this.cargarAsistencia(); },
      error: err => { this.operando.set(false); this.error.set(err?.error?.message ?? 'No se pudo importar la marca biométrica.'); }
    });
  }

  ejecutarConciliacion(): void {
    if (!this.anioId) { this.error.set('Indique el año lectivo antes de conciliar.'); return; }
    this.error.set(null);
    this.asistenciaService.conciliarAsistencia(this.anioId, this.fechaSeleccionada, true).subscribe({
      next: (res) => {
        this.mensaje.set('Conciliación ejecutada. Registros procesados: ' + (res?.length || 0));
        this.cargarAsistencia();
      },
      error: () => {
        this.error.set('No se pudo ejecutar la conciliación.');
      }
    });
  }

  abrirDialogoRegistro(): void {
    this.error.set(null);
    this.mensaje.set(null);
    this.fechaSesionNueva = this.fechaSeleccionada;
    this.registroDialog.nativeElement.showModal();
  }

  abrirDialogoBiometrico(): void {
    this.error.set(null);
    this.mensaje.set(null);
    this.dniBiometrico = '';
    this.fechaHoraBiometrico = '';
    this.biometricoDialog.nativeElement.showModal();
  }

  abrirDialogoJustificacion(id: number): void {
    this.error.set(null);
    this.mensaje.set(null);
    this.motivoJustificacion = '';
    this.justificacionId.set(id);
    this.justificacionDialog.nativeElement.showModal();
  }

  cerrarDialogos(): void {
    for (const dialog of [this.registroDialog?.nativeElement, this.biometricoDialog?.nativeElement, this.justificacionDialog?.nativeElement]) {
      if (dialog?.open) dialog.close();
    }
  }

  justificarSeleccionada(): void {
    const id = this.justificacionId();
    if (id !== null) this.justificar(id);
  }
}
