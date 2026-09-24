package com.colegio.shuji;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.colegio.shuji.tesoreria.application.port.in.ProcesarPagoPasarelaUseCase;
import com.colegio.shuji.tesoreria.infrastructure.controller.rest.WebhookController;
import com.colegio.shuji.tesoreria.infrastructure.security.MercadoPagoWebhookSignatureValidator;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MercadoPagoWebhookControllerTest {
  private final ProcesarPagoPasarelaUseCase pagos = mock(ProcesarPagoPasarelaUseCase.class);

  @Test
  void rechazaNotificacionesSiNoHaySecretoConfigurado() {
    var validator = new MercadoPagoWebhookSignatureValidator("", "", Optional.empty());
    var controller = new WebhookController(pagos, validator);

    var response = controller.webhookMercadoPago("123", null, null, "payment", null,
        null, null, Map.of("type", "payment"));

    assertEquals(503, response.getStatusCode().value());
    verifyNoInteractions(pagos);
  }

  @Test
  void rechazaNotificacionesSinFirmaValida() {
    var validator = new MercadoPagoWebhookSignatureValidator("secreto-de-prueba", "", Optional.empty());
    var controller = new WebhookController(pagos, validator);

    var response = controller.webhookMercadoPago("123", null, null, "payment", null,
        null, null, Map.of("type", "payment"));

    assertEquals(401, response.getStatusCode().value());
    verifyNoInteractions(pagos);
  }
}
