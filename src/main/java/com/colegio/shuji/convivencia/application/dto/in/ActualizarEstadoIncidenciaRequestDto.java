package com.colegio.shuji.convivencia.application.dto.in;

import com.colegio.shuji.convivencia.domain.enums.*;
import jakarta.validation.constraints.*;

public record ActualizarEstadoIncidenciaRequestDto(@NotNull EstadoIncidencia estado) {}
