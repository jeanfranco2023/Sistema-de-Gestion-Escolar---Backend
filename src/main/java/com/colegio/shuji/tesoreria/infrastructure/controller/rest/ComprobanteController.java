package com.colegio.shuji.tesoreria.infrastructure.controller.rest;

import com.colegio.shuji.tesoreria.application.dto.in.EmitirComprobanteRequestDto;
import com.colegio.shuji.tesoreria.application.dto.out.ComprobanteResponseDto;
import com.colegio.shuji.tesoreria.application.port.in.EmitirComprobanteUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/comprobantes")
@Validated
@RequiredArgsConstructor
@Tag(name = "Tesoreria")
public class ComprobanteController {
  private final EmitirComprobanteUseCase comprobantes;

  @PostMapping("")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "comprobantes.emitir")
  public ComprobanteResponseDto emitirComprobante(@Valid @RequestBody EmitirComprobanteRequestDto r) {
    return comprobantes.emitir(r);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "comprobantes.consultar")
  public ComprobanteResponseDto consultarComprobante(@PathVariable @Positive Long id) {
    return comprobantes.consultar(id);
  }
}
