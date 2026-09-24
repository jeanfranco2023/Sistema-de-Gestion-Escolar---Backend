import { Component, inject, signal, OnInit, TemplateRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { UsuarioService } from '../../core/services/usuario.service';
import { UserResponseDto, RolNombre } from '../../core/models/auth.model';
import { AuthStore } from '../../core/state/auth.store';

@Component({
  selector: 'app-usuarios',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  template: `
    <div class="space-y-6">
      <!-- Header -->
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 class="text-2xl sm:text-3xl font-extrabold tracking-tight text-white">Usuarios & Permisos (RBAC)</h2>
          <p class="text-sm text-slate-400 mt-1">
            Administración de cuentas institucionales, asignación de roles y control de acceso sincronizado con PostgreSQL.
          </p>
        </div>
        <div class="flex items-center gap-3">
          @if (store.hasRole('DIRECCION')) { <button mat-flat-button color="primary"
            (click)="abrirModalCrear()"
            class="!h-11 !rounded-xl !px-4 !text-xs !font-semibold sm:!text-sm"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
            </svg>
            <span>Crear Usuario en BD</span>
          </button> }
        </div>
      </div>

      @if (error()) { <p class="school-dialog-error" role="alert">{{ error() }}</p> }

      <!-- Filters & Stats -->
      <div class="p-4 rounded-2xl bg-slate-900 border border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-4">
        <div class="flex w-full flex-col items-stretch gap-3 sm:w-auto sm:flex-row sm:items-center">
          <label class="text-xs font-semibold text-slate-400">Filtrar por Rol:</label>
          <select
            [(ngModel)]="filtroRol"
            (change)="cargarUsuarios()"
            class="px-3 py-1.5 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs focus:outline-none focus:border-blue-500"
          >
            <option value="">Todos los Roles</option>
            <option value="DIRECCION">Dirección</option>
            <option value="SECRETARIA">Secretaría</option>
            <option value="DOCENTE">Docente</option>
            <option value="AUXILIAR">Auxiliar</option>
            <option value="TUTOR">Tutor</option>
            <option value="APODERADO">Apoderado</option>
          </select>
          <button
            (click)="cargarUsuarios()"
            class="px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold cursor-pointer"
          >
            Actualizar BD
          </button>
        </div>

        <div class="text-xs text-slate-400">
          Total de cuentas en BD: <span class="text-white font-bold">{{ usuarios().length }}</span>
        </div>
      </div>

      <!-- Users Table -->
      <div class="rounded-2xl bg-slate-900 border border-slate-800 overflow-hidden shadow-sm">
        <div class="overflow-x-auto">
          <table class="w-full text-left text-xs">
            <thead class="bg-slate-950/70 border-b border-slate-800 text-slate-400 uppercase tracking-wider font-semibold">
              <tr>
                <th class="px-5 py-3.5">ID BD</th>
                <th class="px-5 py-3.5">Usuario</th>
                <th class="px-5 py-3.5">Correo Electrónico</th>
                <th class="px-5 py-3.5">Roles Asignados</th>
                <th class="px-5 py-3.5">Estado en BD</th>
                <th class="px-5 py-3.5">Fecha Creación</th>
                <th class="px-5 py-3.5 text-right">Acción</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-800/60 text-slate-300">
              @if (cargando()) {
                <tr>
                  <td colspan="7" class="text-center py-10 text-slate-400">
                    <span class="inline-block w-5 h-5 border-2 border-blue-500/30 border-t-blue-500 rounded-full animate-spin mb-2"></span>
                    <p>Consultando usuarios en la base de datos...</p>
                  </td>
                </tr>
              } @else {
                @for (user of usuarios(); track user.id) {
                  <tr class="hover:bg-slate-800/40 transition">
                    <td class="px-5 py-3.5 font-mono text-slate-500">{{ user.id }}</td>
                    <td class="px-5 py-3.5 font-bold text-white">{{ user.username }}</td>
                    <td class="px-5 py-3.5 text-slate-300">{{ user.email }}</td>
                    <td class="px-5 py-3.5">
                      <div class="flex flex-wrap gap-1">
                        @for (r of user.roles; track r.codigo) {
                          <span class="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-800 text-blue-400 border border-slate-700">
                            {{ r.codigo }}
                          </span>
                        }
                      </div>
                    </td>
                    <td class="px-5 py-3.5">
                      @if (user.activo) {
                        <span class="px-2.5 py-1 rounded-full text-[10px] font-bold bg-emerald-950 text-emerald-400 border border-emerald-800">
                          ACTIVO
                        </span>
                      } @else {
                        <span class="px-2.5 py-1 rounded-full text-[10px] font-bold bg-red-950 text-red-400 border border-red-800">
                          SUSPENDIDO
                        </span>
                      }
                    </td>
                    <td class="px-5 py-3.5 text-slate-400 font-mono">{{ user.createdAt || '-' }}</td>
                    <td class="px-5 py-3.5 text-right">
                      <button
                        (click)="alternarEstado(user)"
                        [class]="user.activo ? 'text-red-400 hover:text-red-300' : 'text-emerald-400 hover:text-emerald-300'"
                        class="px-2.5 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 transition font-semibold cursor-pointer text-xs"
                      >
                        {{ user.activo ? 'Suspender' : 'Activar' }}
                      </button>
                    </td>
                  </tr>
                } @empty {
                  <tr>
                    <td colspan="7" class="text-center py-12 text-slate-500 space-y-2">
                      <div class="text-2xl">👥</div>
                      <p class="font-medium text-slate-400">No se encontraron usuarios registrados en la base de datos.</p>
                      <p class="text-[11px] text-slate-500">Cree un nuevo usuario institucional con el botón superior para darlo de alta en PostgreSQL.</p>
                    </td>
                  </tr>
                }
              }
            </tbody>
          </table>
        </div>
      </div>

      <ng-template #crearUsuarioDialog>
        <h2 mat-dialog-title class="!px-6 !pt-6 !text-xl !font-semibold !tracking-tight !text-[#14213d]">Nuevo usuario institucional</h2>
        <p class="px-6 text-xs leading-5 text-slate-500">Configura las credenciales y el rol de acceso de la cuenta.</p>
        <form id="crear-usuario-form" (ngSubmit)="guardarNuevoUsuario()">
        <mat-dialog-content class="!px-6 !pt-5">
          @if (error()) { <p class="school-dialog-error mb-4" role="alert">{{ error() }}</p> }
          <div class="grid grid-cols-1 gap-x-4 gap-y-1 sm:grid-cols-2">
            <mat-form-field appearance="outline" subscriptSizing="dynamic" class="sm:col-span-2">
              <mat-label>Nombre de usuario</mat-label>
              <input matInput [(ngModel)]="nuevoUsername" name="nuevoUsername" placeholder="ej. tutor_secundaria" required autocomplete="username" />
            </mat-form-field>
            <mat-form-field appearance="outline" subscriptSizing="dynamic" class="sm:col-span-2">
              <mat-label>Correo institucional</mat-label>
              <input matInput type="email" [(ngModel)]="nuevoEmail" name="nuevoEmail" placeholder="tutor@shuji.edu.pe" required autocomplete="email" />
            </mat-form-field>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>Contraseña inicial</mat-label>
              <input matInput type="password" [(ngModel)]="nuevoPassword" name="nuevoPassword" placeholder="Mínimo 6 caracteres" minlength="6" required autocomplete="new-password" />
            </mat-form-field>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>Rol institucional</mat-label>
              <mat-select [(ngModel)]="nuevoRol" name="nuevoRol">
                <mat-option value="DOCENTE">Docente</mat-option>
                <mat-option value="SECRETARIA">Secretaría</mat-option>
                <mat-option value="AUXILIAR">Auxiliar</mat-option>
                <mat-option value="TUTOR">Tutor</mat-option>
                <mat-option value="DIRECCION">Dirección</mat-option>
              </mat-select>
            </mat-form-field>
          </div>
        </mat-dialog-content>
        <mat-dialog-actions align="end" class="!gap-2 !px-6 !pb-6">
          <button mat-button type="button" mat-dialog-close>Cancelar</button>
          <button mat-flat-button color="primary" type="submit" [disabled]="creando() || !nuevoUsername.trim() || !nuevoEmail.trim() || nuevoPassword.length < 6">{{ creando() ? 'Creando…' : 'Crear cuenta' }}</button>
        </mat-dialog-actions>
        </form>
      </ng-template>

      <ng-template #confirmarEstadoDialog>
        <h2 mat-dialog-title class="!px-6 !pt-6 !text-xl !font-semibold !tracking-tight !text-[#14213d]">{{ usuarioPendienteEstado()?.activo ? 'Suspender usuario' : 'Activar usuario' }}</h2>
        <mat-dialog-content class="!px-6 !pt-3">
          <p class="text-sm leading-6 text-slate-600">{{ usuarioPendienteEstado()?.activo ? 'La cuenta perderá el acceso al portal.' : 'La cuenta recuperará el acceso según sus roles asignados.' }}</p>
          @if (usuarioPendienteEstado()) { <p class="mt-3 break-all text-sm font-semibold text-slate-800">{{ usuarioPendienteEstado()!.email }}</p> }
          @if (error()) { <p class="school-dialog-error mt-4" role="alert">{{ error() }}</p> }
        </mat-dialog-content>
        <mat-dialog-actions align="end" class="!gap-2 !px-6 !pb-6">
          <button mat-button type="button" mat-dialog-close [disabled]="actualizandoEstado()">Cancelar</button>
          <button mat-flat-button color="primary" type="button" (click)="confirmarCambioEstado()" [disabled]="actualizandoEstado()">{{ actualizandoEstado() ? 'Actualizando…' : 'Confirmar' }}</button>
        </mat-dialog-actions>
      </ng-template>
    </div>
  `
})
export class UsuariosComponent implements OnInit {
  private readonly dialog = inject(MatDialog);
  private readonly usuarioService = inject(UsuarioService);
  readonly store = inject(AuthStore);

  @ViewChild('crearUsuarioDialog') private crearUsuarioDialog!: TemplateRef<unknown>;
  @ViewChild('confirmarEstadoDialog') private confirmarEstadoDialog!: TemplateRef<unknown>;
  private dialogRef?: MatDialogRef<unknown>;
  private estadoDialogRef?: MatDialogRef<unknown>;
  readonly error = signal<string | null>(null);
  readonly creando = signal(false);
  readonly actualizandoEstado = signal(false);
  readonly usuarioPendienteEstado = signal<UserResponseDto | null>(null);

  cargando = signal(false);
  readonly usuarios = signal<UserResponseDto[]>([]);
  filtroRol = '';
  nuevoUsername = '';
  nuevoEmail = '';
  nuevoPassword = '';
  nuevoRol: RolNombre = 'DOCENTE';

  ngOnInit(): void {
    this.cargarUsuarios();
  }

  cargarUsuarios(): void {
    this.cargando.set(true);
    this.usuarioService.listarUsuarios(this.filtroRol).subscribe({
      next: (data) => {
        this.cargando.set(false);
        this.usuarios.set(data || []);
      },
      error: () => {
        this.cargando.set(false);
        this.usuarios.set([]);
      }
    });
  }

  abrirModalCrear(): void {
    this.error.set(null);
    this.creando.set(false);
    this.nuevoUsername = '';
    this.nuevoEmail = '';
    this.nuevoPassword = '';
    this.nuevoRol = 'DOCENTE';
    this.dialogRef = this.dialog.open(this.crearUsuarioDialog, {
      width: '560px',
      maxWidth: 'calc(100vw - 28px)',
      panelClass: 'school-dialog',
      autoFocus: 'dialog',
      restoreFocus: true
    });
  }

  guardarNuevoUsuario(): void {
    if (!this.nuevoUsername.trim() || !this.nuevoEmail.trim() || this.nuevoPassword.length < 6 || this.creando()) return;
    this.error.set(null);
    this.creando.set(true);
    const payload = {
      username: this.nuevoUsername.trim(),
      email: this.nuevoEmail.trim(),
      password: this.nuevoPassword,
      roles: [this.nuevoRol]
    };

    this.usuarioService.crearUsuario(payload).subscribe({
      next: () => {
        this.creando.set(false);
        this.dialogRef?.close();
        this.cargarUsuarios();
      },
      error: (err) => {
        this.creando.set(false);
        this.error.set(err?.error?.message || 'No se pudo registrar el usuario. Verifique los datos y permisos.');
      }
    });
  }

  alternarEstado(user: UserResponseDto): void {
    this.error.set(null);
    this.usuarioPendienteEstado.set(user);
    this.actualizandoEstado.set(false);
    this.estadoDialogRef = this.dialog.open(this.confirmarEstadoDialog, {
      width: '480px',
      maxWidth: 'calc(100vw - 24px)',
      panelClass: 'school-dialog',
      autoFocus: 'dialog',
      restoreFocus: true
    });
    this.estadoDialogRef.afterClosed().subscribe(() => this.usuarioPendienteEstado.set(null));
  }

  confirmarCambioEstado(): void {
    const user = this.usuarioPendienteEstado();
    if (!user || this.actualizandoEstado()) return;
    const nuevoEstado = !user.activo;
    this.error.set(null);
    this.actualizandoEstado.set(true);
    this.usuarioService.cambiarEstado(user.id, nuevoEstado).subscribe({
      next: (actualizado) => {
        this.actualizandoEstado.set(false);
        user.activo = actualizado ? actualizado.activo : nuevoEstado;
        this.estadoDialogRef?.close();
      },
      error: (err) => {
        this.actualizandoEstado.set(false);
        this.error.set(err?.error?.message || 'No se pudo cambiar el estado del usuario.');
      }
    });
  }
}
