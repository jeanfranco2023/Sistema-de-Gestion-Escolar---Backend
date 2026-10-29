package com.colegio.shuji.tesoreria.infrastructure.controller.rest;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RetornoPagoController {
  @GetMapping(value = "/api/v1/pagos/retorno", produces = MediaType.TEXT_HTML_VALUE)
  @PreAuthorize("permitAll()")
  public ResponseEntity<String> retorno() {
    // Los parámetros del navegador nunca acreditan un pago.
    return ResponseEntity.ok().header("Cache-Control", "no-store")
        .header("Referrer-Policy", "no-referrer")
        .header("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; base-uri 'none'")
        .body("""
            <!doctype html><html lang="es"><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>Confirmación del pago — Colegio Shuji Kitamura</title></head>
            <body><main><h1>Verificación del pago</h1>
            <p>Vuelve a la pestaña del colegio para consultar el resultado de tu operación.</p>
            <p>El sistema confirma el abono directamente con Mercado Pago. Si elegiste PagoEfectivo,
            el resultado se actualizará después de pagar el CIP y recibir la acreditación.</p>
            <p>Puedes cerrar esta pestaña. Conserva tu constancia hasta ver el pago confirmado.</p>
            </main></body></html>
            """);
  }
}
