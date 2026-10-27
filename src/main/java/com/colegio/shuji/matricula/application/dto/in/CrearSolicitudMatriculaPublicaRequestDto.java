package com.colegio.shuji.matricula.application.dto.in;

import com.colegio.shuji.matricula.domain.enums.Genero;
import com.colegio.shuji.matricula.domain.enums.Parentesco;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.ZoneId;

public record CrearSolicitudMatriculaPublicaRequestDto(
    @NotNull @Positive Short anioLectivoId,
    @NotNull @Positive Integer seccionId,
    @NotBlank @Pattern(regexp = "[0-9]{8}") String numeroDocumentoEstudiante,
    @NotBlank @Size(max = 20) String nombresEstudiante,
    @NotBlank @Size(max = 20) String apellidoPaternoEstudiante,
    @NotBlank @Size(max = 20) String apellidoMaternoEstudiante,
    @NotNull LocalDate fechaNacimientoEstudiante,
    @NotNull Genero generoEstudiante,
    @NotBlank @Pattern(regexp = "[0-9]{8}") String numeroDocumentoApoderado,
    @NotBlank @Size(max = 20) String nombresApoderado,
    @NotBlank @Size(max = 20) String apellidoPaternoApoderado,
    @NotBlank @Size(max = 20) String apellidoMaternoApoderado,
    @NotBlank @Pattern(regexp = "9[0-9]{8}") String celularApoderado,
    @Email @Size(max = 50) String emailApoderado,
    @NotBlank @Size(max = 150) String direccionApoderado,
    @NotBlank @Pattern(regexp = "[0-9]{6}") String ubigeoApoderado,
    @NotNull Parentesco parentesco,
    @AssertTrue boolean consentimientoGemini) {

  @AssertTrue(message = "La fecha de nacimiento debe ser anterior al día de hoy en Perú")
  public boolean isFechaNacimientoAnteriorAlDiaActualEnPeru() {
    return fechaNacimientoEstudiante == null
        || fechaNacimientoEstudiante.isBefore(LocalDate.now(ZoneId.of("America/Lima")));
  }

  @AssertTrue(message = "La solicitud pública solo admite al padre o a la madre")
  public boolean isProgenitorAdmitido() {
    return parentesco == Parentesco.PADRE || parentesco == Parentesco.MADRE;
  }
}
