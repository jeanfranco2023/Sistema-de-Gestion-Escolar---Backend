export type NivelCodigo = 'INICIAL' | 'PRIMARIA' | 'SECUNDARIA';
export interface AsignacionDocenteResponseDto { id: number; docenteUsuarioId: number; seccionId: number; anioLectivoId: number; nivelId: number; areaCurricularId: number; }
export interface CompetenciaResponseDto { id: number; areaId: number; numeroOrden: number; nombre: string; descripcion: string; }

export interface AulaResponseDto {
  id: number;
  codigo: string;
  nombre: string;
  ubicacion: string;
  capacidad: number;
  activa: boolean;
}

export interface NivelResponseDto {
  id: number;
  codigo: NivelCodigo;
  nombre: string;
}

export interface SeccionResponseDto {
  id: number;
  anioLectivoId: number;
  gradoId: number;
  nivelId: number;
  letra: string;
  cupoMaximo: number;
  vacantesOcupadas: number;
  aulaFisica?: string;
}

export interface PeriodoResponseDto {
  id: number;
  anioLectivoId: number;
  numeroPeriodo: number;
  nombre: string;
  fechaInicio: string;
  fechaFin: string;
  cerrado: boolean;
}

export interface AnioLectivoResponseDto {
  id: number;
  anio: number;
  fechaInicio: string;
  fechaFin: string;
  abierto: boolean;
}
