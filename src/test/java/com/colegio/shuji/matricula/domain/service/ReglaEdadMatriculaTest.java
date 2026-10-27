package com.colegio.shuji.matricula.domain.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ReglaEdadMatriculaTest {
  @Test
  void usaElCorteDeMarzoDelAnioLectivoYAdmiteHastaDosAniosDeExtraedad() {
    var rango = ReglaEdadMatricula.paraGrado("Primaria", (short) 1);
    assertEquals(new ReglaEdadMatricula.Rango(6, 8), rango);
    assertTrue(rango.admite(LocalDate.of(2020, 3, 31), 2026));
    assertFalse(rango.admite(LocalDate.of(2020, 4, 1), 2026));
    assertTrue(rango.admite(LocalDate.of(2018, 3, 31), 2026));
    assertFalse(rango.admite(LocalDate.of(2017, 3, 31), 2026));
    assertTrue(rango.admite(LocalDate.of(2020, 4, 1), 2027));
  }

  @Test
  void inicialUsaNumeroDeGradoInternoYNoElIdNiElNombreLibre() {
    for (short grado = 1; grado <= 3; grado++) {
      var rango = ReglaEdadMatricula.paraGrado("INICIAL", grado);
      assertEquals(grado + 2, rango.minima());
      assertEquals(grado + 4, rango.maxima());
    }
    assertEquals(new ReglaEdadMatricula.Rango(16, 18),
        ReglaEdadMatricula.paraGrado("Secundaria", (short) 5));
  }

  @Test
  void rechazaFechasAusentesOPosterioresAlCorteYCatalogosInvalidos() {
    var rango = ReglaEdadMatricula.paraGrado("Primaria", (short) 1);
    assertFalse(rango.admite(null, 2026));
    assertFalse(rango.admite(LocalDate.of(2026, 4, 1), 2026));
    assertThrows(IllegalArgumentException.class, () -> ReglaEdadMatricula.paraGrado(null, (short) 1));
    assertThrows(IllegalArgumentException.class, () -> ReglaEdadMatricula.paraGrado("Inicial", (short) 4));
    assertThrows(IllegalArgumentException.class, () -> ReglaEdadMatricula.paraGrado("Primaria", (short) 7));
    assertThrows(IllegalArgumentException.class, () -> ReglaEdadMatricula.paraGrado("Secundaria", (short) 0));
  }
}
