package com.colegio.shuji.tesoreria.infrastructure.controller.rest;

import com.colegio.shuji.tesoreria.application.dto.in.*;
import com.colegio.shuji.tesoreria.application.dto.out.*;
import com.colegio.shuji.tesoreria.application.port.in.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tesoreria")
@Validated
@RequiredArgsConstructor
@Tag(name = "Tesoreria")
public class TesoreriaController {
  private final GenerarCronogramaPensionesUseCase cronograma;
  private final ProcesarPagoPasarelaUseCase pagos;
  private final RevertirPagoUseCase reversiones;

  @PostMapping("/obligaciones")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "cronograma.generarCronograma")
  public List<ObligacionResponseDto> operacion0(
      @Valid @RequestBody GenerarObligacionesAnualesRequestDto r) {
    return cronograma.generarCronograma(r);
  }

  @GetMapping("/obligaciones")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "cronograma.obligacionesMatricula")
  public List<ObligacionResponseDto> operacion1(@RequestParam Long matriculaId) {
    return cronograma.obligacionesMatricula(matriculaId);
  }

  @GetMapping("/estudiantes/{id}/estado-cuenta")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "cronograma.estadoCuenta")
  public EstadoCuentaEstudianteResponseDto operacion2(@PathVariable @Positive Long id) {
    return cronograma.estadoCuenta(id);
  }

  @PostMapping("/pagos/caja")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "pagos.registrarCaja")
  public TransaccionResponseDto operacion3(@Valid @RequestBody RegistrarPagoCajaRequestDto r) {
    return pagos.registrarCaja(r);
  }

  @PostMapping("/pagos/reversion")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "reversiones.revertir")
  public TransaccionResponseDto operacion4(@Valid @RequestBody RevertirPagoRequestDto r) {
    return reversiones.revertir(r);
  }

  @PostMapping("/pagos/mercadopago/preferencia")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','APODERADO')")
  @Operation(summary = "Crear preferencia de pago en Mercado Pago")
  public PreferenciaMercadoPagoResponseDto crearPreferenciaMercadoPago(
      @Valid @RequestBody CrearPreferenciaMercadoPagoDto r) {
    return pagos.crearPreferenciaMercadoPago(r);
  }
}
