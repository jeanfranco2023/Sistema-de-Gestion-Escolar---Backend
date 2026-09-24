export type EstadoAsistenciaAula =
  | 'PRESENTE'
  | 'TARDANZA'
  | 'FALTA_JUSTIFICADA'
  | 'FALTA_INJUSTIFICADA';

export type TipoDiscrepancia =
  | 'ASISTENCIA_CONCILIADA'
  | 'DISCREPANCIA_FUGA'
  | 'DISCREPANCIA_OMISION_PORTERIA'
  | 'PENDIENTE_CIERRE_TURNO';

export interface AsistenciaAulaResponseDto {
  id: number;
  matriculaId: number;
  fechaSesion: string;
  horaRegistro?: string;
  estado: EstadoAsistenciaAula;
  auxiliarUsuarioId?: number;
  justificada?: boolean;
  motivoJustificacion?: string;
  documentoSustentoUrl?: string;
}

export interface ReporteAsistenciaDiariaResponseDto {
  fecha: string;
  asistencias: AsistenciaAulaResponseDto[];
}

export interface DiscrepanciaAlertaResponseDto {
  estudianteId: number;
  fecha: string;
  tipoDiscrepancia: TipoDiscrepancia;
  estudianteNombre?: string;
  dni?: string;
  marcoPorteria?: boolean;
  presenteAula?: boolean;
}

export interface ConciliacionAsistenciaResponseDto {
  id: number;
  fecha: string;
  estudianteId: number;
  marcoPorteria: boolean;
  presenteAula: boolean;
  tipoDiscrepancia: TipoDiscrepancia;
  alertaNotificada: boolean;
}

export interface RegistrarAsistenciaAulaRequestDto {
  matriculaId: number;
  fechaSesion: string;
  horaRegistro: string;
  estado: EstadoAsistenciaAula;
}

export interface JustificarInasistenciaRequestDto {
  asistenciaId: number;
  motivo: string;
  documentoSustentoUrl?: string | null;
}

export interface ImportarLoteBiometricoRequestDto {
  nombreArchivo: string;
  marcas: Array<{ dniLeido: string; fechaHora: string; dispositivoCodigo: string }>;
}

export interface LoteBiometricoResponseDto {
  id: number;
  nombreArchivo: string;
  totalFilas: number;
  marcasValidas: number;
  marcasErroneas: number;
  importadoPorUsuarioId: number;
  createdAt: string;
}
