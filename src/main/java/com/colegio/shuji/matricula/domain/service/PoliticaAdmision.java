package com.colegio.shuji.matricula.domain.service;

import java.time.LocalDate;
import java.time.ZoneId;

public final class PoliticaAdmision {
  private static final LocalDate INICIO_ADMISION_2027 = LocalDate.of(2026, 11, 9);
  private static final ZoneId LIMA = ZoneId.of("America/Lima");

  private PoliticaAdmision() {}

  public static boolean admiteNuevasMatriculas(Short anio) {
    return admiteNuevasMatriculas(anio, LocalDate.now(LIMA));
  }

  public static boolean admiteNuevasMatriculas(Short anio, LocalDate fecha) {
    return anio != null && (fecha.isBefore(INICIO_ADMISION_2027)
        || anio >= Math.max(2027, fecha.getYear()));
  }

  public static String mensajeAnioNoDisponible() {
    return "Las nuevas matrículas corresponden al año lectivo "
        + Math.max(2027, LocalDate.now(LIMA).getYear()) + " o posterior.";
  }
}
