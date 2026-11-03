package com.colegio.shuji.matricula.application.dto.in;

import com.colegio.shuji.matricula.domain.enums.EstadoMatricula;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CambiarEstadoMatriculaRequestDto(
    @NotNull EstadoMatricula estado,
    @NotBlank @Size(max = 500) String motivo) {}
