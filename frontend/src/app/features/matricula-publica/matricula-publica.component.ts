import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { MatriculaPublicaService } from '../../core/services/matricula-publica.service';
import {
  CatalogoMatriculaPublicaDto,
  CrearSolicitudMatriculaPublicaDto,
  DocumentoSolicitudDto,
  EstadoSolicitudMatriculaPublica,
  PagoMatriculaPublicaDto,
  SolicitudMatriculaPublicaDto,
  TipoDocumentoSolicitud
} from '../../core/models/matricula-publica.model';

type ModalEtapa = 'documentos' | 'validacion' | 'pago' | 'exito' | null;

@Component({
  selector: 'app-matricula-publica',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './matricula-publica.component.html',
  styleUrl: './matricula-publica.component.css'
})
export class MatriculaPublicaComponent implements OnInit, OnDestroy {
  private readonly api = inject(MatriculaPublicaService);
  private pollHandle: number | null = null;

  readonly catalogo = signal<CatalogoMatriculaPublicaDto>({ anios: [], secciones: [] });
  readonly solicitud = signal<SolicitudMatriculaPublicaDto | null>(null);
  readonly modal = signal<ModalEtapa>(null);
  readonly cargandoCatalogo = signal(true);
  readonly procesando = signal(false);
  readonly error = signal('');
  readonly estadoPago = signal('');
  readonly enlacePago = signal('');
  readonly montoPago = signal(1);
  readonly anioId = signal<number | null>(null);
  readonly seccionId = signal<number | null>(null);

  nombresEstudiante = '';
  apellidoPaternoEstudiante = '';
  apellidoMaternoEstudiante = '';
  documentoEstudiante = '';
  nacimientoEstudiante = '';
  generoEstudiante: 'M' | 'F' = 'M';
  nombresApoderado = '';
  apellidoPaternoApoderado = '';
  apellidoMaternoApoderado = '';
  documentoApoderado = '';
  celularApoderado = '';
  emailApoderado = '';
  direccionApoderado = '';
  ubigeoApoderado = '';
  parentesco: CrearSolicitudMatriculaPublicaDto['parentesco'] = 'MADRE';
  consentimientoGemini = false;

  archivos: Record<TipoDocumentoSolicitud, File | null> = {
    PARTIDA_NACIMIENTO: null,
    DNI_C4: null,
    RECIBO_SERVICIO: null
  };

  readonly documentTypes: TipoDocumentoSolicitud[] = [
    'PARTIDA_NACIMIENTO', 'DNI_C4', 'RECIBO_SERVICIO'
  ];

  get seccionesDisponibles() {
    const year = this.anioId();
    return this.catalogo().secciones.filter(s => s.anioLectivoId === year);
  }

  ngOnInit(): void {
    this.api.catalogo().subscribe({
      next: data => {
        this.catalogo.set(data);
        const activo = data.anios.find(a => a.abierto);
        if (activo) this.anioId.set(activo.id);
        this.cargandoCatalogo.set(false);
        this.restaurarSolicitud();
      },
      error: () => {
        this.cargandoCatalogo.set(false);
        this.error.set('No se pudo cargar la oferta académica. Intenta nuevamente en unos minutos.');
      }
    });
  }

  ngOnDestroy(): void {
    this.detenerSeguimientoPago();
  }

  seleccionarArchivo(tipo: TipoDocumentoSolicitud, event: Event): void {
    const input = event.target as HTMLInputElement;
    const archivo = input.files?.[0] ?? null;
    if (!archivo) return;
    const permitidos = ['application/pdf', 'image/jpeg', 'image/png', 'image/webp'];
    if (archivo.size > 5 * 1024 * 1024 || !permitidos.includes(archivo.type)) {
      this.error.set('El archivo debe ser PDF, JPG, PNG o WebP y pesar hasta 5 MB.');
      input.value = '';
      return;
    }
    this.error.set('');
    this.archivos[tipo] = archivo;
  }

  abrirSolicitud(): void {
    this.error.set('');
    this.modal.set('documentos');
  }

