import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ConceptoCobroResponseDto,
  GenerarObligacionesAnualesRequestDto,
  CrearPreferenciaMercadoPagoDto,
  ObligacionResponseDto,
  PreferenciaMercadoPagoResponseDto,
  RegistrarPagoCajaRequestDto,
  TransaccionResponseDto
} from '../models/tesoreria.model';

@Injectable({
  providedIn: 'root'
})
export class TesoreriaService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/tesoreria`;

  listarConceptos(): Observable<ConceptoCobroResponseDto[]> {
    return this.http.get<ConceptoCobroResponseDto[]>(`${this.baseUrl}/conceptos`);
  }

  generarCronograma(payload: GenerarObligacionesAnualesRequestDto): Observable<ObligacionResponseDto[]> {
    return this.http.post<ObligacionResponseDto[]>(`${this.baseUrl}/obligaciones`, payload);
  }

  listarObligaciones(matriculaId: number): Observable<ObligacionResponseDto[]> {
    const params = new HttpParams().set('matriculaId', matriculaId.toString());
    return this.http.get<ObligacionResponseDto[]>(`${this.baseUrl}/obligaciones`, { params });
  }

  listarMisObligaciones(): Observable<ObligacionResponseDto[]> {
    return this.http.get<ObligacionResponseDto[]>(`${this.baseUrl}/mis-obligaciones`);
  }

  registrarPagoCaja(payload: RegistrarPagoCajaRequestDto): Observable<TransaccionResponseDto> {
    return this.http.post<TransaccionResponseDto>(`${this.baseUrl}/pagos/caja`, payload);
  }

  crearPreferenciaMercadoPago(payload: CrearPreferenciaMercadoPagoDto): Observable<PreferenciaMercadoPagoResponseDto> {
    return this.http.post<PreferenciaMercadoPagoResponseDto>(
      `${this.baseUrl}/pagos/mercadopago/preferencia`,
      payload
    );
  }
}
