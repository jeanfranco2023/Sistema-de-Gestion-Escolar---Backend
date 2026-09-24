import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AsignacionDocenteResponseDto,
  AnioLectivoResponseDto,
  CompetenciaResponseDto,
  AulaResponseDto,
  NivelResponseDto,
  PeriodoResponseDto,
  SeccionResponseDto
} from '../models/academico.model';

@Injectable({
  providedIn: 'root'
})
export class AcademicoService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/academico`;

  listarAsignaciones(): Observable<AsignacionDocenteResponseDto[]> {
    return this.http.get<AsignacionDocenteResponseDto[]>(`${environment.apiUrl}/curriculo/asignaciones`);
  }

  listarAnios(): Observable<AnioLectivoResponseDto[]> {
    return this.http.get<AnioLectivoResponseDto[]>(`${this.baseUrl}/anios`);
  }

  listarCompetencias(areaId: number): Observable<CompetenciaResponseDto[]> {
    return this.http.get<CompetenciaResponseDto[]>(`${environment.apiUrl}/curriculo/competencias`, { params: new HttpParams().set('areaId', areaId) });
  }

  listarAulas(): Observable<AulaResponseDto[]> {
    return this.http.get<AulaResponseDto[]>(`${this.baseUrl}/aulas`);
  }

  crearAula(payload: { codigo: string; nombre: string; ubicacion?: string; capacidad: number }): Observable<AulaResponseDto> {
    return this.http.post<AulaResponseDto>(`${this.baseUrl}/aulas`, payload);
  }

  listarNiveles(): Observable<NivelResponseDto[]> {
    return this.http.get<NivelResponseDto[]>(`${this.baseUrl}/niveles`);
  }

  listarSecciones(anioId: number, nivelId?: number): Observable<SeccionResponseDto[]> {
    let params = new HttpParams().set('anioId', anioId.toString());
    if (nivelId !== undefined && nivelId !== null) {
      params = params.set('nivelId', nivelId.toString());
    }
    return this.http.get<SeccionResponseDto[]>(`${this.baseUrl}/secciones`, { params });
  }

  listarPeriodos(anioId: number): Observable<PeriodoResponseDto[]> {
    const params = new HttpParams().set('anioId', anioId.toString());
    return this.http.get<PeriodoResponseDto[]>(`${this.baseUrl}/periodos`, { params });
  }

  crearAnio(payload: { anio: number; fechaInicio: string; fechaFin: string }): Observable<AnioLectivoResponseDto> {
    return this.http.post<AnioLectivoResponseDto>(`${this.baseUrl}/anios`, payload);
  }

  listarGrados(nivelId: number): Observable<any[]> {
    const params = new HttpParams().set('nivelId', nivelId.toString());
    return this.http.get<any[]>(`${this.baseUrl}/grados`, { params });
  }

  crearGrado(payload: { nivelId: number; numeroGrado: number; nombre: string }): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/grados`, payload);
  }

  crearSeccion(payload: {
    anioLectivoId: number;
    gradoId: number;
    nivelId: number;
    letra: string;
    cupoMaximo: number;
    aulaFisica: string;
  }): Observable<SeccionResponseDto> {
    return this.http.post<SeccionResponseDto>(`${this.baseUrl}/secciones`, payload);
  }
}
