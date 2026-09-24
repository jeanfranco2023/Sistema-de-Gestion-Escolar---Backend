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

public record CrearSolicitudMatriculaPublicaRequestDto(
    @NotNull @Positive Short anioLectivoId,
    @NotNull @Positive Integer seccionId,
    @NotBlank @Pattern(regexp = "[0-9]{8}") String numeroDocumentoEstudiante,
    @NotBlank @Size(max = 100) String nombresEstudiante,
    @NotBlank @Size(max = 80) String apellidoPaternoEstudiante,
    @NotBlank @Size(max = 80) String apellidoMaternoEstudiante,
    @NotNull java.time.LocalDate fechaNacimientoEstudiante,
    @NotNull Genero generoEstudiante,
    @NotBlank @Pattern(regexp = "[0-9]{8}") String numeroDocumentoApoderado,
    @NotBlank @Size(max = 100) String nombresApoderado,
    @NotBlank @Size(max = 80) String apellidoPaternoApoderado,
    @NotBlank @Size(max = 80) String apellidoMaternoApoderado,
    @NotBlank @Pattern(regexp = "9[0-9]{8}") String celularApoderado,
    @Email @Size(max = 100) String emailApoderado,
    @NotBlank @Size(max = 200) String direccionApoderado,
    @NotBlank @Pattern(regexp = "[0-9]{6}") String ubigeoApoderado,
    @NotNull Parentesco parentesco,
    @AssertTrue boolean consentimientoGemini) {}
