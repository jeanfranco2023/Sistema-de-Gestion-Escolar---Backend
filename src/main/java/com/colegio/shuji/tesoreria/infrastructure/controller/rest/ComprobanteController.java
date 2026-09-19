package com.colegio.shuji.tesoreria.infrastructure.controller.rest;

import com.colegio.shuji.tesoreria.application.dto.in.*;
import com.colegio.shuji.tesoreria.application.dto.out.*;
import com.colegio.shuji.tesoreria.application.port.in.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
  public ComprobanteResponseDto operacion0(@Valid @RequestBody EmitirComprobanteRequestDto r) {
    return comprobantes.emitir(r);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "comprobantes.consultar")
  public ComprobanteResponseDto operacion1(@PathVariable @Positive Long id) {
    return comprobantes.consultar(id);
  }
}
