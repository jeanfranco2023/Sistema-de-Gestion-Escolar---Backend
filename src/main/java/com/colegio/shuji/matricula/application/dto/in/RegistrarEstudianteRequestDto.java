package com.colegio.shuji.matricula.application.dto.in;

import com.colegio.shuji.matricula.domain.enums.*;
import jakarta.validation.constraints.*;
import java.time.*;

public record RegistrarEstudianteRequestDto(
    @NotNull TipoDocumento tipoDocumento,
    @NotBlank @Size(max = 15) String numeroDocumento,
    @NotBlank @Size(max = 100) String nombres,
    @NotBlank @Size(max = 80) String apellidoPaterno,
    @NotBlank @Size(max = 80) String apellidoMaterno,
    @NotNull @Past LocalDate fechaNacimiento,
    @NotNull Genero genero,
    @Size(max = 14) String codigoEstudianteSiagie,
    @Size(max = 5) String grupoSanguineo,
    @Size(max = 4000) String alergiasCondiciones) {}
