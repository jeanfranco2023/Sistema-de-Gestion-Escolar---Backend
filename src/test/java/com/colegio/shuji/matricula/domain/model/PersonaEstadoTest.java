package com.colegio.shuji.matricula.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.colegio.shuji.shared.domain.exception.BusinessException;
import org.junit.jupiter.api.Test;

class PersonaEstadoTest {

  @Test
  void estadoDelEstudianteMantieneCoherenteElIndicadorActivo() {
    var estudiante = Estudiante.builder().build();

    estudiante.cambiarEstado("ACTIVO");
    assertTrue(estudiante.getActivo());

    estudiante.cambiarEstado("RETIRADO");
    assertEquals("RETIRADO", estudiante.getEstado());
    assertFalse(estudiante.getActivo());
  }

  @Test
  void estadoDelApoderadoMantieneCoherenteElIndicadorActivo() {
    var apoderado = Apoderado.builder().build();

    apoderado.cambiarEstado("ACTIVO");
    assertTrue(apoderado.getActivo());

    apoderado.cambiarEstado("SUSPENDIDO");
    assertEquals("SUSPENDIDO", apoderado.getEstado());
    assertFalse(apoderado.getActivo());
  }

  @Test
  void modelosRechazanEstadosFueraDelCatalogo() {
    assertThrows(BusinessException.class, () -> Estudiante.builder().build().cambiarEstado("OTRO"));
    assertThrows(BusinessException.class, () -> Apoderado.builder().build().cambiarEstado("OTRO"));
  }
}
