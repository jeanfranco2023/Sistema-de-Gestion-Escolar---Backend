package com.colegio.shuji.matricula.application.dto.in;

import com.colegio.shuji.matricula.domain.enums.Genero;
import com.colegio.shuji.matricula.domain.enums.TipoDocumento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record RegistrarEstudianteRequestDto(
    @NotNull TipoDocumento tipoDocumento,
    @NotBlank @Size(min = 8, max = 8) @Pattern(regexp = "[0-9]{8}") String numeroDocumento,
    @NotBlank @Size(max = 30) @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s'-]+$", message = "Los nombres solo deben contener letras y espacios") String nombres,
    @NotBlank @Size(max = 30) @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s'-]+$", message = "El apellido paterno solo debe contener letras y espacios") String apellidoPaterno,
    @NotBlank @Size(max = 30) @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s'-]+$", message = "El apellido materno solo debe contener letras y espacios") String apellidoMaterno,
    @NotNull @Past LocalDate fechaNacimiento,
    @NotNull Genero genero,
    @Size(max = 5) String grupoSanguineo,
    @Size(max = 4000) String alergiasCondiciones,
    @Valid ApoderadoMatriculaRequestDto apoderado) {

  public RegistrarEstudianteRequestDto(
      TipoDocumento tipoDocumento, String numeroDocumento, String nombres,
      String apellidoPaterno, String apellidoMaterno, LocalDate fechaNacimiento,
      Genero genero, String grupoSanguineo, String alergiasCondiciones) {
    this(tipoDocumento, numeroDocumento, nombres, apellidoPaterno, apellidoMaterno,
        fechaNacimiento, genero, grupoSanguineo, alergiasCondiciones, null);
  }
}
