import { Component, inject, signal, OnInit, TemplateRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { MatriculaService } from '../../core/services/matricula.service';
import {
  FichaMatriculaResponseDto,
  EstudianteResponseDto,
  RegistrarEstudianteRequestDto,
  TipoDocumento,
  Genero
} from '../../core/models/matricula.model';
import { AcademicoService } from '../../core/services/academico.service';
import { AnioLectivoResponseDto, SeccionResponseDto } from '../../core/models/academico.model';

export interface EstudianteMatriculadoItem {
  id: number;
  codigoMatricula: string;
  estudianteId: number;
  estado: string;
  fechaMatricula: string;
  observaciones?: string;
}

@Component({
  selector: 'app-matriculas',
  standalone: true,
  imports: [CommonModule, FormsModule, MatDialogModule, MatButtonModule, RouterLink],
  template: `
    <div class="space-y-6">
      <!-- Top Banner / Alert -->
      @if (mensajeExito()) {
        <div class="p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 flex items-center justify-between shadow-lg shadow-emerald-950/20">
          <div class="flex items-center gap-3">
            <span class="p-1.5 rounded-lg bg-emerald-500/20 text-emerald-300">
              <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
              </svg>
            </span>
            <div>
              <p class="font-bold text-sm text-emerald-300">Operación Exitosa</p>
              <p class="text-xs text-emerald-400/90">{{ mensajeExito() }}</p>
            </div>
          </div>
          <button (click)="mensajeExito.set(null)" class="text-emerald-400 hover:text-emerald-200 cursor-pointer text-xs font-semibold px-2 py-1">
            ✕
          </button>
        </div>
      }

      @if (error()) {
        <div class="p-4 rounded-2xl bg-rose-500/10 border border-rose-500/30 text-rose-400 flex items-center justify-between shadow-lg shadow-rose-950/20">
          <div class="flex items-center gap-3">
            <span class="p-1.5 rounded-lg bg-rose-500/20 text-rose-300">
              <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </span>
            <div>
              <p class="font-bold text-sm text-rose-300">Error</p>
              <p class="text-xs text-rose-400/90">{{ error() }}</p>
            </div>
          </div>
          <button (click)="error.set(null)" class="text-rose-400 hover:text-rose-200 cursor-pointer text-xs font-semibold px-2 py-1">
            ✕
          </button>
        </div>
      }

      <!-- Page Header -->
      <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div class="flex items-center gap-2 mb-1">
            <span class="px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-blue-500/10 text-blue-400 border border-blue-500/20">
              Módulo Académico
            </span>
            <span class="text-xs text-slate-500">•</span>
            <span class="text-xs text-slate-400 font-medium">Año Lectivo {{ anioActivoTexto() }}</span>
          </div>
          <h2 class="text-2xl sm:text-3xl font-extrabold tracking-tight text-white">
            Padrón de Estudiantes & Matrículas
          </h2>
          <p class="text-sm text-slate-400 mt-1">
            Gestión centralizada de estudiantes, registro oficial y asignación de vacantes en secciones.
          </p>
        </div>

        <!-- Action Buttons -->
        <div class="flex flex-wrap items-center gap-3">
          <button
            id="btn-abrir-registro-alumno"
            (click)="abrirModalNuevoAlumno()"
            class="px-4 py-2.5 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 text-white font-semibold text-xs sm:text-sm shadow-lg shadow-blue-600/30 transition flex items-center gap-2 cursor-pointer transform hover:-translate-y-0.5"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
            </svg>
            <span>Registrar Alumno</span>
          </button>

          <button
            id="btn-abrir-nueva-matricula"
            (click)="abrirModalNuevaMatricula()"
            class="px-4 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-200 hover:text-white font-semibold text-xs sm:text-sm transition flex items-center gap-2 cursor-pointer"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            <span>Asignar Matrícula</span>
          </button>
          <a
            routerLink="/admin/solicitudes-matricula"
            class="px-4 py-2.5 rounded-xl bg-white hover:bg-slate-50 border border-slate-200 text-slate-700 font-semibold text-xs sm:text-sm transition flex items-center gap-2"
          >
            <span>Solicitudes públicas</span>
          </a>
        </div>
      </div>

      <!-- Main Navigation Tabs -->
      <div class="flex items-center gap-2 border-b border-slate-800">
        <button
          id="tab-estudiantes"
          (click)="pestanaActiva.set('estudiantes')"
          [class]="pestanaActiva() === 'estudiantes'
            ? 'px-4 py-3 border-b-2 border-blue-500 text-blue-400 font-bold text-sm flex items-center gap-2 transition cursor-pointer'
            : 'px-4 py-3 border-b-2 border-transparent text-slate-400 hover:text-slate-200 font-medium text-sm flex items-center gap-2 transition cursor-pointer'"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
          </svg>
          <span>Padrón de Estudiantes</span>
          <span class="px-2 py-0.5 rounded-full text-[10px] font-mono bg-slate-800 text-slate-300">
            {{ listaEstudiantes().length }}
          </span>
        </button>

        <button
          id="tab-matriculas"
          (click)="pestanaActiva.set('matriculas')"
          [class]="pestanaActiva() === 'matriculas'
            ? 'px-4 py-3 border-b-2 border-blue-500 text-blue-400 font-bold text-sm flex items-center gap-2 transition cursor-pointer'
            : 'px-4 py-3 border-b-2 border-transparent text-slate-400 hover:text-slate-200 font-medium text-sm flex items-center gap-2 transition cursor-pointer'"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
          </svg>
          <span>Matrículas por Sección</span>
          <span class="px-2 py-0.5 rounded-full text-[10px] font-mono bg-slate-800 text-slate-300">
            {{ estudiantes().length }}
          </span>
        </button>
      </div>

      <!-- ============================================================== -->
      <!-- TAB 1: PADRON DE ESTUDIANTES (LISTA COMPLETA DE ALUMNOS)       -->
      <!-- ============================================================== -->
      @if (pestanaActiva() === 'estudiantes') {
        <div class="space-y-4">
          <!-- Filter / Search Bar for Students -->
          <div class="p-4 rounded-2xl bg-slate-900 border border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-4">
            <div class="w-full sm:w-96 relative">
              <span class="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-500">
                <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
              </span>
              <input
                id="input-filtro-estudiante"
                type="text"
                [(ngModel)]="busquedaEstudiante"
                placeholder="Buscar por DNI, Nombres o Apellidos..."
                class="w-full pl-10 pr-4 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs placeholder-slate-500 focus:outline-none focus:border-blue-500 transition"
              />
            </div>

            <div class="flex items-center gap-3 w-full sm:w-auto justify-end">
              <button
                (click)="cargarEstudiantes()"
                class="px-3.5 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold transition cursor-pointer flex items-center gap-2 border border-slate-700"
              >
                <svg xmlns="http://www.w3.org/2000/svg" class="w-3.5 h-3.5 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                </svg>
                <span>Actualizar Padrón</span>
              </button>
            </div>
          </div>

          <!-- Students Table -->
          <div class="rounded-2xl bg-slate-900 border border-slate-800 overflow-hidden shadow-sm">
            <div class="overflow-x-auto">
              <table id="tabla-estudiantes" class="w-full text-left text-xs">
                <thead class="bg-slate-950/80 border-b border-slate-800 text-slate-400 uppercase tracking-wider font-semibold">
                  <tr>
                    <th class="px-5 py-3.5">ID</th>
                    <th class="px-5 py-3.5">Documento</th>
                    <th class="px-5 py-3.5">Apellidos y Nombres</th>
                    <th class="px-5 py-3.5">F. Nacimiento</th>
                    <th class="px-5 py-3.5">Género</th>
                    <th class="px-5 py-3.5">Cód. SIAGIE</th>
                    <th class="px-5 py-3.5">Grupo Sang.</th>
                    <th class="px-5 py-3.5">Estado BD</th>
                    <th class="px-5 py-3.5 text-right">Acciones</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-slate-800/60 text-slate-300">
                  @if (cargandoEstudiantes()) {
                    <tr>
                      <td colspan="9" class="text-center py-12 text-slate-400">
                        <span class="inline-block w-6 h-6 border-2 border-blue-500/30 border-t-blue-500 rounded-full animate-spin mb-2"></span>
                        <p class="font-medium text-xs">Consultando estudiantes registrados en la base de datos...</p>
                      </td>
                    </tr>
                  } @else {
                    @for (est of estudiantesPaginados(); track est.id) {
                      <tr class="hover:bg-slate-800/40 transition group">
                        <td class="px-5 py-3.5 font-mono text-slate-500 font-semibold">#{{ est.id }}</td>
                        <td class="px-5 py-3.5 font-mono">
                          <span class="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-800 text-slate-300 border border-slate-700 mr-1.5">
                            {{ est.tipoDocumento || 'DNI' }}
                          </span>
                          <span class="text-blue-400 font-semibold">{{ est.numeroDocumento }}</span>
                        </td>
                        <td class="px-5 py-3.5">
                          <div class="font-semibold text-white">
                            {{ est.apellidoPaterno }} {{ est.apellidoMaterno }}, {{ est.nombres }}
                          </div>
                          @if (est.alergiasCondiciones) {
                            <div class="text-[10px] text-amber-400 truncate max-w-xs mt-0.5">
                              ⚠️ {{ est.alergiasCondiciones }}
                            </div>
                          }
                        </td>
                        <td class="px-5 py-3.5 text-slate-400 font-mono">{{ est.fechaNacimiento || '-' }}</td>
                        <td class="px-5 py-3.5">
                          @if (est.genero === 'M') {
                            <span class="px-2 py-0.5 rounded-full text-[10px] font-medium bg-blue-500/10 text-blue-400 border border-blue-500/20">
                              Masculino
                            </span>
                          } @else if (est.genero === 'F') {
                            <span class="px-2 py-0.5 rounded-full text-[10px] font-medium bg-pink-500/10 text-pink-400 border border-pink-500/20">
                              Femenino
                            </span>
                          } @else {
                            <span class="text-slate-500">-</span>
                          }
                        </td>
                        <td class="px-5 py-3.5 font-mono text-slate-400">{{ est.codigoEstudianteSiagie || 'No asignado' }}</td>
                        <td class="px-5 py-3.5 font-mono text-slate-300">{{ est.grupoSanguineo || '-' }}</td>
                        <td class="px-5 py-3.5">
                          <span class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[10px] font-bold bg-emerald-950 text-emerald-300 border border-emerald-800">
                            <span class="w-1.5 h-1.5 rounded-full bg-emerald-400"></span>
                            Activo
                          </span>
                        </td>
                        <td class="px-5 py-3.5 text-right">
                          <button
                            (click)="prepararMatriculaParaEstudiante(est)"
                            class="px-2.5 py-1.5 rounded-lg bg-blue-600/20 hover:bg-blue-600 text-blue-300 hover:text-white border border-blue-500/30 transition text-[11px] font-medium cursor-pointer"
                          >
                            Matricular
                          </button>
                        </td>
                      </tr>
                    } @empty {
                      <tr>
                        <td colspan="9" class="text-center py-12 text-slate-500 space-y-2">
                          <div class="text-3xl">👨‍🎓</div>
                          <p class="font-medium text-slate-400">No se encontraron estudiantes registrados.</p>
                          <p class="text-[11px] text-slate-500">Haz clic en "Registrar Alumno" para dar de alta al primer estudiante en la base de datos.</p>
                        </td>
                      </tr>
                    }
                  }
                </tbody>
              </table>
            </div>
          </div>
        </div>
      }

      <!-- ============================================================== -->
      <!-- TAB 2: MATRICULAS POR SECCION                                  -->
      <!-- ============================================================== -->
      @if (pestanaActiva() === 'matriculas') {
        <div class="space-y-4">
          <!-- Filters Bar -->
          <div class="p-4 rounded-2xl bg-slate-900 border border-slate-800 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <div>
              <label class="block text-xs font-semibold text-slate-400 mb-1">Año Lectivo</label>
              <select
                [(ngModel)]="anioLectivoId"
                (change)="cargarSecciones()"
                class="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs focus:outline-none focus:border-blue-500"
              >
                @for (anio of anios(); track anio.id) {
                  <option [ngValue]="anio.id">{{ anio.anio }} {{ anio.abierto ? '(Activo)' : '' }}</option>
                }
              </select>
            </div>

            <div>
              <label class="block text-xs font-semibold text-slate-400 mb-1">Sección en BD</label>
              <select
                [(ngModel)]="seccionIdSeleccionada"
                (change)="cargarMatriculas()"
                class="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs focus:outline-none focus:border-blue-500"
              >
                @for (seccion of secciones(); track seccion.id) {
                  <option [ngValue]="seccion.id">{{ obtenerNombreSeccion(seccion) }}</option>
                }
              </select>
            </div>

            <div>
              <label class="block text-xs font-semibold text-slate-400 mb-1">Estado de Matrícula</label>
              <select
                [(ngModel)]="filtroEstado"
                class="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs focus:outline-none focus:border-blue-500"
              >
                <option value="TODOS">Todos los Estados</option>
                <option value="MATRICULADO">Matriculado</option>
                <option value="RESERVADA_TEMPORAL">Reserva temporal</option>
                <option value="SOLICITADA">Solicitada</option>
                <option value="TRASLADADO">Trasladado</option>
              </select>
            </div>

            <div class="flex items-end">
              <button
                (click)="cargarMatriculas()"
                class="w-full py-2 px-3 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold transition cursor-pointer flex items-center justify-center gap-1.5 border border-slate-700"
              >
                <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                </svg>
                <span>Consultar BD</span>
              </button>
            </div>
          </div>

          <!-- Matriculas Table -->
          <div class="rounded-2xl bg-slate-900 border border-slate-800 overflow-hidden shadow-sm">
            <div class="overflow-x-auto">
              <table class="w-full text-left text-xs">
                <thead class="bg-slate-950/70 border-b border-slate-800 text-slate-400 uppercase tracking-wider font-semibold">
                  <tr>
                    <th class="px-5 py-3.5">ID BD</th>
                    <th class="px-5 py-3.5">Código Matrícula</th>
                    <th class="px-5 py-3.5">Estudiante ID</th>
                    <th class="px-5 py-3.5">Fecha Registro</th>
                    <th class="px-5 py-3.5">Estado en BD</th>
                    <th class="px-5 py-3.5">Observaciones</th>
                    <th class="px-5 py-3.5 text-right">Acción</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-slate-800/60 text-slate-300">
                  @if (cargando()) {
                    <tr>
                      <td colspan="7" class="text-center py-10 text-slate-400">
                        <span class="inline-block w-5 h-5 border-2 border-blue-500/30 border-t-blue-500 rounded-full animate-spin mb-2"></span>
                        <p>Consultando matrículas en la base de datos...</p>
                      </td>
                    </tr>
                  } @else {
                    @for (est of estudiantesFiltrados(); track est.id) {
                      <tr class="hover:bg-slate-800/40 transition">
                        <td class="px-5 py-3.5 font-mono text-slate-500">#{{ est.id }}</td>
                        <td class="px-5 py-3.5 font-mono text-blue-400 font-semibold">{{ est.codigoMatricula }}</td>
                        <td class="px-5 py-3.5 font-mono text-slate-300">Estudiante #{{ est.estudianteId }}</td>
                        <td class="px-5 py-3.5 text-slate-400 font-mono">{{ est.fechaMatricula }}</td>
                        <td class="px-5 py-3.5">
                          <span class="px-2 py-1 rounded-full text-[10px] font-bold bg-blue-950 text-blue-300 border border-blue-800">
                            {{ est.estado }}
                          </span>
                        </td>
                        <td class="px-5 py-3.5 text-slate-400 truncate max-w-xs">{{ est.observaciones || '-' }}</td>
                        <td class="px-5 py-3.5 text-right">
                          @if (est.estado === 'SOLICITADA' || est.estado === 'RESERVADA_TEMPORAL') {
                            <button
                              type="button"
                              (click)="confirmarMatricula(est.id)"
                              [disabled]="confirmandoMatricula() === est.id"
                              class="mr-2 rounded-lg bg-emerald-600 px-3 py-2 font-semibold text-white transition hover:bg-emerald-500 disabled:cursor-wait disabled:opacity-60"
                            >
                              {{ confirmandoMatricula() === est.id ? 'Validando pago…' : 'Confirmar matrícula' }}
                            </button>
                          }
                          <button
                            type="button"
                            (click)="verFicha(est.id)"
                            class="rounded-lg bg-slate-800 px-3 py-2 font-medium text-slate-300 transition hover:bg-slate-700 hover:text-white"
                          >
                            Ver Ficha
                          </button>
                        </td>
                      </tr>
                    } @empty {
                      <tr>
                        <td colspan="7" class="text-center py-12 text-slate-500 space-y-2">
                          <div class="text-2xl">📋</div>
                          <p class="font-medium text-slate-400">No se encontraron matrículas en esta sección.</p>
                          <p class="text-[11px] text-slate-500">Los registros generados se sincronizan directamente con PostgreSQL.</p>
                        </td>
                      </tr>
                    }
                  }
                </tbody>
              </table>
            </div>
          </div>
        </div>
      }

      <!-- ============================================================== -->
      <!-- MODAL: REGISTRAR NUEVO ALUMNO EN BD (WEB FORM)                 -->
      <!-- ============================================================== -->
      <ng-template #nuevoAlumnoDialog>
          <div class="school-dialog-shell w-full max-w-2xl p-1">
            <!-- Modal Header -->
            <div class="school-dialog-header flex items-center justify-between border-b border-slate-800 pb-4">
              <div class="flex items-center gap-3">
                <div class="p-2.5 rounded-xl bg-blue-600/20 text-blue-400 border border-blue-500/30">
                  <svg xmlns="http://www.w3.org/2000/svg" class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
                  </svg>
                </div>
                <div>
                  <h3 class="text-lg font-bold text-white">Registrar Nuevo Alumno</h3>
                  <p class="text-xs text-slate-400">El alumno será dado de alta directamente en la base de datos PostgreSQL.</p>
                </div>
              </div>
              <button
                id="btn-cerrar-modal-alumno"
                (click)="cerrarModalNuevoAlumno()"
                class="text-slate-400 hover:text-white p-2 rounded-lg hover:bg-slate-800 transition cursor-pointer"
              >
                ✕
              </button>
            </div>

            <!-- Modal Form -->
            <form id="form-registro-alumno" (ngSubmit)="guardarEstudiante()" class="school-dialog-form space-y-4">
              <div class="school-dialog-body space-y-4">
              <!-- Grid Row 1: Documento -->
              <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div>
                  <label class="block text-xs font-semibold text-slate-300 mb-1">Tipo de Documento *</label>
                  <select
                    id="nuevo-alumno-tipo-doc"
                    [(ngModel)]="nuevoAlumno.tipoDocumento"
                    name="tipoDoc"
                    required
                    class="w-full px-3 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs focus:outline-none focus:border-blue-500"
                  >
                    <option value="DNI">DNI (Perú)</option>
                    <option value="CE">Carné de Extranjería (CE)</option>
                    <option value="PASAPORTE">Pasaporte</option>
                  </select>
                </div>

                <div class="sm:col-span-2">
                  <label class="block text-xs font-semibold text-slate-300 mb-1">Número de Documento *</label>
                  <div class="flex gap-2"><input
                    id="nuevo-alumno-numero-doc"
                    type="text"
                    [(ngModel)]="nuevoAlumno.numeroDocumento"
                    name="numeroDoc"
                    required
                    maxlength="15"
                    placeholder="Ej. 74839201"
                    class="min-w-0 flex-1 px-3 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs placeholder-slate-500 focus:outline-none focus:border-blue-500"
                  />
                  <button type="button" (click)="consultarDni()" [disabled]="consultandoDni() || nuevoAlumno.tipoDocumento !== 'DNI'" class="px-3 py-2.5 rounded-xl bg-blue-600 text-white text-xs font-semibold disabled:opacity-50">{{ consultandoDni() ? 'Consultando...' : 'Consultar DNI' }}</button></div>
                </div>
              </div>

              <p class="text-xs text-slate-400">Los datos del proveedor externo son referenciales. Revisa nombres y apellidos antes de guardar.</p>
              @if (error()) {
                <p class="text-xs text-rose-400" role="alert">{{ error() }}</p>
              }

              <!-- Grid Row 2: Nombres y Apellidos -->
              <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div>
                  <label class="block text-xs font-semibold text-slate-300 mb-1">Nombres *</label>
                  <input
                    id="nuevo-alumno-nombres"
                    type="text"
                    [(ngModel)]="nuevoAlumno.nombres"
                    name="nombres"
                    required
                    maxlength="100"
                    placeholder="Ej. Mateo Alejandro"
                    class="w-full px-3 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs placeholder-slate-500 focus:outline-none focus:border-blue-500"
                  />
                </div>

                <div>
                  <label class="block text-xs font-semibold text-slate-300 mb-1">Apellido Paterno *</label>
                  <input
                    id="nuevo-alumno-ape-paterno"
                    type="text"
                    [(ngModel)]="nuevoAlumno.apellidoPaterno"
                    name="apellidoPaterno"
                    required
                    maxlength="80"
                    placeholder="Ej. Quispe"
                    class="w-full px-3 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs placeholder-slate-500 focus:outline-none focus:border-blue-500"
                  />
                </div>

                <div>
                  <label class="block text-xs font-semibold text-slate-300 mb-1">Apellido Materno *</label>
                  <input
                    id="nuevo-alumno-ape-materno"
                    type="text"
                    [(ngModel)]="nuevoAlumno.apellidoMaterno"
                    name="apellidoMaterno"
                    required
                    maxlength="80"
                    placeholder="Ej. Mendoza"
                    class="w-full px-3 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs placeholder-slate-500 focus:outline-none focus:border-blue-500"
                  />
                </div>
              </div>

              <!-- Grid Row 3: Nacimiento, Genero, Grupo Sanguineo -->
              <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div>
                  <label class="block text-xs font-semibold text-slate-300 mb-1">Fecha de Nacimiento *</label>
                  <input
                    id="nuevo-alumno-fecha-nac"
                    type="date"
                    [(ngModel)]="nuevoAlumno.fechaNacimiento"
                    name="fechaNacimiento"
                    required
                    class="w-full px-3 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs focus:outline-none focus:border-blue-500"
                  />
                </div>

                <div>
                  <label class="block text-xs font-semibold text-slate-300 mb-1">Género *</label>
                  <select
                    id="nuevo-alumno-genero"
                    [(ngModel)]="nuevoAlumno.genero"
                    name="genero"
                    required
                    class="w-full px-3 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs focus:outline-none focus:border-blue-500"
                  >
                    <option value="M">Masculino</option>
                    <option value="F">Femenino</option>
                  </select>
                </div>

                <div>
                  <label class="block text-xs font-semibold text-slate-300 mb-1">Grupo Sanguíneo</label>
                  <select
                    id="nuevo-alumno-grupo-sang"
                    [(ngModel)]="nuevoAlumno.grupoSanguineo"
                    name="grupoSanguineo"
                    class="w-full px-3 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs focus:outline-none focus:border-blue-500"
                  >
                    <option value="O+">O+</option>
                    <option value="O-">O-</option>
                    <option value="A+">A+</option>
                    <option value="A-">A-</option>
                    <option value="B+">B+</option>
                    <option value="B-">B-</option>
                    <option value="AB+">AB+</option>
                    <option value="AB-">AB-</option>
                  </select>
                </div>
              </div>

              <!-- Grid Row 4: Codigo SIAGIE & Alergias -->
              <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div>
                  <label class="block text-xs font-semibold text-slate-300 mb-1">Cód. Modular SIAGIE</label>
                  <input
                    id="nuevo-alumno-siagie"
                    type="text"
                    [(ngModel)]="nuevoAlumno.codigoEstudianteSiagie"
                    name="codigoSiagie"
                    maxlength="14"
                    placeholder="Ej. 2026-74839201"
                    class="w-full px-3 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs placeholder-slate-500 focus:outline-none focus:border-blue-500"
                  />
                </div>

                <div class="sm:col-span-2">
                  <label class="block text-xs font-semibold text-slate-300 mb-1">Alergias o Condiciones Médicas</label>
                  <input
                    id="nuevo-alumno-alergias"
                    type="text"
                    [(ngModel)]="nuevoAlumno.alergiasCondiciones"
                    name="alergias"
                    maxlength="4000"
                    placeholder="Ej. Ninguna alergia conocida / Alérgico a penicilina"
                    class="w-full px-3 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs placeholder-slate-500 focus:outline-none focus:border-blue-500"
                  />
                </div>
              </div>

              </div>
              <!-- Modal Footer -->
              <div class="school-dialog-footer flex items-center justify-end gap-3 pt-4 border-t border-slate-800">
                <button
                  type="button"
                  (click)="cerrarModalNuevoAlumno()"
                  class="px-4 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold transition cursor-pointer"
                >
                  Cancelar
                </button>
                <button
                  id="btn-guardar-alumno"
                  type="submit"
                  [disabled]="guardandoAlumno() || consultandoDni()"
                  class="px-5 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold shadow-lg shadow-blue-600/30 transition flex items-center gap-2 cursor-pointer disabled:opacity-50"
                >
                  @if (guardandoAlumno()) {
                    <span class="inline-block w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
                    <span>Guardando en BD...</span>
                  } @else {
                    <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                    </svg>
                    <span>Guardar y Registrar Alumno</span>
                  }
                </button>
              </div>
            </form>
          </div>
      </ng-template>

      <!-- ============================================================== -->
      <!-- MODAL: ASIGNAR MATRICULA A SECCION                             -->
      <!-- ============================================================== -->
      <ng-template #solicitudDialog>
          <div class="school-dialog-shell w-full max-w-lg p-1">
            <div class="school-dialog-header flex items-center justify-between border-b border-slate-800 pb-4">
              <h3 class="text-lg font-bold text-white">Asignar Solicitud de Matrícula</h3>
              <button (click)="cerrarSolicitudDialog()" class="text-slate-400 hover:text-white p-2" aria-label="Cerrar diálogo">✕</button>
            </div>

            <form (ngSubmit)="solicitar()" class="school-dialog-form space-y-4">
              <div class="school-dialog-body space-y-4">
              <div>
                <label class="block text-xs font-semibold text-slate-300 mb-1">Año Lectivo</label>
                <select [(ngModel)]="anioLectivoId" name="anio" (change)="cargarSecciones()" required class="w-full px-3 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs focus:outline-none focus:border-blue-500">
                  @for (anio of anios(); track anio.id) {
                    <option [ngValue]="anio.id">{{ anio.anio }} {{ anio.abierto ? '(Vigente)' : '' }}</option>
                  }
                </select>
              </div>

              <div>
                <label class="block text-xs font-semibold text-slate-300 mb-1">Estudiante Registrado *</label>
                <select [(ngModel)]="nuevoEstudianteId" name="estudiante" required class="w-full px-3 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs focus:outline-none focus:border-blue-500">
                  <option [ngValue]="0" disabled>Seleccionar estudiante...</option>
                  @for (est of listaEstudiantes(); track est.id) {
                    <option [ngValue]="est.id">
                      #{{ est.id }} - {{ est.numeroDocumento }} · {{ est.apellidoPaterno }} {{ est.apellidoMaterno }}, {{ est.nombres }}
                    </option>
                  }
                </select>
              </div>

              <div>
                <label class="block text-xs font-semibold text-slate-300 mb-1">Sección de Destino *</label>
                <select [(ngModel)]="seccionIdSeleccionada" name="seccion" required class="w-full px-3 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs focus:outline-none focus:border-blue-500">
                  @for (s of secciones(); track s.id) {
                    <option [ngValue]="s.id">{{ obtenerNombreSeccion(s) }}</option>
                  }
                </select>
              </div>

              <div>
                <label class="block text-xs font-semibold text-slate-300 mb-1">Observaciones</label>
                <textarea
                  [(ngModel)]="nuevasObservaciones"
                  name="observaciones"
                  maxlength="4000"
                  rows="3"
                  placeholder="Observaciones de la matrícula..."
                  class="w-full px-3 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs placeholder-slate-500 focus:outline-none focus:border-blue-500"
                ></textarea>
              </div>

              </div>
              <div class="school-dialog-footer flex items-center justify-end gap-3 pt-4 border-t border-slate-800">
                <button type="button" (click)="cerrarSolicitudDialog()" class="px-4 py-2 rounded-xl bg-slate-800 text-slate-300 text-xs font-semibold">
                  Cancelar
                </button>
                <button type="submit" [disabled]="guardando()" class="px-5 py-2 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold disabled:opacity-50">
                  {{ guardando() ? 'Guardando...' : 'Asignar Matrícula' }}
                </button>
              </div>
            </form>
          </div>
      </ng-template>

      <!-- ============================================================== -->
      <!-- MODAL: FICHA DE MATRICULA DETALLADA                            -->
      <!-- ============================================================== -->
      <ng-template #fichaDialog>
          @if (ficha()) {
          <div class="school-dialog-shell w-full max-w-xl p-1">
            <div class="school-dialog-header flex justify-between items-center border-b border-slate-800 pb-3">
              <h3 class="font-bold text-white text-base">Ficha Oficial de Matrícula #{{ ficha()!.matricula.id }}</h3>
              <button (click)="cerrarFichaDialog()" class="text-slate-400 hover:text-white" aria-label="Cerrar ficha">✕</button>
            </div>
            <div class="space-y-3 text-xs text-slate-300">
              <div class="p-3.5 rounded-xl bg-slate-950 border border-slate-800 space-y-1">
                <p class="font-semibold text-blue-400">Datos del Estudiante:</p>
                <p>{{ ficha()!.estudiante.apellidoPaterno }} {{ ficha()!.estudiante.apellidoMaterno }}, {{ ficha()!.estudiante.nombres }}</p>
                <p class="text-slate-400">Doc: {{ ficha()!.estudiante.numeroDocumento }} · F. Nac: {{ ficha()!.estudiante.fechaNacimiento || '-' }}</p>
              </div>
              <div class="p-3.5 rounded-xl bg-slate-950 border border-slate-800 space-y-1">
                <p class="font-semibold text-emerald-400">Datos de la Matrícula:</p>
                <p>Sección ID: {{ ficha()!.matricula.seccionId }} · Estado: <span class="font-bold text-white">{{ ficha()!.matricula.estadoMatricula }}</span></p>
                <p class="text-slate-400">Fecha: {{ ficha()!.matricula.fechaMatricula }}</p>
              </div>
            </div>
            <div class="flex justify-end pt-2">
              <button (click)="cerrarFichaDialog()" class="px-4 py-2 rounded-xl bg-slate-800 text-slate-200 text-xs font-semibold">
                Cerrar
              </button>
            </div>
          </div>
          }
      </ng-template>
    </div>
  `
})
export class MatriculasComponent implements OnInit {
  private readonly dialog = inject(MatDialog);
  private readonly matriculaService = inject(MatriculaService);
  private readonly academicoService = inject(AcademicoService);

  @ViewChild('nuevoAlumnoDialog') private nuevoAlumnoDialog!: TemplateRef<unknown>;
  @ViewChild('solicitudDialog') private solicitudDialog!: TemplateRef<unknown>;
  @ViewChild('fichaDialog') private fichaDialog!: TemplateRef<unknown>;
  private alumnoDialogRef?: MatDialogRef<unknown>;
  private solicitudDialogRef?: MatDialogRef<unknown>;
  private fichaDialogRef?: MatDialogRef<unknown>;

  readonly anios = signal<AnioLectivoResponseDto[]>([]);
  readonly secciones = signal<SeccionResponseDto[]>([]);
  readonly pestanaActiva = signal<'estudiantes' | 'matriculas'>('estudiantes');

  readonly mostrarModalNuevoAlumno = signal(false);
  readonly guardandoAlumno = signal(false);
  readonly consultandoDni = signal(false);

  readonly mostrarSolicitud = signal(false);
  readonly guardando = signal(false);
  readonly cargando = signal(false);
  readonly cargandoEstudiantes = signal(false);

  readonly error = signal<string | null>(null);
  readonly mensajeExito = signal<string | null>(null);
  readonly confirmandoMatricula = signal<number | null>(null);
  readonly ficha = signal<FichaMatriculaResponseDto | null>(null);

  readonly listaEstudiantes = signal<EstudianteResponseDto[]>([]);
  readonly estudiantes = signal<EstudianteMatriculadoItem[]>([]);

  busqueda = '';
  busquedaEstudiante = '';
  seccionIdSeleccionada = 0;
  anioLectivoId = 0;
  nuevoEstudianteId = 0;
  nuevasObservaciones = '';
  filtroEstado = 'TODOS';
  readonly gradosMap = new Map<number, string>();

  nuevoAlumno: RegistrarEstudianteRequestDto = {
    tipoDocumento: 'DNI',
    numeroDocumento: '',
    nombres: '',
    apellidoPaterno: '',
    apellidoMaterno: '',
    fechaNacimiento: '2012-05-15',
    genero: 'M',
    codigoEstudianteSiagie: '',
    grupoSanguineo: 'O+',
    alergiasCondiciones: ''
  };

  anioActivoTexto(): string {
    const a = this.anios().find(x => x.id === this.anioLectivoId);
    return a ? String(a.anio) : '2026';
  }

  obtenerNombreSeccion(s: SeccionResponseDto): string {
    const nombreGrado = this.gradosMap.get(s.gradoId) ?? `Grado ${s.gradoId}`;
    const aula = s.aulaFisica ? ` · ${s.aulaFisica}` : '';
    const vacantes = ` · ${s.vacantesOcupadas ?? 0}/${s.cupoMaximo ?? 30}`;
    return `${nombreGrado} - Sección ${s.letra}${aula}${vacantes}`;
  }

  estudiantesPaginados(): EstudianteResponseDto[] {
    const q = this.busquedaEstudiante.toLowerCase().trim();
    if (!q) return this.listaEstudiantes();

    return this.listaEstudiantes().filter(e =>
      e.numeroDocumento?.toLowerCase().includes(q) ||
      e.nombres?.toLowerCase().includes(q) ||
      e.apellidoPaterno?.toLowerCase().includes(q) ||
      e.apellidoMaterno?.toLowerCase().includes(q)
    );
  }

  estudiantesFiltrados(): EstudianteMatriculadoItem[] {
    const q = this.busqueda.toLowerCase().trim();
    const estado = this.filtroEstado;

    return this.estudiantes().filter(e => {
      const matchQ =
        !q ||
        String(e.estudianteId).includes(q) ||
        (e.codigoMatricula && e.codigoMatricula.toLowerCase().includes(q));
      const matchEstado = estado === 'TODOS' || e.estado === estado;
      return matchQ && matchEstado;
    });
  }

  ngOnInit(): void {
    this.cargarEstudiantes();
    this.cargarCatalogoGrados();
    this.academicoService.listarAnios().subscribe({
      next: anios => {
        this.anios.set(anios);
        this.anioLectivoId = anios.find(a => a.abierto)?.id ?? anios[0]?.id ?? 0;
        this.cargarSecciones();
      },
      error: () => this.error.set('No se pudieron cargar los años lectivos.')
    });
  }

  cargarCatalogoGrados(): void {
    [1, 2, 3].forEach(nivelId => {
      this.academicoService.listarGrados(nivelId).subscribe({
        next: grados => {
          grados.forEach(g => this.gradosMap.set(g.id, g.nombre));
        }
      });
    });
  }

  cargarEstudiantes(): void {
    this.cargandoEstudiantes.set(true);
    this.matriculaService.listarEstudiantes().subscribe({
      next: (estudiantes) => {
        this.cargandoEstudiantes.set(false);
        this.listaEstudiantes.set(estudiantes || []);
      },
      error: () => {
        this.cargandoEstudiantes.set(false);
        this.listaEstudiantes.set([]);
      }
    });
  }

  cargarSecciones(): void {
    if (!this.anioLectivoId) return;
    this.academicoService.listarSecciones(this.anioLectivoId).subscribe({
      next: secciones => {
        this.secciones.set(secciones);
        const conAlumnos = secciones.find(s => (s.vacantesOcupadas ?? 0) > 0);
        this.seccionIdSeleccionada = conAlumnos ? conAlumnos.id : (secciones[0]?.id ?? 0);
        this.cargarMatriculas();
      },
      error: () => this.error.set('No se pudieron cargar las secciones.')
    });
  }

  cargarMatriculas(): void {
    if (!this.seccionIdSeleccionada) {
      this.estudiantes.set([]);
      return;
    }
    this.cargando.set(true);
    this.matriculaService.listarPorSeccion(this.seccionIdSeleccionada, 0, 50).subscribe({
      next: (res) => {
        this.cargando.set(false);
        if (Array.isArray(res)) {
          this.estudiantes.set(
            res.map(m => ({
              id: m.id,
              codigoMatricula: `MAT-${this.anios().find(a => a.id === m.anioLectivoId)?.anio ?? m.anioLectivoId}-${String(m.id).padStart(3, '0')}`,
              estudianteId: m.estudianteId,
              estado: m.estadoMatricula,
              fechaMatricula: m.fechaMatricula ? String(m.fechaMatricula).split('T')[0] : '-',
              observaciones: m.observaciones
            }))
          );
        } else {
          this.estudiantes.set([]);
        }
      },
      error: () => {
        this.cargando.set(false);
        this.estudiantes.set([]);
      }
    });
  }

  confirmarMatricula(matriculaId: number): void {
    this.confirmandoMatricula.set(matriculaId);
    this.error.set(null);
    this.mensajeExito.set(null);
    this.matriculaService.confirmarMatricula(matriculaId).subscribe({
      next: matricula => {
        this.confirmandoMatricula.set(null);
        this.mensajeExito.set(`Matrícula #${matricula.id} confirmada; el pago fue validado.`);
        this.cargarMatriculas();
      },
      error: err => {
        this.confirmandoMatricula.set(null);
        this.error.set(err?.error?.message ?? 'No se pudo confirmar. Verifica el pago de la cuota de matrícula.');
      }
    });
  }

  abrirModalNuevoAlumno(): void {
    this.error.set(null);
    this.mensajeExito.set(null);
    this.nuevoAlumno = {
      tipoDocumento: 'DNI',
      numeroDocumento: '',
      nombres: '',
      apellidoPaterno: '',
      apellidoMaterno: '',
      fechaNacimiento: '2012-05-15',
      genero: 'M',
      codigoEstudianteSiagie: '',
      grupoSanguineo: 'O+',
      alergiasCondiciones: ''
    };
    this.mostrarModalNuevoAlumno.set(true);
    this.alumnoDialogRef = this.dialog.open(this.nuevoAlumnoDialog, {
      width: '900px',
      maxWidth: 'calc(100vw - 24px)',
      panelClass: 'school-dialog',
      autoFocus: 'dialog',
      restoreFocus: true
    });
    this.alumnoDialogRef.afterClosed().subscribe(() => this.mostrarModalNuevoAlumno.set(false));
  }

  cerrarModalNuevoAlumno(): void {
    this.mostrarModalNuevoAlumno.set(false);
    this.alumnoDialogRef?.close();
  }

  consultarDni(): void {
    const dni = this.nuevoAlumno.numeroDocumento.trim();
    if (this.nuevoAlumno.tipoDocumento !== 'DNI' || !/^[0-9]{8}$/.test(dni)) {
      this.error.set('Ingrese un DNI de ocho dígitos para consultar.');
      return;
    }
    this.error.set(null);
    this.consultandoDni.set(true);
    this.matriculaService.consultarDni(dni).subscribe({
      next: identidad => {
        this.consultandoDni.set(false);
        if (this.nuevoAlumno.numeroDocumento.trim() !== dni) return;
        this.nuevoAlumno.nombres = identidad.nombres;
        this.nuevoAlumno.apellidoPaterno = identidad.apellidoPaterno;
        this.nuevoAlumno.apellidoMaterno = identidad.apellidoMaterno;
      },
      error: () => {
        this.consultandoDni.set(false);
        this.error.set('No se pudo consultar el DNI. Puede completar los datos manualmente.');
      }
    });
  }

  guardarEstudiante(): void {
    if (!this.nuevoAlumno.numeroDocumento || !this.nuevoAlumno.nombres || !this.nuevoAlumno.apellidoPaterno || !this.nuevoAlumno.apellidoMaterno || !this.nuevoAlumno.fechaNacimiento) {
      this.error.set('Por favor complete todos los campos obligatorios del estudiante.');
      return;
    }

    this.guardandoAlumno.set(true);
    this.error.set(null);

    this.matriculaService.registrarEstudiante(this.nuevoAlumno).subscribe({
      next: (estudianteCreado) => {
        this.guardandoAlumno.set(false);
        this.cerrarModalNuevoAlumno();
        this.mensajeExito.set(
          `¡Estudiante ${estudianteCreado.nombres} ${estudianteCreado.apellidoPaterno} registrado exitosamente con ID #${estudianteCreado.id}!`
        );
        this.pestanaActiva.set('estudiantes');
        this.cargarEstudiantes();
      },
      error: (err) => {
        this.guardandoAlumno.set(false);
        const backendMsg = err?.error?.message ?? err?.error?.error ?? 'Error al registrar el estudiante en la base de datos.';
        this.error.set(backendMsg);
      }
    });
  }

  prepararMatriculaParaEstudiante(est: EstudianteResponseDto): void {
    this.nuevoEstudianteId = est.id;
    this.abrirDialogoSolicitud();
  }

  abrirModalNuevaMatricula(): void {
    this.abrirDialogoSolicitud();
  }

  private abrirDialogoSolicitud(): void {
    this.mostrarSolicitud.set(true);
    this.solicitudDialogRef = this.dialog.open(this.solicitudDialog, {
      width: '600px',
      maxWidth: 'calc(100vw - 24px)',
      panelClass: 'school-dialog',
      autoFocus: 'dialog',
      restoreFocus: true
    });
    this.solicitudDialogRef.afterClosed().subscribe(() => this.mostrarSolicitud.set(false));
  }

  cerrarSolicitudDialog(): void {
    this.mostrarSolicitud.set(false);
    this.solicitudDialogRef?.close();
  }

  solicitar(): void {
    if (!this.anioLectivoId || !this.seccionIdSeleccionada || !this.nuevoEstudianteId) {
      this.error.set('Complete año, sección y seleccione un estudiante registrado.');
      return;
    }
    this.guardando.set(true);
    this.error.set(null);
    this.matriculaService.solicitarMatricula({
      anioLectivoId: this.anioLectivoId,
      seccionId: this.seccionIdSeleccionada,
      estudianteId: this.nuevoEstudianteId,
      observaciones: this.nuevasObservaciones
    }).subscribe({
      next: (m) => {
        this.guardando.set(false);
        this.cerrarSolicitudDialog();
        this.mensajeExito.set(`¡Matrícula #${m.id} solicitada exitosamente para el estudiante #${this.nuevoEstudianteId}!`);
        this.nuevoEstudianteId = 0;
        this.nuevasObservaciones = '';
        this.pestanaActiva.set('matriculas');
        this.cargarMatriculas();
      },
      error: err => {
        this.guardando.set(false);
        this.error.set(err?.error?.message ?? 'No se pudo solicitar la matrícula.');
      }
    });
  }

  verFicha(matriculaId: number): void {
    this.matriculaService.consultarFicha(matriculaId).subscribe({
      next: (ficha) => {
        this.ficha.set(ficha);
        this.fichaDialogRef = this.dialog.open(this.fichaDialog, {
          width: '620px',
          maxWidth: 'calc(100vw - 24px)',
          panelClass: 'school-dialog',
          autoFocus: 'dialog',
          restoreFocus: true
        });
        this.fichaDialogRef.afterClosed().subscribe(() => this.ficha.set(null));
      },
      error: () => {
        this.error.set('No se pudo recuperar la ficha de la matrícula ID: ' + matriculaId);
      }
    });
  }

  cerrarFichaDialog(): void {
    this.ficha.set(null);
    this.fichaDialogRef?.close();
  }
}
