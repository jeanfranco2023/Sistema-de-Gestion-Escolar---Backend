import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize, firstValueFrom } from 'rxjs';
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
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private pollHandle: number | null = null;

  readonly catalogo = signal<CatalogoMatriculaPublicaDto>({ anios: [], secciones: [] });
  readonly solicitud = signal<SolicitudMatriculaPublicaDto | null>(null);
  readonly modal = signal<ModalEtapa>(null);
  readonly cargandoCatalogo = signal(true);
  readonly procesando = signal(false);
  readonly consultandoDniEstudiante = signal(false);
  readonly consultandoDniApoderado = signal(false);
  readonly error = signal('');
  readonly estadoPago = signal('');
  readonly enlacePago = signal('');
  readonly montoPago = signal(1);
  readonly anioId = signal<number | null>(null);
  readonly seccionId = signal<number | null>(null);
  readonly mensajeDni = signal('');

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
    this.restaurarSolicitud();
    this.api.catalogo().subscribe({
      next: data => {
        this.catalogo.set(data);
        const activo = data.anios.find(a => a.abierto);
        if (activo) this.anioId.set(activo.id);
        this.cargandoCatalogo.set(false);
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

  consultarDni(tipo: 'estudiante' | 'apoderado'): void {
    const dni = tipo === 'estudiante' ? this.documentoEstudiante : this.documentoApoderado;
    if (!/^\d{8}$/.test(dni)) {
      this.mensajeDni.set('Ingresa un DNI de 8 dígitos.');
      return;
    }
    const indicador = tipo === 'estudiante' ? this.consultandoDniEstudiante : this.consultandoDniApoderado;
    indicador.set(true);
    this.mensajeDni.set('');
    this.api.consultarDni(dni).pipe(finalize(() => indicador.set(false))).subscribe({
      next: identidad => {
        if (tipo === 'estudiante') {
          this.nombresEstudiante = identidad.nombres;
          this.apellidoPaternoEstudiante = identidad.apellidoPaterno;
          this.apellidoMaternoEstudiante = identidad.apellidoMaterno;
        } else {
          this.nombresApoderado = identidad.nombres;
          this.apellidoPaternoApoderado = identidad.apellidoPaterno;
          this.apellidoMaternoApoderado = identidad.apellidoMaterno;
        }
        this.mensajeDni.set('Nombres y apellidos consultados. Contrástalos con el documento adjunto.');
      },
      error: error => this.mensajeDni.set(this.mensajeError(error, 'No fue posible consultar este DNI; verifica el número o inténtalo luego.'))
    });
  }

  abrirSolicitud(): void {
    this.error.set('');
    const actual = this.solicitud();
    if (actual?.estado === 'MATRICULADA' || actual?.estado === 'RECHAZADA') {
      this.iniciarNuevaSolicitud();
      return;
    }

    const id = sessionStorage.getItem('shuji-solicitud-matricula-id');
    const token = this.leerToken();
    if (!id || !token) {
      this.modal.set(actual?.estado === 'DOCUMENTOS_VALIDADOS' ? 'validacion' : 'documentos');
      return;
    }

    this.procesando.set(true);
    this.api.consultar(id, token).pipe(finalize(() => this.procesando.set(false))).subscribe({
      next: solicitud => {
        this.solicitud.set(solicitud);
        this.abrirEtapaActualizada(solicitud);
      },
      error: error => {
        if (error?.status === 404) {
          this.limpiarCredencial();
          this.solicitud.set(null);
          this.modal.set('documentos');
          return;
        }
        this.error.set(this.mensajeError(error, 'No se pudo actualizar el estado del trámite.'));
      }
    });
  }

  iniciarNuevaSolicitud(): void {
    this.detenerSeguimientoPago();
    this.limpiarCredencial();
    this.solicitud.set(null);
    this.archivos = { PARTIDA_NACIMIENTO: null, DNI_C4: null, RECIBO_SERVICIO: null };
    this.nombresEstudiante = '';
    this.apellidoPaternoEstudiante = '';
    this.apellidoMaternoEstudiante = '';
    this.documentoEstudiante = '';
    this.nacimientoEstudiante = '';
    this.generoEstudiante = 'M';
    this.nombresApoderado = '';
    this.apellidoPaternoApoderado = '';
    this.apellidoMaternoApoderado = '';
    this.documentoApoderado = '';
    this.celularApoderado = '';
    this.emailApoderado = '';
    this.direccionApoderado = '';
    this.ubigeoApoderado = '';
    this.parentesco = 'MADRE';
    this.consentimientoGemini = false;
    this.seccionId.set(null);
    this.enlacePago.set('');
    this.estadoPago.set('');
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
          emailApoderado: this.emailApoderado.trim() || null,
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

  descargarComprobante(): void {
    this.descargarDocumento('comprobante');
  }

  descargarFicha(): void {
    this.descargarDocumento('ficha');
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
    if (this.emailApoderado.trim() && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.emailApoderado.trim())) {
      return this.error.set('Ingresa un correo electrónico válido o deja el campo vacío.'), false;
    }
    if (!this.consentimientoGemini) return this.error.set('Autoriza el procesamiento de documentos para continuar.'), false;
    if (this.documentTypes.some(type => !this.archivos[type])) return this.error.set('Adjunta los tres documentos requeridos.'), false;
    return true;
  }

  private restaurarSolicitud(): void {
    const id = sessionStorage.getItem('shuji-solicitud-matricula-id');
    const token = sessionStorage.getItem('shuji-solicitud-matricula-token');
    const paymentId = this.paymentIdFromReturn();
    if (!id || !token) {
      if (paymentId) {
        this.limpiarParametrosPago();
        this.error.set('No se encontró la sesión privada de la solicitud para verificar el retorno del pago.');
      }
      return;
    }
    this.api.consultar(id, token).subscribe({
      next: solicitud => {
        this.solicitud.set(solicitud);
        if (paymentId) {
          if (solicitud.estado === 'MATRICULADA') {
            this.limpiarParametrosPago();
            this.mostrarExito(solicitud);
          } else {
            this.procesarRetornoPago(id, token, paymentId);
          }
          return;
        }
        if (solicitud.estado === 'MATRICULADA') this.mostrarExito(solicitud);
        else if (solicitud.estado === 'PAGO_PENDIENTE' && solicitud.enlacePago) {
          this.enlacePago.set(solicitud.enlacePago);
          this.montoPago.set(solicitud.montoPago);
          this.modal.set('pago');
          this.iniciarSeguimientoPago();
        }
      },
      error: () => {
        this.limpiarCredencial();
        if (paymentId) {
          this.limpiarParametrosPago();
          this.error.set('No se pudo recuperar la solicitud para verificar el pago.');
        }
      }
    });
  }

  private abrirEtapaActualizada(solicitud: SolicitudMatriculaPublicaDto): void {
    switch (solicitud.estado) {
      case 'MATRICULADA':
        this.mostrarExito(solicitud);
        return;
      case 'RECHAZADA':
        this.iniciarNuevaSolicitud();
        return;
      case 'PAGO_PENDIENTE':
        this.enlacePago.set(solicitud.enlacePago ?? '');
        this.montoPago.set(solicitud.montoPago);
        this.modal.set('pago');
        this.estadoPago.set('Completa el pago de prueba o consulta su confirmación.');
        this.iniciarSeguimientoPago();
        return;
      case 'DOCUMENTOS_VALIDADOS':
        this.modal.set('validacion');
        return;
      default:
        this.modal.set('documentos');
    }
  }

  private paymentIdFromReturn(): string | null {
    const paymentId = this.route.snapshot.queryParamMap.get('payment_id')
      ?? this.route.snapshot.queryParamMap.get('collection_id');
    return paymentId && paymentId.toLowerCase() !== 'null' ? paymentId : null;
  }

  private procesarRetornoPago(id: string, token: string, paymentId: string): void {
    this.limpiarParametrosPago();
    this.modal.set('pago');
    if (!/^\d{1,32}$/.test(paymentId)) {
      this.estadoPago.set('Mercado Pago devolvió un identificador de pago inválido.');
      return;
    }

    this.procesando.set(true);
    this.estadoPago.set('Verificando el pago directamente con Mercado Pago…');
    this.api.confirmarPago(id, token, paymentId)
      .pipe(finalize(() => this.procesando.set(false)))
      .subscribe({
        next: solicitud => {
          this.solicitud.set(solicitud);
          if (solicitud.estado === 'MATRICULADA') {
            this.mostrarExito(solicitud);
            return;
          }
          if (solicitud.enlacePago) this.enlacePago.set(solicitud.enlacePago);
          this.estadoPago.set('El pago aún no figura aprobado. Seguiremos consultando la confirmación segura.');
          this.iniciarSeguimientoPago();
        },
        error: error => {
          this.estadoPago.set(this.mensajeError(error, 'No se pudo verificar el pago con Mercado Pago.'));
          this.iniciarSeguimientoPago();
        }
      });
  }

  private limpiarParametrosPago(): void {
    const parametrosMercadoPago = [
      'payment_id', 'collection_id', 'status', 'collection_status',
      'external_reference', 'preference_id', 'merchant_order_id'
    ];
    const queryParams = Object.fromEntries(parametrosMercadoPago.map(parametro => [parametro, null]));
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams,
      queryParamsHandling: 'merge',
      replaceUrl: true
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
  }

  private descargarDocumento(tipo: 'comprobante' | 'ficha'): void {
    const id = sessionStorage.getItem('shuji-solicitud-matricula-id');
    const token = this.leerToken();
    if (!id || !token) {
      this.error.set('No se encontró el acceso privado para descargar los documentos.');
      return;
    }
    const solicitud = this.solicitud();
    if (!solicitud || solicitud.estado !== 'MATRICULADA') return;

    this.error.set('');
    this.procesando.set(true);
    const descarga = tipo === 'comprobante'
      ? this.api.descargarComprobante(id, token)
      : this.api.descargarFicha(id, token);
    descarga.pipe(finalize(() => this.procesando.set(false))).subscribe({
      next: archivo => {
        const url = URL.createObjectURL(archivo);
        const enlace = document.createElement('a');
        enlace.href = url;
        enlace.download = `${tipo}-matricula-${solicitud.matriculaId}.html`;
        document.body.appendChild(enlace);
        enlace.click();
        enlace.remove();
        window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
      },
      error: error => this.error.set(this.mensajeError(error, 'No se pudo generar el documento.'))
    });
  }

  private mensajeError(error: unknown, fallback: string): string {
    const apiError = error as { error?: { message?: string }; message?: string };
    return apiError?.error?.message || apiError?.message || fallback;
  }
}
