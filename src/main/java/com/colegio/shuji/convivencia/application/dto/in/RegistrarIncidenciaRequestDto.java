package com.colegio.shuji.convivencia.application.dto.in;

import com.colegio.shuji.convivencia.domain.enums.*;
import jakarta.validation.constraints.*;
import java.time.*;

public record RegistrarIncidenciaRequestDto(
    @NotNull Long matriculaId,
    @NotNull @PastOrPresent LocalDate fechaIncidencia,
    @NotNull TipoFalta tipoFalta,
    @NotBlank @Size(max = 10000) String descripcion,
    @NotNull Boolean requiereCitacion) {}
