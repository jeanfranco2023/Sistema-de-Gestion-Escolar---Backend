export type EstadoMatricula =
  | 'SOLICITADA'
  | 'RESERVADA_TEMPORAL'
  | 'MATRICULADO'
  | 'TRASLADADO'
  | 'RETIRADO'
  | 'CANCELADA';

export interface MatriculaResponseDto {
  id: number;
  uuid?: string;
  anioLectivoId: number;
  estudianteId: number;
  seccionId: number;
  fechaMatricula: string;
  estadoMatricula: EstadoMatricula;
  reservaExpiraAt?: string;
  observaciones?: string;
}

export type TipoDocumento = 'DNI' | 'CE' | 'PASAPORTE';
export type Genero = 'M' | 'F';

export interface ConsultaDniResponseDto {
  numeroDocumento: string;
  nombres: string;
  apellidoPaterno: string;
  apellidoMaterno: string;
}

export interface RegistrarEstudianteRequestDto {
  tipoDocumento: TipoDocumento;
  numeroDocumento: string;
  nombres: string;
  apellidoPaterno: string;
  apellidoMaterno: string;
  fechaNacimiento: string;
  genero: Genero;
  codigoEstudianteSiagie?: string;
  grupoSanguineo?: string;
  alergiasCondiciones?: string;
}

export interface EstudianteResponseDto {
  id: number;
  uuid?: string;
  tipoDocumento?: TipoDocumento;
  numeroDocumento: string;
  nombres: string;
  apellidoPaterno: string;
  apellidoMaterno: string;
  fechaNacimiento?: string;
  genero?: Genero;
  codigoEstudianteSiagie?: string;
  validadoReniec?: boolean;
  origenRegistro?: 'RENIEC_API' | 'MANUAL_CONTINGENCIA';
  grupoSanguineo?: string;
  alergiasCondiciones?: string;
  activo?: boolean;
  createdAt?: string;
}

export interface ApoderadoResponseDto {
  id: number;
  uuid?: string;
  usuarioId?: number;
  tipoDocumento?: TipoDocumento;
  numeroDocumento: string;
  nombres: string;
  apellidoPaterno: string;
  apellidoMaterno: string;
  celular?: string;
  email?: string;
  direccion?: string;
  ubigeoInei?: string;
  validadoReniec?: boolean;
  origenRegistro?: 'RENIEC_API' | 'MANUAL_CONTINGENCIA';
  createdAt?: string;
}

export interface FichaMatriculaResponseDto {
  matricula: MatriculaResponseDto;
  estudiante: EstudianteResponseDto;
  apoderados: ApoderadoResponseDto[];
}

export interface SolicitarMatriculaRequestDto {
  anioLectivoId: number;
  estudianteId: number;
  seccionId: number;
  reservaExpiraAt?: string;
  observaciones?: string;
}
