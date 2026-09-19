package com.colegio.shuji.tesoreria.application.dto.out;

import com.colegio.shuji.tesoreria.domain.enums.*;
import java.math.BigDecimal;

public record ConceptoCobroResponseDto(
    Short id, String codigo, String nombre, TipoConcepto tipoConcepto, BigDecimal montoSugerido) {}
