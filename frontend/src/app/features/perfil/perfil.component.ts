import { Component, ElementRef, inject, signal, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthStore } from '../../core/state/auth.store';
import { UsuarioService } from '../../core/services/usuario.service';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

@Component({
  selector: 'app-perfil',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  template: `
    <div class="max-w-4xl space-y-8">
      <!-- Header -->
      <div>
        <h2 class="text-2xl sm:text-3xl font-extrabold tracking-tight text-white">Mi Perfil Institucional</h2>
        <p class="text-sm text-slate-400 mt-1">
          Información de la cuenta de usuario, credenciales de acceso y roles autorizados.
        </p>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        <!-- User Info Card -->
        <div class="md:col-span-1 p-6 rounded-2xl bg-slate-900 border border-slate-800 space-y-5 text-center">
          <div class="w-20 h-20 mx-auto rounded-3xl bg-gradient-to-tr from-blue-700 to-indigo-500 p-0.5 shadow-xl flex items-center justify-center">
            <div class="w-full h-full bg-slate-900 rounded-[22px] flex items-center justify-center font-black text-2xl text-blue-400">
              {{ (store.user()?.username || 'U').substring(0, 2) | uppercase }}
            </div>
          </div>

          <div>
            <h3 class="text-lg font-bold text-white">{{ store.user()?.username }}</h3>
            <p class="text-xs text-slate-400 font-mono">{{ store.user()?.email }}</p>
          </div>

          <div class="pt-4 border-t border-slate-800 text-left space-y-2">
            <span class="text-[10px] font-bold uppercase text-slate-500 tracking-wider">Roles Asignados</span>
            <div class="flex flex-wrap gap-1.5">
              @for (rol of store.roles(); track rol) {
                <span class="px-2.5 py-1 rounded-lg bg-blue-950 text-blue-400 border border-blue-800 text-xs font-bold">
                  {{ rol }}
                </span>
              }
            </div>
          </div>
        </div>

        <!-- Security / Password Change -->
        <div class="md:col-span-2 p-6 sm:p-8 rounded-2xl bg-slate-900 border border-slate-800 space-y-6">
          <div>
            <h3 class="text-base font-bold text-white">Seguridad de la Cuenta</h3>
            <p class="text-xs text-slate-400 mt-0.5">
              Actualice periódicamente su contraseña para preservar la integridad de los datos CNEB.
            </p>
          </div>

          @if (mensajeExito()) {
            <div class="p-3.5 rounded-xl bg-emerald-950/60 border border-emerald-800 text-emerald-300 text-xs">
              ✓ Contraseña actualizada exitosamente.
            </div>
          }

          @if (mensajeError()) {
            <div class="p-3.5 rounded-xl bg-red-950/60 border border-red-800 text-red-300 text-xs">
              ✕ {{ mensajeError() }}
            </div>
          }

          <button mat-flat-button color="primary" type="button" (click)="abrirCambioPassword()" class="!rounded-xl !px-5 !text-xs !font-semibold">Cambiar contraseña</button>
        </div>
      </div>

      <dialog #passwordDialog class="school-native-dialog" aria-labelledby="password-dialog-title">
        <div class="school-dialog-shell"><header class="school-dialog-header"><div><h3 id="password-dialog-title">Cambiar contraseña</h3><p>Usa una clave nueva y confirma que ambas coincidan.</p></div><button type="button" class="school-dialog-cancel !min-h-9 !px-3" (click)="cerrarCambioPassword()" aria-label="Cerrar">✕</button></header>
          <form (ngSubmit)="cambiarPassword()" class="school-dialog-form"><div class="school-dialog-body space-y-4">
            @if (mensajeError()) { <p class="school-dialog-error" role="alert">{{ mensajeError() }}</p> }
            <label class="school-dialog-field">Contraseña actual<input type="password" [(ngModel)]="passwordActual" name="passwordActual" required autocomplete="current-password"></label>
            <label class="school-dialog-field">Nueva contraseña<input type="password" [(ngModel)]="passwordNuevo" name="passwordNuevo" minlength="6" required autocomplete="new-password" placeholder="Mínimo 6 caracteres"></label>
            <label class="school-dialog-field">Confirmar nueva contraseña<input type="password" [(ngModel)]="confirmarPassword" name="confirmarPassword" minlength="6" required autocomplete="new-password"></label>
          </div><footer class="school-dialog-footer"><button type="button" class="school-dialog-cancel" (click)="cerrarCambioPassword()">Cancelar</button><button type="submit" [disabled]="guardando() || !passwordActual || passwordNuevo.length < 6 || passwordNuevo !== confirmarPassword" class="school-dialog-submit">{{ guardando() ? 'Actualizando…' : 'Actualizar contraseña' }}</button></footer></form>
        </div>
      </dialog>
    </div>
  `
})
export class PerfilComponent {
  readonly store = inject(AuthStore);
  private readonly usuarioService = inject(UsuarioService);
  @ViewChild('passwordDialog') private passwordDialog!: ElementRef<HTMLDialogElement>;

  passwordActual = '';
  passwordNuevo = '';
  confirmarPassword = '';
  mensajeExito = signal(false);
  mensajeError = signal<string | null>(null);
  guardando = signal(false);

  abrirCambioPassword(): void {
    this.mensajeError.set(null);
    this.passwordActual = '';
    this.passwordNuevo = '';
    this.confirmarPassword = '';
    this.passwordDialog.nativeElement.showModal();
  }

  cerrarCambioPassword(): void { this.passwordDialog.nativeElement.close(); }

  cambiarPassword(): void {
    if (this.passwordNuevo !== this.confirmarPassword) {
      this.mensajeError.set('Las contraseñas no coinciden.');
      return;
    }

    this.mensajeError.set(null);
    this.guardando.set(true);
    this.usuarioService.cambiarPassword({
      passwordActual: this.passwordActual,
      passwordNuevo: this.passwordNuevo
    }).subscribe({
      next: () => {
        this.guardando.set(false);
        this.cerrarCambioPassword();
        this.mensajeExito.set(true);
        this.passwordActual = '';
        this.passwordNuevo = '';
        this.confirmarPassword = '';
        setTimeout(() => this.mensajeExito.set(false), 4000);
      },
      error: (err) => {
        this.guardando.set(false);
        this.mensajeError.set(err?.error?.message || 'Error al actualizar contraseña en la base de datos.');
      }
    });
  }
}
