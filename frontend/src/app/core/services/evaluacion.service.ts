import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  FilaCalificacionResponseDto,
  CalificacionResponseDto,
  EstudiantesEnRiesgoResponseDto,
  LibretaNotasResponseDto,
  RegistrarCalificacionesMasivasRequestDto
} from '../models/evaluacion.model';

@Injectable({
  providedIn: 'root'
})
export class EvaluacionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/evaluacion`;

  listarMatriz(asignacionDocenteId: number, periodoAcademicoId: number, competenciaId: number): Observable<FilaCalificacionResponseDto[]> {
    const params = new HttpParams().set('asignacionDocenteId', asignacionDocenteId).set('periodoAcademicoId', periodoAcademicoId).set('competenciaId', competenciaId);
    return this.http.get<FilaCalificacionResponseDto[]>(`${this.baseUrl}/cneb/matriz`, { params });
  }

  listarCalificaciones(asignacionDocenteId: number, periodoAcademicoId: number): Observable<CalificacionResponseDto[]> {
    const params = new HttpParams().set('asignacionDocenteId', asignacionDocenteId).set('periodoAcademicoId', periodoAcademicoId);
    return this.http.get<CalificacionResponseDto[]>(`${this.baseUrl}/cneb`, { params });
  }

  consultarEstudiantesEnRiesgo(periodoId: number): Observable<EstudiantesEnRiesgoResponseDto> {
    const params = new HttpParams().set('periodoId', periodoId.toString());
    return this.http.get<EstudiantesEnRiesgoResponseDto>(`${this.baseUrl}/riesgo`, { params });
  }

  registrarCalificaciones(payload: RegistrarCalificacionesMasivasRequestDto): Observable<CalificacionResponseDto[]> {
    return this.http.post<CalificacionResponseDto[]>(`${this.baseUrl}/cneb`, payload);
  }

  consultarLibreta(matriculaId: number): Observable<LibretaNotasResponseDto> {
    return this.http.get<LibretaNotasResponseDto>(`${this.baseUrl}/libreta/${matriculaId}`);
  }
}