  async validarYEnviarDocumentos(): Promise<void> {
    this.error.set('');
    const actual = this.solicitud();
    if (!actual && !this.validarFormulario()) return;
    if (!this.consentimientoGemini && !actual) {
      this.error.set('Debes autorizar la revisión automatizada de los documentos.');
      return;
    }
    this.procesando.set(true);
    this.modal.set('validacion');
    try {
      let solicitud = actual;
      if (!solicitud) {
        const payload: CrearSolicitudMatriculaPublicaDto = {
          anioLectivoId: this.anioId()!,
          seccionId: this.seccionId()!,
          numeroDocumentoEstudiante: this.documentoEstudiante.trim(),
          nombresEstudiante: this.nombresEstudiante.trim(),
          apellidoPaternoEstudiante: this.apellidoPaternoEstudiante.trim(),
          apellidoMaternoEstudiante: this.apellidoMaternoEstudiante.trim(),
          fechaNacimientoEstudiante: this.nacimientoEstudiante,
          generoEstudiante: this.generoEstudiante,
          numeroDocumentoApoderado: this.documentoApoderado.trim(),
          nombresApoderado: this.nombresApoderado.trim(),
          apellidoPaternoApoderado: this.apellidoPaternoApoderado.trim(),
          apellidoMaternoApoderado: this.apellidoMaternoApoderado.trim(),
          celularApoderado: this.celularApoderado.trim(),
          emailApoderado: this.emailApoderado.trim(),
          direccionApoderado: this.direccionApoderado.trim(),
          ubigeoApoderado: this.ubigeoApoderado.trim(),
          parentesco: this.parentesco,
          consentimientoGemini: true
        };
        solicitud = await firstValueFrom(this.api.crear(payload));
        this.guardarCredencial(solicitud);
        this.solicitud.set(solicitud);
      }

      const token = this.leerToken();
      if (!token) throw new Error('La sesión de solicitud expiró; vuelve a iniciar el trámite.');
      for (const tipo of this.documentTypes) {
        const estado = solicitud.documentos.find(d => d.tipo === tipo)?.estado;
        if (estado === 'VALIDADO') continue;
        const archivo = this.archivos[tipo];
        if (!archivo) continue;
        solicitud = await firstValueFrom(this.api.subirDocumento(solicitud.id, token, tipo, archivo));
        this.solicitud.set(solicitud);
      }
      const faltantes = this.documentTypes.filter(tipo =>
        solicitud!.documentos.find(d => d.tipo === tipo)?.estado !== 'VALIDADO'
      );
      if (faltantes.length > 0 && faltantes.some(tipo => !this.archivos[tipo])) {
        this.modal.set('documentos');
        this.error.set('Adjunta los documentos pendientes u observados para continuar.');
      } else {
        this.archivos = { PARTIDA_NACIMIENTO: null, DNI_C4: null, RECIBO_SERVICIO: null };
      }
    } catch (error: unknown) {
      this.modal.set('documentos');
      this.error.set(this.mensajeError(error, 'No se pudo validar la documentación.'));
    } finally {
      this.procesando.set(false);
    }
  }

  async continuarAlPago(): Promise<void> {
    const solicitud = this.solicitud();
    const token = this.leerToken();
    if (!solicitud || !token) return;
    this.error.set('');
    this.procesando.set(true);
    try {
      const pago: PagoMatriculaPublicaDto = await firstValueFrom(this.api.crearPago(solicitud.id, token));
      this.enlacePago.set(pago.enlacePago);
      this.montoPago.set(pago.monto);
      this.modal.set('pago');
      this.estadoPago.set('Completa el pago de prueba y luego vuelve aquí para consultar la confirmación.');
      this.iniciarSeguimientoPago();
    } catch (error: unknown) {
      this.error.set(this.mensajeError(error, 'No se pudo preparar el pago de prueba.'));
    } finally {
      this.procesando.set(false);
    }
  }

  async actualizarEstadoPago(): Promise<void> {
    const solicitud = this.solicitud();
    const token = this.leerToken();
    if (!solicitud || !token) return;
    this.estadoPago.set('Consultando confirmación segura del pago…');
    try {
      const actualizada = await firstValueFrom(this.api.consultar(solicitud.id, token));
      this.solicitud.set(actualizada);
      if (actualizada.estado === 'MATRICULADA') this.mostrarExito(actualizada);
      else this.estadoPago.set('Aún no recibimos confirmación del pago. Espera un momento y vuelve a consultar.');
    } catch (error: unknown) {
      this.estadoPago.set(this.mensajeError(error, 'No se pudo consultar el estado del pago.'));
    }
  }

  async reintentarDocumento(tipo: TipoDocumentoSolicitud, event: Event): Promise<void> {
    this.seleccionarArchivo(tipo, event);
  }

  cerrarModal(): void {
    if (this.modal() !== 'pago') this.modal.set(null);
  }

  etiquetaDocumento(tipo: TipoDocumentoSolicitud): string {
    switch (tipo) {
      case 'PARTIDA_NACIMIENTO': return 'Partida de nacimiento';
      case 'DNI_C4': return 'DNI o C4 del estudiante';
      case 'RECIBO_SERVICIO': return 'Recibo de luz o agua';
    }
  }

