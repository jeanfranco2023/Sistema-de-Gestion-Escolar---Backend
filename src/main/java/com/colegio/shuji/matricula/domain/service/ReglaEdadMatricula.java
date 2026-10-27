package com.colegio.shuji.matricula.domain.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.Locale;

/** Rangos EBR de la RM 010-2026-MINEDU, VI.9; corte del año solicitado. */
public final class ReglaEdadMatricula {
  private ReglaEdadMatricula() {}

  public static Rango paraGrado(String nivel, Short numeroGrado) {
    if (nivel == null || numeroGrado == null) throw configuracionInvalida();
    int grado = numeroGrado;
    int minima = switch (nivel.strip().toUpperCase(Locale.ROOT)) {
      // En el catálogo, Inicial 1/2/3 corresponde a 3/4/5 años.
      case "INICIAL" -> edadMinima(grado, 3, 2);
      case "PRIMARIA" -> edadMinima(grado, 6, 5);
      case "SECUNDARIA" -> edadMinima(grado, 5, 11);
      default -> throw configuracionInvalida();
    };
    return new Rango(minima, minima + 2);
  }

  private static int edadMinima(int grado, int maximoGrado, int incremento) {
    if (grado < 1 || grado > maximoGrado) throw configuracionInvalida();
    return grado + incremento;
  }

  private static IllegalArgumentException configuracionInvalida() {
    return new IllegalArgumentException("El nivel o grado no tiene configurada una edad de admisión EBR");
  }

  public record Rango(int minima, int maxima) {
    public boolean admite(LocalDate nacimiento, int anioLectivo) {
      LocalDate corte = LocalDate.of(anioLectivo, 3, 31);
      if (nacimiento == null || nacimiento.isAfter(corte)) return false;
      int edad = Period.between(nacimiento, corte).getYears();
      return edad >= minima && edad <= maxima;
    }
  }
}
