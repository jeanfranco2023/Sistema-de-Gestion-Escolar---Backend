package com.colegio.shuji.matricula.domain.model;

import static com.colegio.shuji.shared.domain.model.Reglas.exigir;
import com.colegio.shuji.matricula.domain.enums.EstadoSuplencia;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class Suplencia {
  private Long id;
  private Long estudianteId;
  private Long apoderadoTitularId;
  private String numeroDocumento;
  private String nombres;
  private String apellidoPaterno;
  private String apellidoMaterno;
  private String parentesco;
  private String celular;
  private LocalDate fechaInicio;
  private LocalDate fechaFin;
  private EstadoSuplencia estado;
  private String motivoResolucion;
  private Long autorizadoPorUsuarioId;
  private OffsetDateTime fechaSolicitud;
  private OffsetDateTime fechaResolucion;

  public void aprobar(Long usuarioId, LocalDate hoy) {
    exigir(estado == EstadoSuplencia.PENDIENTE_APROBACION, "La solicitud ya fue resuelta");
    exigir(!fechaFin.isBefore(hoy), "El periodo solicitado ya venció");
    resolver(EstadoSuplencia.ACTIVO, usuarioId, null);
  }
  public void rechazar(Long usuarioId, String motivo) {
    exigir(estado == EstadoSuplencia.PENDIENTE_APROBACION, "Solo se rechazan solicitudes pendientes");
    motivo(motivo); resolver(EstadoSuplencia.RECHAZADO, usuarioId, motivo.strip());
  }
  public void revocar(Long usuarioId, String motivo) {
    exigir(estado == EstadoSuplencia.ACTIVO, "Solo se revocan suplencias aprobadas");
    motivo(motivo); resolver(EstadoSuplencia.REVOCADO, usuarioId, motivo.strip());
  }
  private void motivo(String motivo) { exigir(motivo != null && !motivo.isBlank() && motivo.strip().length() <= 500, "Indica un motivo de hasta 500 caracteres"); }
  private void resolver(EstadoSuplencia estado, Long usuarioId, String motivo) {
    this.estado=estado; this.autorizadoPorUsuarioId=usuarioId; this.motivoResolucion=motivo;
    this.fechaResolucion=OffsetDateTime.now(ZoneOffset.UTC);
  }
  public String estadoVisible(LocalDate hoy) { return estado == EstadoSuplencia.ACTIVO && fechaFin.isBefore(hoy) ? "VENCIDO" : estado.name(); }
}
