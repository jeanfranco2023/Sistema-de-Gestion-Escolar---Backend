export interface ComunicadoResponseDto {
  id: number;
  titulo: string;
  contenido: string;
  remitenteUsuarioId: number;
  fechaPublicacion: string;
  requiereAcuse: boolean;
}

export interface BandejaApoderadoResponseDto {
  apoderadoId: number;
  comunicados: ComunicadoResponseDto[];
}

export interface MetricasLecturaResponseDto {
  comunicadoId: number;
  destinatarios: number;
  leidos: number;
  acuses: number;
}

export interface EmitirComunicadoRequestDto {
  titulo: string;
  contenido: string;
  requiereAcuse: boolean;
  anioLectivoId: number;
  nivelId?: number;
  seccionId?: number;
}
