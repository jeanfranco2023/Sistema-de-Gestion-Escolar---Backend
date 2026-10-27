package com.colegio.shuji.evaluacion.application.dto.out;

import java.util.List;

public record ImportacionCalificacionesResponseDto(int filasValidadas, int filasProcesadas, List<Fila> filas) {
  public record Fila(int fila, String dni, String estudiante, Short competenciaId, String nota, String conclusion) {}
}
