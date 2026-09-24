import { Component, ElementRef, inject, signal, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AcademicoService } from '../../core/services/academico.service';
import { AulaResponseDto, NivelResponseDto } from '../../core/models/academico.model';

export interface AulaItem {
  id: number;
  codigo: string;
  nombre: string;
  ubicacion: string;
  capacidad: number;
  activa: boolean;
}

export interface NivelItem {
  id: number;
  codigo: string;
  nombre: string;
}

@Component({
  selector: 'app-academico',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="space-y-6">
      <!-- Header -->
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 class="text-2xl sm:text-3xl font-extrabold tracking-tight text-white">Estructura Académica & Aulas</h2>
          <p class="text-sm text-slate-400 mt-1">
            Configuración de niveles educativos, secciones, aforo físico y aulas registradas en la base de datos.
          </p>
        </div>
        <div class="flex items-center gap-3">
          <button
            (click)="abrirModalNuevaAula()"
            class="px-4 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-semibold text-xs sm:text-sm shadow-lg shadow-blue-600/30 transition flex items-center gap-2 cursor-pointer"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
            </svg>
            <span>Nueva Aula en BD</span>
          </button>
        </div>
      </div>

      @if (error()) { <p class="text-red-400 text-sm" role="alert">{{ error() }}</p> }
      <dialog #aulaDialog class="school-native-dialog" aria-labelledby="aula-dialog-title">
        <div class="school-dialog-shell">
          <header class="school-dialog-header">
            <div><h3 id="aula-dialog-title">Registrar aula</h3><p>Completa los datos del ambiente escolar.</p></div>
            <button type="button" class="school-dialog-cancel !min-h-9 !px-3" (click)="cerrarDialogoAula()" aria-label="Cerrar">✕</button>
          </header>
          <form (ngSubmit)="crearAula()" class="school-dialog-form">
            <div class="school-dialog-body space-y-4">
              @if (error()) { <p class="school-dialog-error" role="alert">{{ error() }}</p> }
              <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <label class="school-dialog-field">Código de aula *<input [(ngModel)]="codigo" name="codigo" required maxlength="30" placeholder="Ej. AULA-101" autocomplete="off"></label>
                <label class="school-dialog-field">Nombre de aula *<input [(ngModel)]="nombre" name="nombre" required maxlength="100" placeholder="Ej. Aula de 1.º A"></label>
                <label class="school-dialog-field sm:col-span-2">Ubicación<input [(ngModel)]="ubicacion" name="ubicacion" maxlength="150" placeholder="Pabellón, piso u otra referencia"></label>
                <label class="school-dialog-field">Capacidad *<input type="number" [(ngModel)]="capacidad" name="capacidad" min="1" max="200" required></label>
              </div>
            </div>
            <footer class="school-dialog-footer">
              <button type="button" class="school-dialog-cancel" (click)="cerrarDialogoAula()">Cancelar</button>
              <button type="submit" [disabled]="guardando()" class="school-dialog-submit">{{ guardando() ? 'Guardando…' : 'Crear aula' }}</button>
            </footer>
          </form>
        </div>
      </dialog>

      <!-- Level Cards -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-5">
        <div class="p-6 rounded-2xl bg-slate-900 border border-slate-800">
          <div class="flex items-center justify-between">
            <span class="text-xs font-bold text-amber-400 uppercase tracking-wider">Nivel Inicial</span>
            <span class="px-2 py-0.5 rounded text-[10px] bg-amber-950 text-amber-300 border border-amber-800 font-semibold">
              {{ contarAulasPorFiltro('INIC') }} Aulas
            </span>
          </div>
          <div class="text-2xl font-black text-white mt-3">{{ contarCapacidadPorFiltro('INIC') }} Capacidad</div>
          <p class="text-xs text-slate-400 mt-1">Aulas de 3, 4 y 5 años configuradas en PostgreSQL</p>
        </div>

        <div class="p-6 rounded-2xl bg-slate-900 border border-slate-800">
          <div class="flex items-center justify-between">
            <span class="text-xs font-bold text-emerald-400 uppercase tracking-wider">Nivel Primaria</span>
            <span class="px-2 py-0.5 rounded text-[10px] bg-emerald-950 text-emerald-300 border border-emerald-800 font-semibold">
              {{ contarAulasPorFiltro('PRIM') }} Aulas
            </span>
          </div>
          <div class="text-2xl font-black text-white mt-3">{{ contarCapacidadPorFiltro('PRIM') }} Capacidad</div>
          <p class="text-xs text-slate-400 mt-1">De 1° a 6° de Primaria registradas en PostgreSQL</p>
        </div>

        <div class="p-6 rounded-2xl bg-slate-900 border border-slate-800">
          <div class="flex items-center justify-between">
            <span class="text-xs font-bold text-blue-400 uppercase tracking-wider">Nivel Secundaria</span>
            <span class="px-2 py-0.5 rounded text-[10px] bg-blue-950 text-blue-300 border border-blue-800 font-semibold">
              {{ contarAulasPorFiltro('SEC') }} Aulas
            </span>
          </div>
          <div class="text-2xl font-black text-white mt-3">{{ contarCapacidadPorFiltro('SEC') }} Capacidad</div>
          <p class="text-xs text-slate-400 mt-1">De 1° a 5° de Secundaria registradas en PostgreSQL</p>
        </div>
      </div>

      <!-- Classrooms List -->
      <div class="rounded-2xl bg-slate-900 border border-slate-800 overflow-hidden shadow-sm">
        <div class="px-6 py-4 bg-slate-950/50 border-b border-slate-800 flex items-center justify-between">
          <h3 class="text-sm font-bold text-white">Directorio de Aulas Físicas</h3>
          <button
            (click)="cargarDatos()"
            class="px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold cursor-pointer"
          >
            Actualizar BD
          </button>
        </div>

        <div class="overflow-x-auto">
          <table class="w-full text-left text-xs">
            <thead class="bg-slate-950/70 border-b border-slate-800 text-slate-400 uppercase tracking-wider font-semibold">
              <tr>
                <th class="px-5 py-3.5">ID BD</th>
                <th class="px-5 py-3.5">Código Aula</th>
                <th class="px-5 py-3.5">Nombre & Ubicación</th>
                <th class="px-5 py-3.5">Capacidad Aforo</th>
                <th class="px-5 py-3.5 text-center">Estado en BD</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-800/60 text-slate-300">
              @if (cargando()) {
                <tr>
                  <td colspan="5" class="text-center py-10 text-slate-400">
                    <span class="inline-block w-5 h-5 border-2 border-blue-500/30 border-t-blue-500 rounded-full animate-spin mb-2"></span>
                    <p>Consultando infraestructura escolar en la base de datos...</p>
                  </td>
                </tr>
              } @else {
                @for (aula of aulas(); track aula.id) {
                  <tr class="hover:bg-slate-800/40 transition">
                    <td class="px-5 py-3.5 font-mono text-slate-500">{{ aula.id }}</td>
                    <td class="px-5 py-3.5 font-mono text-blue-400 font-semibold">{{ aula.codigo }}</td>
                    <td class="px-5 py-3.5">
                      <div class="font-bold text-white">{{ aula.nombre }}</div>
                      <div class="text-[11px] text-slate-500">{{ aula.ubicacion || 'Sin ubicación registrada' }}</div>
                    </td>
                    <td class="px-5 py-3.5 font-mono text-white">
                      {{ aula.capacidad }} vacantes
                    </td>
                    <td class="px-5 py-3.5 text-center">
                      @if (aula.activa) {
                        <span class="px-2.5 py-1 rounded-full text-[10px] font-bold bg-emerald-950 text-emerald-400 border border-emerald-800">
                          ACTIVA
                        </span>
                      } @else {
                        <span class="px-2.5 py-1 rounded-full text-[10px] font-bold bg-slate-800 text-slate-400 border border-slate-700">
                          INACTIVA
                        </span>
                      }
                    </td>
                  </tr>
                } @empty {
                  <tr>
                    <td colspan="5" class="text-center py-12 text-slate-500 space-y-2">
                      <div class="text-2xl">🏫</div>
                      <p class="font-medium text-slate-400">No se encontraron aulas físicas en la base de datos.</p>
                      <p class="text-[11px] text-slate-500">Configure los pabellones y aulas escolares en PostgreSQL para visualizar el directorio.</p>
                    </td>
                  </tr>
                }
              }
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `
})
export class AcademicoComponent implements OnInit {
  private readonly academicoService = inject(AcademicoService);
  readonly error = signal<string | null>(null);
  readonly guardando = signal(false);
  codigo = '';
  nombre = '';
  ubicacion = '';
  capacidad = 1;

  cargando = signal(false);
  readonly aulas = signal<AulaItem[]>([]);
  readonly niveles = signal<NivelItem[]>([]);
  @ViewChild('aulaDialog') private aulaDialog!: ElementRef<HTMLDialogElement>;

  ngOnInit(): void {
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.cargando.set(true);
    this.academicoService.listarAulas().subscribe({
      next: (res: AulaResponseDto[]) => {
        this.cargando.set(false);
        if (Array.isArray(res)) {
          this.aulas.set(
            res.map(a => ({
              id: a.id,
              codigo: a.codigo,
              nombre: a.nombre,
              ubicacion: a.ubicacion,
              capacidad: a.capacidad,
              activa: a.activa ?? true
            }))
          );
        } else {
          this.aulas.set([]);
        }
      },
      error: () => {
        this.cargando.set(false);
        this.aulas.set([]);
      }
    });

    this.academicoService.listarNiveles().subscribe({
      next: (res: NivelResponseDto[]) => {
        if (Array.isArray(res)) {
          this.niveles.set(res.map(n => ({ id: n.id, codigo: n.codigo, nombre: n.nombre })));
        } else {
          this.niveles.set([]);
        }
      },
      error: () => {
        this.niveles.set([]);
      }
    });
  }

  contarAulasPorFiltro(codigoPrefix: string): number {
    return this.aulas().filter(a => a.codigo && a.codigo.toUpperCase().includes(codigoPrefix)).length;
  }

  contarCapacidadPorFiltro(codigoPrefix: string): number {
    return this.aulas()
      .filter(a => a.codigo && a.codigo.toUpperCase().includes(codigoPrefix))
      .reduce((sum, a) => sum + (a.capacidad || 0), 0);
  }

  abrirModalNuevaAula(): void {
    this.error.set(null);
    this.codigo = '';
    this.nombre = '';
    this.ubicacion = '';
    this.capacidad = 1;
    this.aulaDialog.nativeElement.showModal();
  }

  cerrarDialogoAula(): void {
    this.aulaDialog.nativeElement.close();
  }

  crearAula(): void {
    if (!this.codigo.trim() || !this.nombre.trim() || this.capacidad < 1) { this.error.set('Complete código, nombre y capacidad.'); return; }
    this.guardando.set(true); this.error.set(null);
    this.academicoService.crearAula({ codigo: this.codigo.trim(), nombre: this.nombre.trim(), ubicacion: this.ubicacion.trim(), capacidad: this.capacidad }).subscribe({
      next: () => { this.guardando.set(false); this.cerrarDialogoAula(); this.error.set(null); this.cargarDatos(); },
      error: err => { this.guardando.set(false); this.error.set(err?.error?.message ?? 'No se pudo crear el aula.'); }
    });
  }
}
