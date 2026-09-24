export type CalificacionCualitativa = 'AD' | 'A' | 'B' | 'C';
export interface FilaCalificacionResponseDto { matriculaId: number; competenciaId: number; calificacionCualitativa: CalificacionCualitativa | null; conclusionDescriptiva: string | null; requiereRefuerzo: boolean; }

export interface CalificacionResponseDto {
  id: number;
  matriculaId: number;
  anioLectivoId: number;
  seccionId: number;
  periodoAcademicoId: number;
  asignacionDocenteId?: number;
  areaCurricularId?: number;
  docenteUsuarioId?: number;
  competenciaId?: number;
  calificacionCualitativa: CalificacionCualitativa;
  conclusionDescriptiva?: string;
  sugerenciaIaUtilizada?: boolean;
  requiereRefuerzo?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface EstudiantesEnRiesgoResponseDto {
  periodoId: number;
  calificaciones: CalificacionResponseDto[];
}

export interface LibretaNotasResponseDto {
  matriculaId: number;
  calificaciones: CalificacionResponseDto[];
}

export interface ItemCalificacionRequestDto {
  matriculaId: number;
  competenciaId: number;
  calificacionCualitativa: CalificacionCualitativa;
  conclusionDescriptiva?: string;
  sugerenciaIaUtilizada: boolean;
}

export interface RegistrarCalificacionesMasivasRequestDto {
  periodoAcademicoId: number;
  asignacionDocenteId: number;
  calificaciones: ItemCalificacionRequestDto[];
}
