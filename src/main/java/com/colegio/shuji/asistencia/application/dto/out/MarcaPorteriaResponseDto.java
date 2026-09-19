package com.colegio.shuji.asistencia.application.dto.out;

import com.colegio.shuji.asistencia.domain.enums.*;
import java.time.*;

public record MarcaPorteriaResponseDto(
    Long id,
    Long loteId,
    String dniLeido,
    OffsetDateTime fechaHora,
    String dispositivoCodigo,
    Long estudianteId,
    EstadoMarca estadoProcesamiento,
    OffsetDateTime createdAt) {}
