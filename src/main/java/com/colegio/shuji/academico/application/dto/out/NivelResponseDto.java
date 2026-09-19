package com.colegio.shuji.academico.application.dto.out;

import com.colegio.shuji.academico.domain.enums.NivelCodigo;

public record NivelResponseDto(Short id, NivelCodigo codigo, String nombre) {}
