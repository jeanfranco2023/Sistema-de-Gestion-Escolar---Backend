export type TipoDocumentoSolicitud = 'PARTIDA_NACIMIENTO' | 'DNI_C4' | 'RECIBO_SERVICIO';
export type EstadoValidacionDocumento = 'PENDIENTE' | 'VALIDADO' | 'OBSERVADO';
export type EstadoSolicitudMatriculaPublica =
  | 'DOCUMENTOS_PENDIENTES'
  | 'DOCUMENTOS_OBSERVADOS'
  | 'DOCUMENTOS_VALIDADOS'
  | 'PAGO_PENDIENTE'
  | 'MATRICULADA'
  | 'RECHAZADA';

export interface DocumentoSolicitudDto {
  tipo: TipoDocumentoSolicitud;
  estado: EstadoValidacionDocumento;
  observacion: string | null;
}

export interface AnioLectivoPublicoDto {
  id: number;
  anio: number;
  fechaInicio: string;
  fechaFin: string;
  abierto: boolean;
}

export interface SeccionMatriculaPublicaDto {
  id: number;
  anioLectivoId: number;
  nivel: string;
  grado: string;
  letra: string;
  vacantesDisponibles: number;
}

export interface CatalogoMatriculaPublicaDto {
  anios: AnioLectivoPublicoDto[];
  secciones: SeccionMatriculaPublicaDto[];
}

export interface CrearSolicitudMatriculaPublicaDto {
  anioLectivoId: number;
  seccionId: number;
  numeroDocumentoEstudiante: string;
  nombresEstudiante: string;
  apellidoPaternoEstudiante: string;
  apellidoMaternoEstudiante: string;
  fechaNacimientoEstudiante: string;
  generoEstudiante: 'M' | 'F';
  numeroDocumentoApoderado: string;
  nombresApoderado: string;
  apellidoPaternoApoderado: string;
  apellidoMaternoApoderado: string;
  celularApoderado: string;
  emailApoderado: string;
  direccionApoderado: string;
  ubigeoApoderado: string;
  parentesco: 'PADRE' | 'MADRE' | 'TUTOR_LEGAL' | 'ABUELO_A' | 'OTRO';
  consentimientoGemini: true;
}

export interface SolicitudMatriculaPublicaDto {
  id: string;
  token: string | null;
  estado: EstadoSolicitudMatriculaPublica;
  nombreEstudiante: string;
  documentos: DocumentoSolicitudDto[];
  enlacePago: string | null;
  montoPago: number;
  estudianteId: number | null;
  matriculaId: number | null;
}

export interface PagoMatriculaPublicaDto {
  preferenceId: string;
  enlacePago: string;
  monto: number;
  moneda: 'PEN';
}

export interface SolicitudMatriculaPublicaAdminDto extends Omit<SolicitudMatriculaPublicaDto, 'token' | 'enlacePago' | 'montoPago'> {
  documentoEstudiante: string;
  nombreApoderado: string;
  celularApoderado: string;
  emailApoderado: string | null;
  anioLectivoId: number;
  seccionId: number;
  creadaAt: string;
}
