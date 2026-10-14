package com.colegio.shuji.matricula.application.dto.in;

import com.colegio.shuji.matricula.domain.enums.Genero;
import com.colegio.shuji.matricula.domain.enums.TipoDocumento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record RegistrarEstudianteRequestDto(
    @NotNull TipoDocumento tipoDocumento,
    @NotBlank @Size(min = 8, max = 8) @Pattern(regexp = "[0-9]{8}") String numeroDocumento,
    @NotBlank @Size(max = 20) String nombres,
    @NotBlank @Size(max = 20) String apellidoPaterno,
    @NotBlank @Size(max = 20) String apellidoMaterno,
    @NotNull @Past LocalDate fechaNacimiento,
    @NotNull Genero genero,
    @Size(max = 14) String codigoEstudianteSiagie,
    @Size(max = 5) String grupoSanguineo,
    @Size(max = 4000) String alergiasCondiciones) {}
