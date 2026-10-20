package com.colegio.shuji.tesoreria.infrastructure.controller.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class RetornoPagoControllerTest {
  @Test void elRetornoNoApruebaPagosNiReflejaParametrosDelNavegador() throws Exception {
    MockMvcBuilders.standaloneSetup(new RetornoPagoController()).build()
        .perform(get("/api/v1/pagos/retorno?status=approved&payment_id=999&external_reference=malicioso"))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Vuelve a la pestaña")))
        .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("malicioso"))))
        .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Pago aprobado"))));
  }
}
