export type TipoFalta = 'LEVE' | 'GRAVE' | 'MUY_GRAVE';
export type EstadoIncidencia = 'ABIERTA' | 'ATENDIDA' | 'CERRADA';

export interface IncidenciaResponseDto {
  id: number;
  matriculaId: number;
  fechaIncidencia: string;
  tipoFalta: TipoFalta;
  descripcion: string;
  reportadoPorUsuarioId: number;
  requiereCitacion: boolean;
  estado: EstadoIncidencia;
  createdAt?: string;
}

export interface RegistrarIncidenciaRequestDto {
  matriculaId: number;
  fechaIncidencia: string;
  tipoFalta: TipoFalta;
  descripcion: string;
  requiereCitacion: boolean;
}
