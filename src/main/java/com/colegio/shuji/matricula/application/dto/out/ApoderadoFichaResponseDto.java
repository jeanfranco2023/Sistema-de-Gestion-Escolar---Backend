package com.colegio.shuji.matricula.application.dto.out;

import com.colegio.shuji.matricula.domain.enums.Parentesco;

public record ApoderadoFichaResponseDto(
    ApoderadoResponseDto apoderado,
    Parentesco parentesco,
    Boolean esResponsableEconomico) {}
