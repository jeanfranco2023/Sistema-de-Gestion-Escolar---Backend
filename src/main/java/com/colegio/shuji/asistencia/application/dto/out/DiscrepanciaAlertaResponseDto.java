package com.colegio.shuji.asistencia.application.dto.out;

import com.colegio.shuji.asistencia.domain.enums.TipoDiscrepancia;
import java.time.LocalDate;

public record DiscrepanciaAlertaResponseDto(
    Long estudianteId, String nombreEstudiante, LocalDate fecha, TipoDiscrepancia tipoDiscrepancia) {}
