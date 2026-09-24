package com.colegio.shuji.tesoreria.infrastructure.controller.rest;

import com.colegio.shuji.tesoreria.application.dto.in.CrearPreferenciaMercadoPagoDto;
import com.colegio.shuji.tesoreria.application.dto.in.GenerarObligacionesAnualesRequestDto;
import com.colegio.shuji.tesoreria.application.dto.in.RegistrarPagoCajaRequestDto;
import com.colegio.shuji.tesoreria.application.dto.in.RevertirPagoRequestDto;
import com.colegio.shuji.tesoreria.application.dto.out.EstadoCuentaEstudianteResponseDto;
import com.colegio.shuji.tesoreria.application.dto.out.ConceptoCobroResponseDto;
import com.colegio.shuji.tesoreria.application.dto.out.ObligacionResponseDto;
import com.colegio.shuji.tesoreria.application.dto.out.PreferenciaMercadoPagoResponseDto;
import com.colegio.shuji.tesoreria.application.dto.out.TransaccionResponseDto;
import com.colegio.shuji.tesoreria.application.port.in.GenerarCronogramaPensionesUseCase;
import com.colegio.shuji.tesoreria.application.port.in.ProcesarPagoPasarelaUseCase;
import com.colegio.shuji.tesoreria.application.port.in.RevertirPagoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tesoreria")
@Validated
@RequiredArgsConstructor
@Tag(name = "Tesoreria")
public class TesoreriaController {
  private final GenerarCronogramaPensionesUseCase cronograma;
  private final ProcesarPagoPasarelaUseCase pagos;
  private final RevertirPagoUseCase reversiones;

  @GetMapping("/conceptos")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  public List<ConceptoCobroResponseDto> listarConceptos() {
    return cronograma.listarConceptos();
  }

  @PostMapping("/obligaciones")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "cronograma.generarCronograma")
  public List<ObligacionResponseDto> generarCronograma(
      @Valid @RequestBody GenerarObligacionesAnualesRequestDto r) {
    return cronograma.generarCronograma(r);
  }

  @GetMapping("/obligaciones")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "cronograma.obligacionesMatricula")
  public List<ObligacionResponseDto> listarObligacionesMatricula(@RequestParam Long matriculaId) {
    return cronograma.obligacionesMatricula(matriculaId);
  }

  @GetMapping("/mis-obligaciones")
  @PreAuthorize("hasRole('APODERADO')")
  public List<ObligacionResponseDto> listarMisObligaciones() {
    return cronograma.misObligaciones();
  }

  @GetMapping("/estudiantes/{id}/estado-cuenta")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "cronograma.estadoCuenta")
  public EstadoCuentaEstudianteResponseDto consultarEstadoCuenta(@PathVariable @Positive Long id) {
    return cronograma.estadoCuenta(id);
  }

  @PostMapping("/pagos/caja")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "pagos.registrarCaja")
  public TransaccionResponseDto registrarPagoCaja(@Valid @RequestBody RegistrarPagoCajaRequestDto r) {
    return pagos.registrarCaja(r);
  }

  @PostMapping("/pagos/reversion")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "reversiones.revertir")
  public TransaccionResponseDto revertirPago(@Valid @RequestBody RevertirPagoRequestDto r) {
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
