package com.colegio.shuji.asistencia.application.dto.out;

import com.colegio.shuji.asistencia.domain.enums.*;
import java.time.*;

public record DiscrepanciaAlertaResponseDto(
    Long estudianteId, LocalDate fecha, TipoDiscrepancia tipoDiscrepancia) {}
