export type RolNombre = 'DIRECCION' | 'SECRETARIA' | 'DOCENTE' | 'AUXILIAR' | 'TUTOR' | 'APODERADO';

export interface RolResponseDto {
  id: number;
  codigo: RolNombre;
  nombre: string;
}

export interface UserResponseDto {
  id: number;
  uuid: string;
  username: string;
  email: string;
  activo: boolean;
  roles: RolResponseDto[];
  createdAt: string;
  updatedAt: string;
}

export interface LoginRequestDto {
  username: string;
  password: string;
}

export interface RefreshTokenRequestDto {
  refreshToken: string;
}

export interface AuthResponseDto {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserResponseDto;
}
