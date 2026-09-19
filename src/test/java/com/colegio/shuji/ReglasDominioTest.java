package com.colegio.shuji;

import static org.junit.jupiter.api.Assertions.*;

import com.colegio.shuji.academico.domain.exception.CupoAgotadoException;
import com.colegio.shuji.academico.domain.model.Seccion;
import com.colegio.shuji.asistencia.domain.enums.EstadoAsistenciaAula;
import com.colegio.shuji.asistencia.domain.model.AsistenciaAula;
import com.colegio.shuji.comunicado.domain.model.ComunicadoDestinatario;
import com.colegio.shuji.convivencia.domain.enums.EstadoIncidencia;
import com.colegio.shuji.convivencia.domain.model.IncidenciaConductual;
import com.colegio.shuji.matricula.domain.enums.EstadoMatricula;
import com.colegio.shuji.matricula.domain.exception.ReservaExpiradaException;
import com.colegio.shuji.matricula.domain.model.Matricula;
import com.colegio.shuji.shared.domain.exception.BusinessException;
import com.colegio.shuji.tesoreria.domain.enums.*;
import com.colegio.shuji.tesoreria.domain.exception.SobrepagoException;
import com.colegio.shuji.tesoreria.domain.model.ObligacionPago;
import com.colegio.shuji.tesoreria.domain.model.PagoTransaccion;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class ReglasDominioTest {
  @Test
  void noPermiteExcederCupo() {
    var s = new Seccion();
    s.setCupoMaximo((short) 20);
    s.setVacantesOcupadas((short) 20);
    assertThrows(CupoAgotadoException.class, s::verificarCupo);
  }

  @Test
  void reservaExpiradaNoSeConfirma() {
    var m = new Matricula();
    m.setEstadoMatricula(EstadoMatricula.RESERVADA_TEMPORAL);
    var ahora = OffsetDateTime.now();
    m.setReservaExpiraAt(ahora);
    assertThrows(ReservaExpiradaException.class, () -> m.confirmar(ahora));
    assertTrue(m.liberarSiVencida(ahora));
    assertFalse(m.liberarSiVencida(ahora));
    assertEquals(EstadoMatricula.CANCELADA, m.getEstadoMatricula());
  }

  @Test
  void confirmacionEsIdempotente() {
    var m = new Matricula();
    m.setEstadoMatricula(EstadoMatricula.SOLICITADA);
    m.confirmar(OffsetDateTime.now());
    m.confirmar(OffsetDateTime.now());
    assertEquals(EstadoMatricula.MATRICULADO, m.getEstadoMatricula());
  }

  @Test
  void saldoUsaDecimalesYRechazaSobrepago() {
    var o = new ObligacionPago();
    o.setMontoBase(new BigDecimal("380.00"));
    o.setMontoMora(new BigDecimal("0.10"));
    o.setMontoDescuento(new BigDecimal("20.00"));
    o.setTotalPagado(new BigDecimal("100.10"));
    o.setEstado(EstadoObligacion.PAGADO_PARCIAL);
    assertEquals(new BigDecimal("260.00"), o.saldo());
    o.validarPago(new BigDecimal("260.00"));
    assertThrows(SobrepagoException.class, () -> o.validarPago(new BigDecimal("260.01")));
  }

  @Test
  void reversionEsTerminal() {
    var p = new PagoTransaccion();
    p.setEstadoPago(EstadoPago.APROBADO);
    p.revertir();
    assertThrows(BusinessException.class, p::revertir);
  }

  @Test
  void justificacionRequiereFaltaYMotivo() {
    var a = new AsistenciaAula();
    a.setEstado(EstadoAsistenciaAula.PRESENTE);
    assertThrows(BusinessException.class, () -> a.justificar("Enfermedad", null));
    a.setEstado(EstadoAsistenciaAula.FALTA_INJUSTIFICADA);
    a.justificar("Enfermedad", null);
    assertTrue(a.getJustificada());
    assertEquals(EstadoAsistenciaAula.FALTA_JUSTIFICADA, a.getEstado());
  }

  @Test
  void incidenciaCerradaNoSeReabre() {
    var i = new IncidenciaConductual();
    i.setEstado(EstadoIncidencia.ABIERTA);
    assertThrows(BusinessException.class, () -> i.cambiarEstado(EstadoIncidencia.CERRADA));
    i.cambiarEstado(EstadoIncidencia.ATENDIDA);
    i.cambiarEstado(EstadoIncidencia.CERRADA);
    assertThrows(BusinessException.class, () -> i.cambiarEstado(EstadoIncidencia.ABIERTA));
  }

  @Test
  void acuseConservaPrimeraFecha() {
    var d = new ComunicadoDestinatario();
    var primero = OffsetDateTime.now();
    d.confirmarLectura(primero, true);
    d.confirmarLectura(primero.plusHours(1), true);
    assertEquals(primero, d.getFechaLectura());
    assertEquals(primero, d.getFechaAcuse());
  }
}
