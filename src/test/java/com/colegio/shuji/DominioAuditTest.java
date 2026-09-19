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
  void serieComprobanteIncrementaCorrelativoSecuencialmente() {
    var serie =
        com.colegio.shuji.tesoreria.domain.model.SerieComprobante.builder()
            .serie("B001")
            .ultimoCorrelativo(0)
            .updatedAt(java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC))
            .build();
    assertEquals(1, serie.siguienteCorrelativo());
    assertEquals(2, serie.siguienteCorrelativo());
    assertEquals(3, serie.siguienteCorrelativo());
    assertEquals(3, serie.getUltimoCorrelativo());
    assertNotNull(serie.getUpdatedAt());
  }

  @Test
  void validacionFirmaHmacYFrescuraTsProtegeContraReplay() throws Exception {
    String secret = "clave_secreta_test";
    String dataId = "123456";
    String requestId = "req-test-789";
    long now = java.time.Instant.now().getEpochSecond();
    String tsValido = String.valueOf(now);
    String tsExpirado = String.valueOf(now - 600); // 10 minutos atrás

    // Manifest: id:123456;request-id:req-test-789;ts:now;
    String manifest = "id:" + dataId + ";request-id:" + requestId + ";ts:" + tsValido + ";";
    var mac = javax.crypto.Mac.getInstance("HmacSHA256");
    mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
    var rawBytes = mac.doFinal(manifest.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    var sb = new StringBuilder();
    for (byte b : rawBytes) sb.append(String.format("%02x", b));
    String firmaCorrecta = sb.toString();

    var clock =
        java.time.Clock.fixed(
            java.time.Instant.ofEpochSecond(now), java.time.ZoneOffset.UTC);
    var validator =
        new com.colegio.shuji.tesoreria.infrastructure.security
            .MercadoPagoWebhookSignatureValidator(secret, clock);
    assertTrue(validator.validar("ts=" + tsValido + ",v1=" + firmaCorrecta, requestId, dataId));
    assertFalse(
        validator.validar(
            "ts=" + tsValido + ",v1=" + "0".repeat(64), requestId, dataId));

    String manifestExpirado =
        "id:" + dataId + ";request-id:" + requestId + ";ts:" + tsExpirado + ";";
    mac.init(
        new javax.crypto.spec.SecretKeySpec(
            secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
    String firmaExpirada = java.util.HexFormat.of().formatHex(
        mac.doFinal(manifestExpirado.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    assertFalse(
        validator.validar("ts=" + tsExpirado + ",v1=" + firmaExpirada, requestId, dataId));
  }

  @Test
  void vinculoApoderadoExigeResponsabilidadEconomicaParaGestionPago() {
    var vinculoNoEconomico =
        com.colegio.shuji.matricula.domain.model.EstudianteApoderado.builder()
            .estudianteId(10L)
            .apoderadoId(20L)
            .tieneCustodia(true)
            .esResponsableEconomico(false)
            .build();
    assertFalse(vinculoNoEconomico.getEsResponsableEconomico());

    var vinculoEconomico =
        com.colegio.shuji.matricula.domain.model.EstudianteApoderado.builder()
            .estudianteId(10L)
            .apoderadoId(20L)
            .tieneCustodia(true)
            .esResponsableEconomico(true)
            .build();
    assertTrue(vinculoEconomico.getEsResponsableEconomico());
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
