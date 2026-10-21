package com.colegio.shuji.tesoreria.infrastructure.adapter;

import static org.junit.jupiter.api.Assertions.*;

import com.colegio.shuji.shared.domain.exception.BusinessException;
import com.colegio.shuji.tesoreria.domain.enums.MetodoPago;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MercadoPagoHttpAdapterTest {
  final ObjectMapper json = new ObjectMapper();
  HttpServer server;
  MercadoPagoHttpAdapter adapter;
  boolean testUser = true;
  String pago;
  String preferenciaEnviada;

  @BeforeEach void iniciar() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", exchange -> {
      String path = exchange.getRequestURI().getPath();
      String body = switch (path) {
        case "/users/me" -> "{\"id\":77,\"country_id\":\"PE\",\"tags\":" + (testUser ? "[\"test_user\"]" : "[]") + "}";
        case "/checkout/preferences" -> {
          preferenciaEnviada = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
          yield "{\"id\":\"pref-test\",\"init_point\":\"https://www.mercadopago.com.pe/checkout/test\",\"sandbox_init_point\":\"https://sandbox.mercadopago.com.pe/checkout/test\"}";
        }
        case "/v1/payments/search" -> "{\"results\":[{\"id\":123,\"external_reference\":\"42\"}]}";
        default -> pago;
      };
      byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type", "application/json");
      exchange.sendResponseHeaders(200, bytes.length);
      try (var output = exchange.getResponseBody()) { output.write(bytes); }
    });
    server.start();
    adapter = new MercadoPagoHttpAdapter("http://127.0.0.1:" + server.getAddress().getPort(),
        "APP_USR-cuenta-vendedora-de-prueba", "public-key", "colegio.edu.pe", "");
    pago = respuesta("yape", "debit_card", "false", "PEN", "350.00");
  }

  @AfterEach void detener() { server.stop(0); }

  @Test void aceptaCuentaTestConTokenAppUsrYNoExcluyeMedios() throws Exception {
    ReflectionTestUtils.setField(adapter, "notificationUrl", "https://colegio.edu.pe/api/v1/pagos/webhook/mercadopago");
    var pref = adapter.crearPreferenciaMatriculaPublica("42", new BigDecimal("350"), "Matrícula");
    assertTrue(pref.initPoint().contains("www.mercadopago.com.pe"));
    var enviado = json.readTree(preferenciaEnviada);
    assertEquals(0, new BigDecimal("350").compareTo(enviado.get("items").get(0).get("unit_price").decimalValue()));
    assertFalse(enviado.has("payment_methods"));
    assertTrue(enviado.has("notification_url"));
  }

  @Test void sandboxNoCreaCheckoutConCuentaReal() {
    testUser = false;
    assertThrows(BusinessException.class, () -> adapter.crearPreferenciaMatriculaPublica("42", new BigDecimal("350"), "Matrícula"));
    assertNull(preferenciaEnviada);
  }

  @Test void mapeaYapeYTarjetaDebitoPorElTipoReal() {
    assertEquals(MetodoPago.YAPE, adapter.consultarPago("123").metodo());
    pago = respuesta("visa", "debit_card", "false", "PEN", "350.00");
    assertEquals(MetodoPago.TARJETA_DEBITO, adapter.consultarPago("123").metodo());
  }

  @Test void identificaTransferenciaYEfectivoSinInventarTarjeta() {
    pago = respuesta("banco", "bank_transfer", "false", "PEN", "350.00");
    assertEquals(MetodoPago.TRANSFERENCIA, adapter.consultarPago("123").metodo());
    pago = respuesta("pagoefectivo_atm", "atm", "false", "PEN", "350.00");
    assertEquals(MetodoPago.PAGO_EFECTIVO, adapter.consultarPago("123").metodo());
  }

  @Test void aceptaTestAppUsrConLiveModeTrueSoloDelVendedorVerificado() {
    pago = respuesta("yape", "debit_card", "true", "PEN", "350.00");
    assertTrue(adapter.consultarPago("123").aprobado());
    pago = pago.replace("\"collector_id\":77", "\"collector_id\":999");
    assertThrows(BusinessException.class, () -> adapter.consultarPago("123"));
  }

  @Test void cuentaRealNoVerificaPagosEnSandboxAunqueDeclarenLiveModeFalse() {
    testUser = false;
    assertThrows(BusinessException.class, () -> adapter.consultarPago("123"));
  }

  @Test void tokenLegacyTestExigeLiveModeFalse() {
    adapter = new MercadoPagoHttpAdapter("http://127.0.0.1:" + server.getAddress().getPort(),
        "TEST-token", "public-key", "colegio.edu.pe", "");
    pago = respuesta("visa", "credit_card", "true", "PEN", "350.00");
    assertThrows(BusinessException.class, () -> adapter.consultarPago("123"));
    pago = respuesta("visa", "credit_card", "false", "PEN", "350.00");
    assertTrue(adapter.consultarPago("123").aprobado());
  }

  @Test void produccionExigeCuentaRealYLiveModeTrue() {
    ReflectionTestUtils.setField(adapter, "sandbox", false);
    testUser = false;
    assertThrows(BusinessException.class, () -> adapter.consultarPago("123"));
    pago = respuesta("visa", "credit_card", "true", "PEN", "350.00");
    assertTrue(adapter.consultarPago("123").aprobado());
  }

  @Test void consultaReferenciaYRevalidaElPagoPorId() {
    var confirmado = adapter.buscarPagoAprobado("42").orElseThrow();
    assertEquals("123", confirmado.transaccionId());
    assertEquals("42", confirmado.referenciaExterna());
    assertTrue(adapter.buscarPagoAprobado("otra-referencia").isEmpty());
  }

  @Test void rechazaIdDistintoMonedaOImporteIncompletos() {
    pago = respuesta("yape", "debit_card", "false", "PEN", "350.00").replace("\"id\":123", "\"id\":999");
    assertThrows(BusinessException.class, () -> adapter.consultarPago("123"));
    pago = respuesta("yape", "debit_card", "false", "PEN", "350.001");
    assertThrows(BusinessException.class, () -> adapter.consultarPago("123"));
  }

  private String respuesta(String metodo, String tipo, String live, String moneda, String monto) {
    return "{\"id\":123,\"collector_id\":77,\"external_reference\":\"42\",\"transaction_amount\":" + monto
        + ",\"currency_id\":\"" + moneda + "\",\"payment_method_id\":\"" + metodo
        + "\",\"payment_type_id\":\"" + tipo + "\",\"status\":\"approved\",\"live_mode\":" + live + "}";
  }
}
