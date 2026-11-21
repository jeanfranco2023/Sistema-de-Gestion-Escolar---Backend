package com.colegio.shuji.matricula.application.dto.in;

import com.colegio.shuji.matricula.domain.enums.Genero;
import com.colegio.shuji.matricula.domain.enums.ModalidadIngreso;
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
    @NotNull @Positive Short gradoId,
    @NotBlank @Pattern(regexp = "^[0-9]{8}$", message = "El DNI del estudiante debe tener 8 dígitos numéricos") String numeroDocumentoEstudiante,
    @NotBlank @Size(max = 30) @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s'-]+$", message = "Los nombres del estudiante solo deben contener letras y espacios") String nombresEstudiante,
    @NotBlank @Size(max = 30) @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s'-]+$", message = "El apellido paterno del estudiante solo deben contener letras y espacios") String apellidoPaternoEstudiante,
    @NotBlank @Size(max = 30) @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s'-]+$", message = "El apellido materno del estudiante solo debe contener letras y espacios") String apellidoMaternoEstudiante,
    @NotNull LocalDate fechaNacimientoEstudiante,
    @NotNull Genero generoEstudiante,
    @NotBlank @Pattern(regexp = "^[0-9]{8}$", message = "El DNI del apoderado debe tener 8 dígitos numéricos") String numeroDocumentoApoderado,
    @NotBlank @Size(max = 30) @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s'-]+$", message = "Los nombres del apoderado solo deben contener letras y espacios") String nombresApoderado,
    @NotBlank @Size(max = 30) @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s'-]+$", message = "El apellido paterno del apoderado solo debe contener letras y espacios") String apellidoPaternoApoderado,
    @NotBlank @Size(max = 30) @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s'-]+$", message = "El apellido materno del apoderado solo debe contener letras y espacios") String apellidoMaternoApoderado,
    @NotBlank @Pattern(regexp = "^9[0-9]{8}$", message = "El celular del apoderado debe tener 9 dígitos numéricos") String celularApoderado,
    @NotBlank @Email @Size(max = 100) String emailApoderado,
    @NotBlank @Size(max = 150) @Pattern(regexp = "^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑüÜ\\s#.,\\-_/°º]+$", message = "La dirección no permite caracteres especiales extraños") String direccionApoderado,
    @Pattern(regexp = "^([0-9]{6})?$", message = "El ubigeo debe tener 6 dígitos numéricos si se especifica") String ubigeoApoderado,
    @NotNull Parentesco parentesco,
    @AssertTrue boolean consentimientoGemini,
    ModalidadIngreso modalidadIngreso,
    @Size(max = 150) String colegioProcedencia) {

  public CrearSolicitudMatriculaPublicaRequestDto {
    if (modalidadIngreso == null) {
      modalidadIngreso = ModalidadIngreso.REGULAR;
    }
    if (colegioProcedencia != null) {
      colegioProcedencia = colegioProcedencia.trim();
      if (colegioProcedencia.isEmpty()) colegioProcedencia = null;
    }
  }

  public CrearSolicitudMatriculaPublicaRequestDto(
      Short anioLectivoId, Short gradoId,
      String numeroDocumentoEstudiante, String nombresEstudiante, String apellidoPaternoEstudiante, String apellidoMaternoEstudiante,
      LocalDate fechaNacimientoEstudiante, Genero generoEstudiante,
      String numeroDocumentoApoderado, String nombresApoderado, String apellidoPaternoApoderado, String apellidoMaternoApoderado,
      String celularApoderado, String emailApoderado, String direccionApoderado, String ubigeoApoderado,
      Parentesco parentesco, boolean consentimientoGemini) {
    this(anioLectivoId, gradoId,
        numeroDocumentoEstudiante, nombresEstudiante, apellidoPaternoEstudiante, apellidoMaternoEstudiante,
        fechaNacimientoEstudiante, generoEstudiante,
        numeroDocumentoApoderado, nombresApoderado, apellidoPaternoApoderado, apellidoMaternoApoderado,
        celularApoderado, emailApoderado, direccionApoderado, ubigeoApoderado,
        parentesco, consentimientoGemini, ModalidadIngreso.REGULAR, null);
  }

  @AssertTrue(message = "La fecha de nacimiento debe ser anterior al día de hoy en Perú")
  public boolean isFechaNacimientoAnteriorAlDiaActualEnPeru() {
    return fechaNacimientoEstudiante == null
        || fechaNacimientoEstudiante.isBefore(LocalDate.now(ZoneId.of("America/Lima")));
  }

  @AssertTrue(message = "La solicitud pública solo admite al padre o a la madre")
  public boolean isProgenitorAdmitido() {
    return parentesco == Parentesco.PADRE || parentesco == Parentesco.MADRE;
  }

  @AssertTrue(message = "En solicitudes por traslado debes indicar el colegio de procedencia")
  public boolean isColegioProcedenciaValidoSiTraslado() {
    if (modalidadIngreso != ModalidadIngreso.TRASLADO) return true;
    return colegioProcedencia != null && colegioProcedencia.trim().length() >= 3;
  }
}
