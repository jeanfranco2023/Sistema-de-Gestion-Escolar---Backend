package com.colegio.shuji.matricula.domain.model;

import com.colegio.shuji.matricula.domain.enums.OrigenRegistro;
import com.colegio.shuji.matricula.domain.enums.TipoDocumento;
import com.colegio.shuji.shared.domain.model.Reglas;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Apoderado {
  private Long id;
  private UUID uuid;
  private Long usuarioId;
  private TipoDocumento tipoDocumento;
  private String numeroDocumento;
  private String nombres;
  private String apellidoPaterno;
  private String apellidoMaterno;
  private String celular;
  private String email;
  private String direccion;
  private String ubigeoInei;
  private Boolean validadoReniec;
  private OrigenRegistro origenRegistro;
  private OffsetDateTime createdAt;

  public String nombreCompleto() {
    return nombres + " " + apellidoPaterno + " " + (apellidoMaterno == null ? "" : apellidoMaterno);
  }

  public void actualizarContacto(
      String celular, String email, String direccion, String ubigeoInei) {
    this.celular = celular;
    this.email = email;
    this.direccion = direccion;
    this.ubigeoInei = ubigeoInei;
  }

  public void vincularUsuario(Long usuarioId) {
    this.usuarioId = usuarioId;
  }

  public void marcarValidadoReniec() {
    this.validadoReniec = true;
    this.origenRegistro = OrigenRegistro.RENIEC_API;
  }

  public void iniciarRegistroManual() {
    Reglas.exigir(id == null, "El registro ya existe");
    validadoReniec = false;
    origenRegistro = OrigenRegistro.MANUAL_CONTINGENCIA;
  }

  public void verificarIdentidad(String documento, String nombres, String paterno, String materno) {
    Reglas.exigir(
        tipoDocumento == TipoDocumento.DNI && java.util.Objects.equals(numeroDocumento, documento),
        "RENIEC devolvió otro documento");
    Reglas.exigir(
        nombres != null
            && !nombres.isBlank()
            && paterno != null
            && !paterno.isBlank()
            && materno != null,
        "Identidad RENIEC incompleta");
    this.nombres = nombres;
    apellidoPaterno = paterno;
    apellidoMaterno = materno;
    marcarValidadoReniec();
  }
}
