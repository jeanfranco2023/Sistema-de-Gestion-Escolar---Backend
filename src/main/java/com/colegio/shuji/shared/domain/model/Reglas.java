package com.colegio.shuji.shared.domain.model;

import com.colegio.shuji.shared.domain.exception.BusinessException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

public final class Reglas {
  private Reglas() {}

  public static void exigir(boolean cumple, String mensaje) {
    if (!cumple) throw new BusinessException(mensaje);
  }

  public static <T> T requerido(Optional<T> valor) {
    return valor.orElseThrow(() -> new BusinessException("Registro no encontrado"));
  }

  public static void fechas(LocalDate inicio, LocalDate fin) {
    exigir(inicio != null && fin != null && fin.isAfter(inicio), "Rango de fechas inválido");
  }

  public static void horas(LocalTime inicio, LocalTime fin) {
    exigir(inicio != null && fin != null && fin.isAfter(inicio), "Rango de horas inválido");
  }
}
