package com.colegio.shuji.matricula.domain.model;

import com.colegio.shuji.matricula.domain.enums.Genero;
import com.colegio.shuji.matricula.domain.enums.OrigenRegistro;
import com.colegio.shuji.matricula.domain.enums.TipoDocumento;
import com.colegio.shuji.shared.domain.model.Reglas;
import java.time.LocalDate;
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
public class Estudiante {
  private Long id;
  private UUID uuid;
  private TipoDocumento tipoDocumento;
  private String numeroDocumento;
  private String nombres;
  private String apellidoPaterno;
  private String apellidoMaterno;
  private LocalDate fechaNacimiento;
  private Genero genero;
  private String codigoEstudianteSiagie;
  private Boolean validadoReniec;
  private OrigenRegistro origenRegistro;
  private String grupoSanguineo;
  private String alergiasCondiciones;
  private Boolean activo;
  private OffsetDateTime createdAt;

  public String nombreCompleto() {
    return nombres + " " + apellidoPaterno + " " + (apellidoMaterno == null ? "" : apellidoMaterno);
  }

  public void actualizarDatos(
      String nombres,
      String apellidoPaterno,
      String apellidoMaterno,
      LocalDate fechaNacimiento,
      Genero genero) {
    this.nombres = nombres;
    this.apellidoPaterno = apellidoPaterno;
    this.apellidoMaterno = apellidoMaterno;
    this.fechaNacimiento = fechaNacimiento;
    this.genero = genero;
  }

  public void actualizarInformacionMedica(String grupoSanguineo, String alergiasCondiciones) {
    this.grupoSanguineo = grupoSanguineo;
    this.alergiasCondiciones = alergiasCondiciones;
  }

  public void actualizarCodigoSiagie(String codigoSiagie) {
    this.codigoEstudianteSiagie = codigoSiagie;
  }

  public void marcarValidadoReniec() {
    this.validadoReniec = true;
    this.origenRegistro = OrigenRegistro.RENIEC_API;
  }

  public void activar() {
    this.activo = true;
  }

  public void desactivar() {
    this.activo = false;
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
