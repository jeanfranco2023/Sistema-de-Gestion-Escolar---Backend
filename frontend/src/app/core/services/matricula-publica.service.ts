import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CatalogoMatriculaPublicaDto,
  CrearSolicitudMatriculaPublicaDto,
  IdentidadDniDto,
  PagoMatriculaPublicaDto,
  SolicitudMatriculaPublicaAdminDto,
  SolicitudMatriculaPublicaDto,
  TipoDocumentoSolicitud
} from '../models/matricula-publica.model';

@Injectable({ providedIn: 'root' })
export class MatriculaPublicaService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/solicitudes-matricula`;

  catalogo(): Observable<CatalogoMatriculaPublicaDto> {
    return this.http.get<CatalogoMatriculaPublicaDto>(`${this.baseUrl}/public/catalogo`);
  }

  consultarDni(dni: string): Observable<IdentidadDniDto> {
    return this.http.get<IdentidadDniDto>(`${this.baseUrl}/public/dni/${encodeURIComponent(dni)}`);
  }

  crear(payload: CrearSolicitudMatriculaPublicaDto): Observable<SolicitudMatriculaPublicaDto> {
    return this.http.post<SolicitudMatriculaPublicaDto>(`${this.baseUrl}/public`, payload);
  }

  consultar(id: string, token: string): Observable<SolicitudMatriculaPublicaDto> {
    return this.http.get<SolicitudMatriculaPublicaDto>(`${this.baseUrl}/public/${encodeURIComponent(id)}`, {
      headers: this.tokenHeaders(token)
    });
  }

  subirDocumento(
    id: string,
    token: string,
    tipo: TipoDocumentoSolicitud,
    archivo: File
  ): Observable<SolicitudMatriculaPublicaDto> {
    const data = new FormData();
    data.append('archivo', archivo, archivo.name);
    return this.http.post<SolicitudMatriculaPublicaDto>(
      `${this.baseUrl}/public/${encodeURIComponent(id)}/documentos/${tipo}`,
      data,
      { headers: this.tokenHeaders(token) }
    );
  }

  crearPago(id: string, token: string): Observable<PagoMatriculaPublicaDto> {
    return this.http.post<PagoMatriculaPublicaDto>(`${this.baseUrl}/public/${encodeURIComponent(id)}/pago`, {}, {
      headers: this.tokenHeaders(token)
    });
  }

  confirmarPago(id: string, token: string, paymentId: string): Observable<SolicitudMatriculaPublicaDto> {
    return this.http.post<SolicitudMatriculaPublicaDto>(
      `${this.baseUrl}/public/${encodeURIComponent(id)}/confirmar-pago`,
      { paymentId },
      { headers: this.tokenHeaders(token) }
    );
  }

  descargarComprobante(id: string, token: string): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/public/${encodeURIComponent(id)}/comprobante`, {
      headers: this.tokenHeaders(token),
      responseType: 'blob'
    });
  }

  descargarFicha(id: string, token: string): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/public/${encodeURIComponent(id)}/ficha`, {
      headers: this.tokenHeaders(token),
      responseType: 'blob'
    });
  }

  listarAdmin(): Observable<SolicitudMatriculaPublicaAdminDto[]> {
    return this.http.get<SolicitudMatriculaPublicaAdminDto[]>(`${this.baseUrl}/admin`);
  }

  observarAdmin(id: string, tipo: TipoDocumentoSolicitud, observacion: string): Observable<SolicitudMatriculaPublicaAdminDto> {
    return this.http.put<SolicitudMatriculaPublicaAdminDto>(`${this.baseUrl}/admin/${encodeURIComponent(id)}/observar`, {
      tipo, observacion
    });
  }

  rechazarAdmin(id: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/admin/${encodeURIComponent(id)}/rechazar`, {});
  }

  private tokenHeaders(token: string): HttpHeaders {
    return new HttpHeaders({ 'X-Solicitud-Matricula-Token': token });
  }
}