  estadoDocumento(tipo: TipoDocumentoSolicitud): DocumentoSolicitudDto | undefined {
    return this.solicitud()?.documentos.find(d => d.tipo === tipo);
  }

  documentosAprobados(): boolean {
    return this.solicitud()?.documentos.length === 3
      && this.solicitud()!.documentos.every(d => d.estado === 'VALIDADO');
  }

  estadoLabel(estado: EstadoSolicitudMatriculaPublica): string {
    const labels: Record<EstadoSolicitudMatriculaPublica, string> = {
      DOCUMENTOS_PENDIENTES: 'Documentos pendientes',
      DOCUMENTOS_OBSERVADOS: 'Requiere correcciones',
      DOCUMENTOS_VALIDADOS: 'Documentos validados',
      PAGO_PENDIENTE: 'Pago pendiente',
      MATRICULADA: 'Matrícula confirmada',
      RECHAZADA: 'Solicitud rechazada'
    };
    return labels[estado];
  }

  private validarFormulario(): boolean {
    if (!this.anioId() || !this.seccionId()) return this.error.set('Selecciona año lectivo y sección.'), false;
    const required = [this.documentoEstudiante, this.nombresEstudiante, this.apellidoPaternoEstudiante,
      this.apellidoMaternoEstudiante, this.nacimientoEstudiante, this.documentoApoderado,
      this.nombresApoderado, this.apellidoPaternoApoderado, this.apellidoMaternoApoderado,
      this.celularApoderado, this.direccionApoderado, this.ubigeoApoderado];
    if (required.some(value => !value.trim())) return this.error.set('Completa todos los campos obligatorios.'), false;
    if (!/^\d{8}$/.test(this.documentoEstudiante) || !/^\d{8}$/.test(this.documentoApoderado)) {
      return this.error.set('Los DNI deben tener 8 dígitos.'), false;
    }
    if (!/^9\d{8}$/.test(this.celularApoderado) || !/^\d{6}$/.test(this.ubigeoApoderado)) {
      return this.error.set('Verifica el celular (9 dígitos) y el ubigeo (6 dígitos).'), false;
    }
    if (!this.consentimientoGemini) return this.error.set('Autoriza el procesamiento de documentos para continuar.'), false;
    if (this.documentTypes.some(type => !this.archivos[type])) return this.error.set('Adjunta los tres documentos requeridos.'), false;
    return true;
  }

  private restaurarSolicitud(): void {
    const id = sessionStorage.getItem('shuji-solicitud-matricula-id');
    const token = sessionStorage.getItem('shuji-solicitud-matricula-token');
    if (!id || !token) return;
    this.api.consultar(id, token).subscribe({
      next: solicitud => {
        this.solicitud.set(solicitud);
        if (solicitud.estado === 'MATRICULADA') this.mostrarExito(solicitud);
        else if (solicitud.estado === 'PAGO_PENDIENTE' && solicitud.enlacePago) {
          this.enlacePago.set(solicitud.enlacePago);
          this.modal.set('pago');
          this.iniciarSeguimientoPago();
        }
      },
      error: () => this.limpiarCredencial()
    });
  }

  private guardarCredencial(solicitud: SolicitudMatriculaPublicaDto): void {
    if (!solicitud.token) return;
    sessionStorage.setItem('shuji-solicitud-matricula-id', solicitud.id);
    sessionStorage.setItem('shuji-solicitud-matricula-token', solicitud.token);
  }

  private leerToken(): string | null {
    return sessionStorage.getItem('shuji-solicitud-matricula-token');
  }

  private limpiarCredencial(): void {
    sessionStorage.removeItem('shuji-solicitud-matricula-id');
    sessionStorage.removeItem('shuji-solicitud-matricula-token');
  }

  private iniciarSeguimientoPago(): void {
    this.detenerSeguimientoPago();
    this.pollHandle = window.setInterval(() => void this.actualizarEstadoPago(), 8000);
  }

  private detenerSeguimientoPago(): void {
    if (this.pollHandle !== null) window.clearInterval(this.pollHandle);
    this.pollHandle = null;
  }

  private mostrarExito(solicitud: SolicitudMatriculaPublicaDto): void {
    this.detenerSeguimientoPago();
    this.solicitud.set(solicitud);
    this.modal.set('exito');
    this.limpiarCredencial();
  }

  private mensajeError(error: unknown, fallback: string): string {
    const apiError = error as { error?: { message?: string }; message?: string };
    return apiError?.error?.message || apiError?.message || fallback;
  }
}
