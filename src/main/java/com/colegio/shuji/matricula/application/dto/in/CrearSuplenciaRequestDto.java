package com.colegio.shuji.matricula.application.dto.in;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.ZoneId;

public record CrearSuplenciaRequestDto(@NotNull @Positive Long estudianteId,
    @NotBlank @Pattern(regexp="[0-9]{8}") String numeroDocumento,
    @NotBlank @Size(max=20) String nombres, @NotBlank @Size(max=20) String apellidoPaterno,
    @NotBlank @Size(max=20) String apellidoMaterno, @NotBlank @Size(max=30) String parentesco,
    @NotBlank @Pattern(regexp="9[0-9]{8}") String celular,
    @NotNull LocalDate fechaInicio, @NotNull LocalDate fechaFin) {
  @AssertTrue(message="Las fechas deben ser válidas, desde hoy en Perú y con fin posterior o igual al inicio")
  public boolean isPeriodoValido() {
    return fechaInicio == null || fechaFin == null
        || (!fechaInicio.isBefore(LocalDate.now(ZoneId.of("America/Lima"))) && !fechaFin.isBefore(fechaInicio));
  }
}
