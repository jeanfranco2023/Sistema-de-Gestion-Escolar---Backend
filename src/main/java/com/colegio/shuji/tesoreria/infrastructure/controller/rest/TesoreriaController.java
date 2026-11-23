package com.colegio.shuji.tesoreria.infrastructure.controller.rest;

import com.colegio.shuji.tesoreria.application.dto.in.CrearPreferenciaMercadoPagoDto;
import com.colegio.shuji.tesoreria.application.dto.in.GenerarObligacionesAnualesRequestDto;
import com.colegio.shuji.tesoreria.application.dto.in.GenerarObligacionMatriculaRequestDto;
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
import com.colegio.shuji.tesoreria.application.dto.in.CobroExtraordinarioRequest;
import com.colegio.shuji.tesoreria.application.service.TesoreriaService;
import com.colegio.shuji.tesoreria.infrastructure.adapter.ComprobanteMatriculaPdfAdapter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
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

  private final TesoreriaService extraordinarios;
  private final ComprobanteMatriculaPdfAdapter pdf;

  @GetMapping("/extraordinarios/conceptos")
  @PreAuthorize("hasAnyRole('DIRECCION','ADMINISTRADOR','SECRETARIA','AUXILIAR')")
  public List<ConceptoCobroResponseDto> conceptosExtraordinarios() {
    return extraordinarios.conceptosExtraordinarios();
  }

  @PostMapping("/extraordinarios/conceptos")
  @PreAuthorize("hasAnyRole('DIRECCION','ADMINISTRADOR','SECRETARIA')")
  public ConceptoCobroResponseDto crearConcepto(@Valid @RequestBody CobroExtraordinarioRequest.Concepto r) {
    return extraordinarios.guardarConceptoExtraordinario(null, r);
  }

  @PutMapping("/extraordinarios/conceptos/{id}")
  @PreAuthorize("hasAnyRole('DIRECCION','ADMINISTRADOR','SECRETARIA')")
  public ConceptoCobroResponseDto actualizarConcepto(@PathVariable @Positive Short id,
      @Valid @RequestBody CobroExtraordinarioRequest.Concepto r) {
    return extraordinarios.guardarConceptoExtraordinario(id, r);
  }

  @PutMapping("/pensiones/configuracion")
  @PreAuthorize("hasAnyRole('DIRECCION','ADMINISTRADOR','SECRETARIA')")
  public ConceptoCobroResponseDto configurarPension(@Valid @RequestBody CobroExtraordinarioRequest.Pension r) {
    return extraordinarios.configurarPension(r);
  }

  @GetMapping("/extraordinarios/matriculas")
  @PreAuthorize("hasAnyRole('DIRECCION','ADMINISTRADOR','SECRETARIA','AUXILIAR')")
  public List<TesoreriaService.MatriculaParaCobro> matriculasParaCobro() {
    return extraordinarios.matriculasParaCobro();
  }

  @GetMapping("/extraordinarios/obligaciones")
  @PreAuthorize("hasAnyRole('AUXILIAR','ADMINISTRADOR','DIRECCION')")
  public List<ObligacionResponseDto> obligacionesExtraordinarias(
      @RequestParam(required = false) Long matriculaId) {
    return extraordinarios.obligacionesExtraordinarias(matriculaId);
  }

  @PostMapping("/extraordinarios/obligaciones")
  @PreAuthorize("hasAnyRole('DIRECCION','ADMINISTRADOR','SECRETARIA','AUXILIAR')")
  public List<ObligacionResponseDto> generarCobrosExtraordinarios(
      @Valid @RequestBody CobroExtraordinarioRequest.Asignacion r) {
    return extraordinarios.generarCobrosExtraordinarios(r);
  }

  @GetMapping(value = "/extraordinarios/obligaciones/{id}/constancia", produces = MediaType.APPLICATION_PDF_VALUE)
  @PreAuthorize("hasAnyRole('DIRECCION','ADMINISTRADOR','SECRETARIA','AUXILIAR','APODERADO')")
  public ResponseEntity<byte[]> constancia(@PathVariable @Positive Long id) {
    var datos = extraordinarios.constanciaExtraordinaria(id);
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=constancia-" + datos.numeroComprobante() + ".pdf")
        .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
        .body(pdf.generarConstanciaExtraordinaria(datos));
  }

  @GetMapping("/conceptos")
  @PreAuthorize("hasAnyRole('DIRECCION','ADMINISTRADOR','SECRETARIA')")
  public List<ConceptoCobroResponseDto> listarConceptos() {
    return cronograma.listarConceptos();
  }

  @PostMapping("/obligaciones")
  @PreAuthorize("hasAnyRole('SECRETARIA','ADMINISTRADOR')")
  @Deprecated
  @Operation(summary = "Consultar el cronograma generado automaticamente")
  public List<ObligacionResponseDto> generarCronograma(
      @Valid @RequestBody GenerarObligacionesAnualesRequestDto r) {
    return cronograma.obligacionesMatricula(r.matriculaId());
  }

  @PostMapping("/obligaciones/matricula")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','ADMINISTRADOR')")
  @Operation(summary = "tesoreria.generarObligacionUnicaMatricula")
  public ObligacionResponseDto generarObligacionMatricula(
      @Valid @RequestBody GenerarObligacionMatriculaRequestDto r) {
    return cronograma.generarObligacionMatricula(r);
  }

  @GetMapping("/obligaciones")
  @PreAuthorize("hasAnyRole('DIRECCION','ADMINISTRADOR','SECRETARIA')")
  @Operation(summary = "cronograma.obligacionesMatricula")
  public List<ObligacionResponseDto> listarObligacionesMatricula(
      @RequestParam(required = false) Long matriculaId) {
    return cronograma.obligacionesMatricula(matriculaId);
  }

  @GetMapping("/mis-obligaciones")
  @PreAuthorize("hasRole('APODERADO')")
  public List<ObligacionResponseDto> listarMisObligaciones() {
    return cronograma.misObligaciones();
  }

  @GetMapping("/estudiantes/{id}/estado-cuenta")
  @PreAuthorize("hasAnyRole('DIRECCION','ADMINISTRADOR','SECRETARIA')")
  @Operation(summary = "cronograma.estadoCuenta")
  public EstadoCuentaEstudianteResponseDto consultarEstadoCuenta(@PathVariable @Positive Long id) {
    return cronograma.estadoCuenta(id);
  }

  @PostMapping("/pagos/caja")
  @PreAuthorize("hasAnyRole('DIRECCION','ADMINISTRADOR','SECRETARIA','AUXILIAR')")
  @Operation(summary = "pagos.registrarCaja")
  public TransaccionResponseDto registrarPagoCaja(@Valid @RequestBody RegistrarPagoCajaRequestDto r) {
    return pagos.registrarCaja(r);
  }

  @PostMapping("/pagos/reversion")
  @PreAuthorize("hasAnyRole('DIRECCION','ADMINISTRADOR','SECRETARIA')")
  @Operation(summary = "reversiones.revertir")
  public TransaccionResponseDto revertirPago(@Valid @RequestBody RevertirPagoRequestDto r) {
    return reversiones.revertir(r);
  }

  @PostMapping("/pagos/mercadopago/preferencia")
  @PreAuthorize("hasAnyRole('SECRETARIA','APODERADO')")
  @Operation(summary = "Crear preferencia de pago en Mercado Pago")
  public PreferenciaMercadoPagoResponseDto crearPreferenciaMercadoPago(
      @Valid @RequestBody CrearPreferenciaMercadoPagoDto r) {
    return pagos.crearPreferenciaMercadoPago(r);
  }

  @PostMapping("/pagos/mercadopago/preferencia-adelanto")
  @PreAuthorize("hasAnyRole('SECRETARIA','APODERADO')")
  @Operation(summary = "Crear preferencia de pago adelantado en Mercado Pago")
  public PreferenciaMercadoPagoResponseDto crearPreferenciaAdelantoMercadoPago(
      @Valid @RequestBody CobroExtraordinarioRequest.PreferenciaAdelanto r) {
    return extraordinarios.crearPreferenciaAdelantoMercadoPago(r);
  }

  @PostMapping("/obligaciones/{id}/confirmar-pago")
  @PreAuthorize("hasAnyRole('SECRETARIA','APODERADO')")
  public org.springframework.http.ResponseEntity<TransaccionResponseDto> confirmarPago(@PathVariable @Positive Long id) {
    var pago = pagos.confirmarPagoMercadoPago(id);
    return pago == null ? org.springframework.http.ResponseEntity.noContent().build()
        : org.springframework.http.ResponseEntity.ok(pago);
  }

  @PostMapping("/obligaciones/{id}/simular-pago")
  @PreAuthorize("hasAnyRole('SECRETARIA','APODERADO')")
  public TransaccionResponseDto simularPagoMercadoPago(
      @PathVariable @Positive Long id,
      @Valid @RequestBody com.colegio.shuji.tesoreria.application.dto.in.SimularPagoRequestDto r) {
    return extraordinarios.simularPagoMercadoPago(id, r.metodoPago(), r.referenciaOperacion());
  }

  @PostMapping("/obligaciones/adelanto/confirmar-pago")
  @PreAuthorize("hasAnyRole('SECRETARIA','APODERADO')")
  @Operation(summary = "Verificar estado de pago adelantado en Mercado Pago")
  public List<TransaccionResponseDto> confirmarPagoAdelantoMercadoPago(
      @Valid @RequestBody CobroExtraordinarioRequest.ConfirmarAdelanto r) {
    return extraordinarios.confirmarPagoAdelantoMercadoPago(r.obligacionPagoIds());
  }

  @PostMapping("/obligaciones/adelanto/simular-pago")
  @PreAuthorize("hasAnyRole('SECRETARIA','APODERADO')")
  @Operation(summary = "Simular pago adelantado múltiple en modo sandbox")
  public List<TransaccionResponseDto> simularPagoAdelantoMercadoPago(
      @Valid @RequestBody CobroExtraordinarioRequest.SimularPagoAdelanto r) {
    return extraordinarios.simularPagoAdelantoMercadoPago(r);
  }
}
