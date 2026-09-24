import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  FichaMatriculaResponseDto,
  MatriculaResponseDto,
  SolicitarMatriculaRequestDto,
  RegistrarEstudianteRequestDto,
  EstudianteResponseDto,
  ConsultaDniResponseDto
} from '../models/matricula.model';

@Injectable({
  providedIn: 'root'
})
export class MatriculaService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/matriculas`;
  private readonly estudiantesUrl = `${environment.apiUrl}/estudiantes`;

  listarPorSeccion(seccionId: number, page: number = 0, size: number = 20): Observable<MatriculaResponseDto[]> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<MatriculaResponseDto[]>(`${this.baseUrl}/seccion/${seccionId}`, { params });
  }

  consultarFicha(id: number): Observable<FichaMatriculaResponseDto> {
    return this.http.get<FichaMatriculaResponseDto>(`${this.baseUrl}/${id}`);
  }

  solicitarMatricula(payload: SolicitarMatriculaRequestDto): Observable<MatriculaResponseDto> {
    return this.http.post<MatriculaResponseDto>(this.baseUrl, payload);
  }

  confirmarMatricula(matriculaId: number): Observable<MatriculaResponseDto> {
    return this.http.post<MatriculaResponseDto>(`${this.baseUrl}/confirmacion`, { matriculaId });
  }

  registrarEstudiante(payload: RegistrarEstudianteRequestDto): Observable<EstudianteResponseDto> {
    return this.http.post<EstudianteResponseDto>(this.estudiantesUrl, {
      ...payload,
      codigoEstudianteSiagie: payload.codigoEstudianteSiagie?.trim() || undefined
    });
  }

  consultarDni(dni: string): Observable<ConsultaDniResponseDto> {
    return this.http.get<ConsultaDniResponseDto>(`${this.estudiantesUrl}/dni/${encodeURIComponent(dni)}`);
  }

  listarEstudiantes(): Observable<EstudianteResponseDto[]> {
    return this.http.get<EstudianteResponseDto[]>(this.estudiantesUrl);
  }

  consultarEstudiante(id: number): Observable<EstudianteResponseDto> {
    return this.http.get<EstudianteResponseDto>(`${this.estudiantesUrl}/${id}`);
  }
}
