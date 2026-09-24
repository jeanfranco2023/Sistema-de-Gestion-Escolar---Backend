import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  IncidenciaResponseDto,
  RegistrarIncidenciaRequestDto
} from '../models/convivencia.model';

@Injectable({
  providedIn: 'root'
})
export class ConvivenciaService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/convivencia`;

  listarIncidencias(): Observable<IncidenciaResponseDto[]> {
    return this.http.get<IncidenciaResponseDto[]>(`${this.baseUrl}/incidencias`);
  }

  listarCitaciones(page: number = 0, size: number = 20): Observable<IncidenciaResponseDto[]> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<IncidenciaResponseDto[]>(`${this.baseUrl}/incidencias/citaciones`, { params });
  }

  registrarIncidencia(payload: RegistrarIncidenciaRequestDto): Observable<IncidenciaResponseDto> {
    return this.http.post<IncidenciaResponseDto>(`${this.baseUrl}/incidencias`, payload);
  }
}
