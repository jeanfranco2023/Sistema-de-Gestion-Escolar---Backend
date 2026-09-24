import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatriculaPublicaService } from '../../core/services/matricula-publica.service';
import {
  DocumentoSolicitudDto,
  SolicitudMatriculaPublicaAdminDto,
  TipoDocumentoSolicitud
} from '../../core/models/matricula-publica.model';

@Component({
  selector: 'app-solicitudes-matricula',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './solicitudes-matricula.component.html',
  styleUrl: './solicitudes-matricula.component.css'
})
export class SolicitudesMatriculaComponent implements OnInit {
  private readonly api = inject(MatriculaPublicaService);
  readonly solicitudes = signal<SolicitudMatriculaPublicaAdminDto[]>([]);
  readonly cargando = signal(false);
  readonly error = signal('');
  readonly aviso = signal('');
  readonly seleccion = signal<SolicitudMatriculaPublicaAdminDto | null>(null);
  readonly documento = signal<TipoDocumentoSolicitud>('PARTIDA_NACIMIENTO');
  readonly observacion = signal('');
  readonly confirmandoRechazo = signal(false);
  readonly guardando = signal(false);

  ngOnInit(): void { this.cargar(); }

  cargar(): void {
    this.cargando.set(true);
    this.api.listarAdmin().subscribe({
      next: values => { this.solicitudes.set(values); this.cargando.set(false); },
      error: err => { this.error.set(err?.error?.message || 'No se pudieron cargar las solicitudes.'); this.cargando.set(false); }
    });
  }

  abrirObservacion(solicitud: SolicitudMatriculaPublicaAdminDto): void {
    this.seleccion.set(solicitud);
    const doc = solicitud.documentos.find(d => d.estado !== 'VALIDADO');
    this.documento.set(doc?.tipo ?? 'PARTIDA_NACIMIENTO');
    this.observacion.set(doc?.observacion ?? '');
    this.confirmandoRechazo.set(false);
    this.error.set('');
  }

  abrirRechazo(solicitud: SolicitudMatriculaPublicaAdminDto): void {
    this.seleccion.set(solicitud);
    this.confirmandoRechazo.set(true);
    this.error.set('');
  }

  cerrarModal(): void { this.seleccion.set(null); this.confirmandoRechazo.set(false); }

  guardarObservacion(): void {
    const item = this.seleccion();
    if (!item || !this.observacion().trim()) { this.error.set('Escribe una observación para la familia.'); return; }
    this.guardando.set(true);
    this.api.observarAdmin(item.id, this.documento(), this.observacion().trim()).subscribe({
      next: updated => {
        this.solicitudes.update(current => current.map(s => s.id === updated.id ? updated : s));
        this.aviso.set('Se solicitó la corrección del documento. El apoderado podrá cargarlo nuevamente con su código privado.');
        this.guardando.set(false);
        this.cerrarModal();
      },
      error: err => { this.error.set(err?.error?.message || 'No se pudo registrar la observación.'); this.guardando.set(false); }
    });
  }

  rechazar(): void {
    const item = this.seleccion();
    if (!item) return;
    this.guardando.set(true);
    this.api.rechazarAdmin(item.id).subscribe({
      next: () => {
        this.solicitudes.update(current => current.map(s => s.id === item.id ? { ...s, estado: 'RECHAZADA' } : s));
        this.aviso.set('La solicitud se cerró y se liberó cualquier vacante reservada.');
        this.guardando.set(false);
        this.cerrarModal();
      },
      error: err => { this.error.set(err?.error?.message || 'No se pudo cerrar la solicitud.'); this.guardando.set(false); }
    });
  }

  etiquetaDocumento(tipo: TipoDocumentoSolicitud): string {
    return tipo === 'PARTIDA_NACIMIENTO' ? 'Partida de nacimiento'
      : tipo === 'DNI_C4' ? 'DNI o C4' : 'Recibo de luz o agua';
  }

  estadoLabel(estado: SolicitudMatriculaPublicaAdminDto['estado']): string {
    const labels: Record<SolicitudMatriculaPublicaAdminDto['estado'], string> = {
      DOCUMENTOS_PENDIENTES: 'Documentos pendientes', DOCUMENTOS_OBSERVADOS: 'Corrección solicitada',
      DOCUMENTOS_VALIDADOS: 'Documentos validados', PAGO_PENDIENTE: 'Pago pendiente',
      MATRICULADA: 'Matriculada', RECHAZADA: 'Rechazada'
    };
    return labels[estado];
  }

  documentoPendiente(solicitud: SolicitudMatriculaPublicaAdminDto): DocumentoSolicitudDto | undefined {
    return solicitud.documentos.find(d => d.estado === 'OBSERVADO')
      ?? solicitud.documentos.find(d => d.estado === 'PENDIENTE');
  }
}
