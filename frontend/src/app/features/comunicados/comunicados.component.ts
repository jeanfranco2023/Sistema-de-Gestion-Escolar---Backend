import { Component, ElementRef, inject, signal, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { map } from 'rxjs';
import { ComunicadoService } from '../../core/services/comunicado.service';
import { AcademicoService } from '../../core/services/academico.service';
import { AuthStore } from '../../core/state/auth.store';
import { AnioLectivoResponseDto } from '../../core/models/academico.model';

export interface ComunicadoEscolar {
  id: number;
  titulo: string;
  resumen: string;
  destinatarios: string;
  prioridad: string;
  fechaPublicacion: string;
  autor: string;
  requiereAcuse: boolean;
}

@Component({
  selector: 'app-comunicados',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="space-y-6">
      <!-- Header -->
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 class="text-2xl sm:text-3xl font-extrabold tracking-tight text-white">Comunicados Institucionales</h2>
          <p class="text-sm text-slate-400 mt-1">
            Difusión de circulares oficiales y avisos a la comunidad educativa de la I.E.P. Shuji Kitamura.
          </p>
        </div>
        <div class="flex flex-wrap items-center gap-3">
          <button
            (click)="cargarComunicados()"
            class="px-3 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold cursor-pointer"
          >
            Actualizar BD
          </button>
          @if (store.hasAnyRole(['DIRECCION', 'SECRETARIA'])) { <button
            (click)="nuevoComunicado()"
            class="px-4 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-semibold text-xs sm:text-sm shadow-lg shadow-blue-600/30 transition flex items-center gap-2 cursor-pointer"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
            </svg>
            <span>Publicar Comunicado en BD</span>
          </button> }
        </div>
      </div>

      @if (error()) { <p class="text-sm text-red-400" role="alert">{{ error() }}</p> }
      @if (mensaje()) { <p class="text-sm text-emerald-400" role="status">{{ mensaje() }}</p> }
      <dialog #comunicadoDialog class="school-native-dialog" aria-labelledby="comunicado-dialog-title">
        <div class="school-dialog-shell"><header class="school-dialog-header"><div><h3 id="comunicado-dialog-title">Publicar comunicado</h3><p>El mensaje será visible para la comunidad según su alcance.</p></div><button type="button" class="school-dialog-cancel !min-h-9 !px-3" (click)="cerrarDialogo()" aria-label="Cerrar">✕</button></header>
          <form (ngSubmit)="publicar()" class="school-dialog-form"><div class="school-dialog-body space-y-4">
            @if (error()) { <p class="school-dialog-error" role="alert">{{ error() }}</p> }
            <label class="school-dialog-field">Año lectivo *<select [(ngModel)]="anioLectivoId" name="anioLectivoId" [disabled]="!anios().length" required>@if (!anios().length) { <option [ngValue]="0">Sin años lectivos disponibles</option> } @for (anio of anios(); track anio.id) { <option [ngValue]="anio.id">Año {{ anio.anio }}</option> }</select></label>
            <label class="school-dialog-field">Título *<input [(ngModel)]="titulo" name="titulo" required maxlength="150" placeholder="Asunto del comunicado"></label>
            <label class="school-dialog-field">Contenido *<textarea [(ngModel)]="contenido" name="contenido" required maxlength="20000" rows="6" placeholder="Escribe el mensaje para la comunidad"></textarea></label>
            <label class="inline-flex items-center gap-3 text-sm text-slate-700"><input type="checkbox" [(ngModel)]="requiereAcuse" name="requiereAcuse"> Solicitar confirmación de lectura</label>
          </div><footer class="school-dialog-footer"><button type="button" class="school-dialog-cancel" (click)="cerrarDialogo()">Cancelar</button><button type="submit" [disabled]="publicando()" class="school-dialog-submit">{{ publicando() ? 'Publicando…' : 'Publicar comunicado' }}</button></footer></form>
        </div>
      </dialog>

      <!-- Feed Grid -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
        @if (cargando()) {
          <div class="col-span-full p-12 text-center text-slate-400 rounded-2xl bg-slate-900 border border-slate-800">
            <span class="inline-block w-6 h-6 border-2 border-blue-500/30 border-t-blue-500 rounded-full animate-spin mb-2"></span>
            <p class="text-xs">Consultando comunicados en la base de datos...</p>
          </div>
        } @else {
          @for (item of comunicados(); track item.id) {
            <div class="p-6 rounded-2xl bg-slate-900 border border-slate-800 hover:border-slate-700 transition space-y-3 shadow-sm flex flex-col justify-between">
              <div>
                <div class="flex items-center justify-between gap-2">
                  <span class="px-2.5 py-0.5 rounded-md text-[10px] font-bold tracking-wide uppercase bg-slate-800 text-blue-400 border border-slate-700">
                    Para: {{ item.destinatarios }}
                  </span>
                  @switch (item.prioridad) {
                    @case ('URGENTE') {
                      <span class="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-red-950 text-red-400 border border-red-800">
                        URGENTE
                      </span>
                    }
                    @case ('IMPORTANTE') {
                      <span class="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-amber-950 text-amber-400 border border-amber-800">
                        IMPORTANTE
                      </span>
                    }
                    @default {
                      <span class="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-slate-800 text-slate-400 border border-slate-700">
                        INFORMATIVO
                      </span>
                    }
                  }
                </div>

                <h3 class="text-base font-bold text-white mt-3">{{ item.titulo }}</h3>
                <p class="text-xs text-slate-400 mt-2 leading-relaxed">{{ item.resumen }}</p>
              </div>

              <div class="pt-4 border-t border-slate-800/80 flex items-center justify-between text-[11px] text-slate-500 font-mono">
                <span>Publicado por: {{ item.autor }}</span>
                <span>{{ item.fechaPublicacion }}</span>
              </div>
              @if (store.hasRole('APODERADO') && item.requiereAcuse) {
                <button (click)="confirmarAcuse(item.id)" class="mt-3 p-2 rounded bg-blue-600 text-white text-xs">Confirmar lectura y acuse</button>
              }
            </div>
          } @empty {
            <div class="col-span-full p-12 text-center text-slate-500 rounded-2xl bg-slate-900 border border-slate-800 space-y-2">
              <div class="text-3xl">📢</div>
              <p class="font-medium text-slate-400">No hay comunicados oficiales emitidos en la base de datos.</p>
              <p class="text-xs text-slate-500">Las circulares y notificaciones dirigidas a la comunidad educativa se sincronizarán directamente desde PostgreSQL.</p>
            </div>
          }
        }
      </div>
    </div>
  `
})
export class ComunicadosComponent implements OnInit {
  private readonly comunicadoService = inject(ComunicadoService);
  private readonly academicoService = inject(AcademicoService);
  @ViewChild('comunicadoDialog') private comunicadoDialog!: ElementRef<HTMLDialogElement>;
  readonly store = inject(AuthStore);
  readonly anios = signal<AnioLectivoResponseDto[]>([]);
  readonly publicando = signal(false);
  readonly error = signal<string | null>(null);
  readonly mensaje = signal<string | null>(null);
  anioLectivoId = 0;
  titulo = '';
  contenido = '';
  requiereAcuse = false;

  cargando = signal(false);
  readonly comunicados = signal<ComunicadoEscolar[]>([]);

  ngOnInit(): void {
    this.cargarComunicados();
    if (this.store.hasAnyRole(['DIRECCION', 'SECRETARIA'])) {
      this.academicoService.listarAnios().subscribe({ next: anios => { this.anios.set(anios); this.anioLectivoId = anios.find(a => a.abierto)?.id ?? anios[0]?.id ?? 0; } });
    }
  }

  cargarComunicados(): void {
    this.cargando.set(true);
    const consulta = this.store.hasRole('APODERADO')
      ? this.comunicadoService.consultarBandeja(0, 20).pipe(map(res => res.comunicados))
      : this.comunicadoService.listarPublicados();
    consulta.subscribe({
      next: (res) => {
        this.cargando.set(false);
        if (Array.isArray(res)) {
          this.comunicados.set(
            res.map((c) => ({
              id: c.id,
              titulo: c.titulo || `Comunicado #${c.id}`,
              resumen: c.contenido || 'Sin contenido adicional',
              destinatarios: 'APODERADOS',
              prioridad: 'INFORMATIVO',
              fechaPublicacion: c.fechaPublicacion ? String(c.fechaPublicacion).split('T')[0] : '—',
              autor: c.remitenteUsuarioId ? `Usuario #${c.remitenteUsuarioId}` : 'Dirección',
              requiereAcuse: c.requiereAcuse
            }))
          );
        } else {
          this.comunicados.set([]);
        }
      },
      error: () => {
        this.cargando.set(false);
        this.comunicados.set([]);
      }
    });
  }

  nuevoComunicado(): void {
    this.error.set(null);
    this.mensaje.set(null);
    this.titulo = '';
    this.contenido = '';
    this.requiereAcuse = false;
    this.comunicadoDialog.nativeElement.showModal();
  }

  cerrarDialogo(): void { this.comunicadoDialog.nativeElement.close(); }

  publicar(): void {
    if (!this.anioLectivoId || !this.titulo.trim() || !this.contenido.trim()) { this.error.set('Complete año, título y contenido.'); return; }
    this.publicando.set(true);
    this.error.set(null);
    this.comunicadoService.publicarComunicado({ anioLectivoId: this.anioLectivoId, titulo: this.titulo.trim(), contenido: this.contenido.trim(), requiereAcuse: this.requiereAcuse }).subscribe({
      next: () => { this.publicando.set(false); this.cerrarDialogo(); this.mensaje.set('Comunicado publicado correctamente.'); this.titulo = ''; this.contenido = ''; this.cargarComunicados(); },
      error: err => { this.publicando.set(false); this.error.set(err?.error?.message ?? 'No se pudo publicar el comunicado.'); }
    });
  }

  confirmarAcuse(id: number): void {
    this.error.set(null);
    this.mensaje.set(null);
    this.comunicadoService.confirmarAcuse(id).subscribe({
      next: () => this.mensaje.set('Lectura y acuse confirmados.'),
      error: err => this.error.set(err?.error?.message ?? 'No se pudo confirmar el acuse.')
    });
  }
}
