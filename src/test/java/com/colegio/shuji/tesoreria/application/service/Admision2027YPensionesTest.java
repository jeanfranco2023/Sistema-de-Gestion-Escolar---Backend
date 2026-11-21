package com.colegio.shuji.tesoreria.application.service;

import static org.junit.jupiter.api.Assertions.*;

import com.colegio.shuji.matricula.domain.service.PoliticaAdmision;
import com.colegio.shuji.tesoreria.application.dto.out.ObligacionResponseDto;
import com.colegio.shuji.tesoreria.domain.enums.EstadoObligacion;
import com.colegio.shuji.tesoreria.domain.enums.TipoConcepto;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class Admision2027YPensionesTest {
  @Test
  void desdeElLunesNueveSeAdmiteElSiguienteAnioLectivo() {
    LocalDate corte = LocalDate.of(2026, 11, 9);
    assertTrue(PoliticaAdmision.admiteNuevasMatriculas((short) 2026, corte.minusDays(1)));
    assertFalse(PoliticaAdmision.admiteNuevasMatriculas((short) 2026, corte));
    assertTrue(PoliticaAdmision.admiteNuevasMatriculas((short) 2027, corte));
  }

  @Test
  void laPensionConSaldoVenceElDiaSiguienteYLaPagadaConservaSuEstado() {
    LocalDate dia20 = LocalDate.of(2027, 3, 20);
    var importe = new BigDecimal("120.00");
    var pendiente = cuota(dia20, EstadoObligacion.PENDIENTE, importe);
    var parcial = cuota(dia20, EstadoObligacion.PAGADO_PARCIAL, BigDecimal.ONE);
    var pagada = cuota(dia20, EstadoObligacion.PAGADO_TOTAL, BigDecimal.ZERO);

    assertEquals(EstadoObligacion.PENDIENTE, TesoreriaService.estadoParaConsulta(pendiente, dia20));
    assertEquals(EstadoObligacion.VENCIDO, TesoreriaService.estadoParaConsulta(pendiente, dia20.plusDays(1)));
    assertEquals(EstadoObligacion.VENCIDO, TesoreriaService.estadoParaConsulta(parcial, dia20.plusDays(1)));
    assertEquals(EstadoObligacion.PAGADO_TOTAL, TesoreriaService.estadoParaConsulta(pagada, dia20.plusDays(1)));
  }

  private static ObligacionResponseDto cuota(LocalDate vencimiento, EstadoObligacion estado, BigDecimal saldo) {
    var importe = new BigDecimal("120.00");
    return new ObligacionResponseDto(1L, 1L, (short) 1, TipoConcepto.PENSION, (short) 1,
        "Marzo 2027", vencimiento, importe, BigDecimal.ZERO, BigDecimal.ZERO,
        importe.subtract(saldo), saldo, estado, "Estudiante QA");
  }
}
