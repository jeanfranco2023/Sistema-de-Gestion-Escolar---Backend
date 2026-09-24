import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { UserResponseDto } from '../models/auth.model';

@Injectable({
  providedIn: 'root'
})
export class UsuarioService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/usuarios`;

  listarUsuarios(rol?: string): Observable<UserResponseDto[]> {
    let params = new HttpParams();
    if (rol && rol.trim().length > 0) {
      params = params.set('rol', rol.trim());
    }
    return this.http
      .get<ApiResponse<UserResponseDto[]>>(this.baseUrl, { params })
      .pipe(map(res => res.data ?? []));
  }

  crearUsuario(payload: any): Observable<UserResponseDto> {
    return this.http
      .post<ApiResponse<UserResponseDto>>(this.baseUrl, payload)
      .pipe(map(res => res.data));
  }

  cambiarEstado(id: number, activo: boolean): Observable<UserResponseDto> {
    return this.http
      .patch<ApiResponse<UserResponseDto>>(`${this.baseUrl}/${id}/estado`, { activo })
      .pipe(map(res => res.data));
  }

  cambiarPassword(payload: any): Observable<void> {
    return this.http
      .put<ApiResponse<void>>(`${this.baseUrl}/password`, payload)
      .pipe(map(() => undefined));
  }

  obtenerPerfil(): Observable<UserResponseDto> {
    return this.http
      .get<ApiResponse<UserResponseDto>>(`${this.baseUrl}/me`)
      .pipe(map(res => res.data));
  }
}
