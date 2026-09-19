package com.colegio.shuji;

import static org.junit.jupiter.api.Assertions.*;

import com.colegio.shuji.academico.domain.model.AnioLectivo;
import com.colegio.shuji.asistencia.domain.enums.EstadoAsistenciaAula;
import com.colegio.shuji.asistencia.domain.enums.TipoDiscrepancia;
import com.colegio.shuji.asistencia.domain.model.AsistenciaAula;
import com.colegio.shuji.asistencia.domain.model.ConciliacionAsistencia;
import com.colegio.shuji.asistencia.domain.model.LoteBiometrico;
import com.colegio.shuji.evaluacion.domain.enums.EstadoAsistenciaRefuerzo;
import com.colegio.shuji.evaluacion.domain.model.InscripcionRefuerzo;
import com.colegio.shuji.matricula.domain.enums.OrigenRegistro;
import com.colegio.shuji.matricula.domain.enums.TipoDocumento;
import com.colegio.shuji.matricula.domain.model.Estudiante;
import com.colegio.shuji.shared.domain.exception.BusinessException;
import com.colegio.shuji.tesoreria.domain.enums.EstadoPago;
import com.colegio.shuji.tesoreria.domain.enums.TipoComprobante;
import com.colegio.shuji.tesoreria.domain.model.ComprobantePago;
import com.colegio.shuji.tesoreria.domain.model.PagoTransaccion;
import com.colegio.shuji.usuario.domain.model.Sesion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class DominioAuditTest {
  @Test
  void pagoRevertidoNoPuedeAprobarseNiRechazarse() {
    var pago =
        PagoTransaccion.builder()
            .estadoPago(EstadoPago.REVERTIDO)
            .montoPagado(BigDecimal.TEN)
            .build();
    assertThrows(BusinessException.class, pago::aprobar);
    assertThrows(BusinessException.class, pago::rechazar);
    assertEquals(EstadoPago.REVERTIDO, pago.getEstadoPago());
  }

  @Test
  void pagoAprobadoExigeReversionTrazable() {
    var pago =
        PagoTransaccion.builder()
            .estadoPago(EstadoPago.APROBADO)
            .montoPagado(BigDecimal.TEN)
            .build();
    assertThrows(BusinessException.class, pago::rechazar);
    assertThrows(BusinessException.class, () -> pago.revertir(" "));
    pago.revertir("Cobro incorrecto");
    assertEquals("Cobro incorrecto", pago.getPayloadWebhook().get("motivoReversion"));
  }

  @Test
  void emisionBoletaExigePagoAprobadoYSerieValida() {
    var pagoRechazado =
        PagoTransaccion.builder()
            .id(1L)
            .estadoPago(EstadoPago.RECHAZADO)
            .montoPagado(BigDecimal.valueOf(450.00))
            .build();
    assertThrows(
        BusinessException.class,
        () -> ComprobantePago.emitir(pagoRechazado, TipoComprobante.BOLETA, "B001", 1));

    var pagoAprobado =
        PagoTransaccion.builder()
            .id(1L)
            .estadoPago(EstadoPago.APROBADO)
            .montoPagado(BigDecimal.valueOf(450.00))
            .build();
    assertThrows(
        BusinessException.class,
        () -> ComprobantePago.emitir(pagoAprobado, TipoComprobante.BOLETA, "SERIE_INVALIDA", 1));
    assertThrows(
        BusinessException.class,
        () -> ComprobantePago.emitir(pagoAprobado, TipoComprobante.BOLETA, "B001", 0));

    var comprobante =
        ComprobantePago.emitir(pagoAprobado, TipoComprobante.BOLETA, "B001", 1);
    assertNotNull(comprobante);
    assertEquals("B001-00000001", comprobante.numeroCompleto());
    assertEquals(BigDecimal.valueOf(450.00), comprobante.getMontoTotal());
    assertFalse(comprobante.estaAnulado());
  }

  @Test
  void justificacionNoSePuedeCrearSinMotivo() {
    var asistencia =
        AsistenciaAula.builder().estado(EstadoAsistenciaAula.FALTA_INJUSTIFICADA).build();
    assertThrows(BusinessException.class, () -> asistencia.justificar(" ", null));
    assertThrows(
        BusinessException.class,
        () -> asistencia.registrarEstado(EstadoAsistenciaAula.FALTA_JUSTIFICADA));
    assertEquals(EstadoAsistenciaAula.FALTA_INJUSTIFICADA, asistencia.getEstado());
  }

  @Test
  void refuerzoJustificadoRequiereMotivo() {
    var inscripcion =
        InscripcionRefuerzo.builder().estadoAsistencia(EstadoAsistenciaRefuerzo.PENDIENTE).build();
    assertThrows(
        BusinessException.class,
        () -> inscripcion.registrarAsistencia(EstadoAsistenciaRefuerzo.JUSTIFICADO, null));
    inscripcion.justificar("Atención médica");
    assertEquals(EstadoAsistenciaRefuerzo.JUSTIFICADO, inscripcion.getEstadoAsistencia());
  }

  @Test
  void loteNoAceptaTotalesInconsistentes() {
    var lote = LoteBiometrico.builder().totalFilas(3).build();
    assertThrows(BusinessException.class, () -> lote.registrarResultado(3, 1));
    lote.registrarResultado(2, 1);
    assertEquals(2, lote.getMarcasValidas());
  }

  @Test
  void cierreConciliacionNoRetrocedeYRecalculoInvalidaNotificacion() {
    var c =
        ConciliacionAsistencia.builder().fecha(LocalDate.of(2026, 9, 18)).estudianteId(1L).build();
    c.conciliar(true, false, true);
    assertEquals(TipoDiscrepancia.DISCREPANCIA_FUGA, c.getTipoDiscrepancia());
    c.resolver();
    c.conciliar(true, true, false);
    assertEquals(TipoDiscrepancia.DISCREPANCIA_FUGA, c.getTipoDiscrepancia());
    c.conciliar(true, true, true);
    assertEquals(TipoDiscrepancia.ASISTENCIA_CONCILIADA, c.getTipoDiscrepancia());
    assertFalse(c.estaNotificada());
  }

  @Test
  void identidadRechazaRespuestaDeOtroDocumento() {
    var e =
        Estudiante.builder().tipoDocumento(TipoDocumento.DNI).numeroDocumento("12345678").build();
    e.iniciarRegistroManual();
    assertThrows(
        BusinessException.class, () -> e.verificarIdentidad("87654321", "Ana", "Perez", "Lopez"));
    assertFalse(e.getValidadoReniec());
    e.verificarIdentidad("12345678", "Ana", "Perez", "Lopez");
    assertTrue(e.getValidadoReniec());
    assertEquals(OrigenRegistro.RENIEC_API, e.getOrigenRegistro());
  }

  @Test
  void sesionExpiraExactamenteEnSuFechaLimite() {
    var limite = LocalDateTime.of(2026, 9, 18, 10, 0);
    var sesion = Sesion.builder().expiraAt(limite).build();
    assertTrue(sesion.estaExpirada(limite));
    assertFalse(sesion.estaExpirada(limite.minusNanos(1)));
    assertThrows(
        BusinessException.class,
        () -> sesion.renovar("nuevo", null, null, limite, limite.plusDays(1)));
  }

  @Test
  void anioInvalidoNoSeAbre() {
    var anio =
        AnioLectivo.builder()
            .fechaInicio(LocalDate.of(2026, 12, 31))
            .fechaFin(LocalDate.of(2026, 1, 1))
            .build();
    assertThrows(BusinessException.class, anio::abrir);
    assertFalse(anio.estaAbierto());
  }
}
