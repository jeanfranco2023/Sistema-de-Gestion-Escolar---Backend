package com.colegio.shuji.convivencia.application.dto.in;

import com.colegio.shuji.convivencia.domain.enums.TipoFalta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record RegistrarIncidenciaRequestDto(
    @NotNull Long matriculaId,
    @NotNull @PastOrPresent LocalDate fechaIncidencia,
    @NotNull TipoFalta tipoFalta,
    @NotBlank @Size(max = 10000) String descripcion,
    @NotNull Boolean requiereCitacion) {}
