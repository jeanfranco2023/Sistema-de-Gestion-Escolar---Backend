package com.colegio.shuji.matricula.infrastructure.controller.rest;

import com.colegio.shuji.matricula.application.dto.in.ConfirmarMatriculaRequestDto;
import com.colegio.shuji.matricula.application.dto.in.CambiarEstadoMatriculaRequestDto;
import com.colegio.shuji.matricula.application.dto.in.SolicitarMatriculaRequestDto;
import com.colegio.shuji.matricula.application.dto.out.FichaMatriculaResponseDto;
import com.colegio.shuji.matricula.application.dto.out.MatriculaResponseDto;
import com.colegio.shuji.matricula.application.port.in.LiberarReservasVencidasUseCase;
import com.colegio.shuji.matricula.application.port.in.ProcesarMatriculaUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/matriculas")
@Validated
@RequiredArgsConstructor
@Tag(name = "Matricula")
public class MatriculaController {
  private final ProcesarMatriculaUseCase matriculas;
  private final LiberarReservasVencidasUseCase reservas;
  private final com.colegio.shuji.matricula.infrastructure.service.MatriculaPresencialService presencialService;

  @GetMapping("/presencial/importe")
  @PreAuthorize("hasAnyRole('DIRECCION','ADMINISTRADOR','SECRETARIA')")
  public java.math.BigDecimal importeEfectivoPresencial() {
    return presencialService.importeEfectivo();
  }

  @PostMapping(value = "/presencial", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyRole('DIRECCION','ADMINISTRADOR','SECRETARIA')")
  @Operation(summary = "Registrar matrícula presencial con evidencias y pago en efectivo")
  public com.colegio.shuji.matricula.application.dto.out.MatriculaPresencialResponseDto registrarMatriculaPresencial(
      @Valid @org.springframework.web.bind.annotation.ModelAttribute com.colegio.shuji.matricula.application.dto.in.RegistrarMatriculaPresencialRequestDto r) {
    return presencialService.registrar(r);
  }

  @PostMapping(value = "/presencial/pago-en-linea", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyRole('DIRECCION','ADMINISTRADOR','SECRETARIA')")
  public com.colegio.shuji.matricula.infrastructure.service.MatriculaPresencialService.PagoEnLinea iniciarPagoEnLinea(
      @Valid @org.springframework.web.bind.annotation.ModelAttribute com.colegio.shuji.matricula.application.dto.in.RegistrarMatriculaPresencialRequestDto r) {
    return presencialService.iniciarPagoEnLinea(r);
  }

  @PostMapping("/presencial/{id}/verificar-pago")
  @PreAuthorize("hasAnyRole('DIRECCION','ADMINISTRADOR','SECRETARIA')")
  public com.colegio.shuji.matricula.infrastructure.service.MatriculaPresencialService.EstadoPagoPresencial estadoPago(
      @PathVariable java.util.UUID id) {
    return presencialService.estadoPago(id);
  }

  @PostMapping("/presencial/{id}/simular-pago")
  @PreAuthorize("hasAnyRole('DIRECCION','ADMINISTRADOR','SECRETARIA')")
  public com.colegio.shuji.matricula.infrastructure.service.MatriculaPresencialService.EstadoPagoPresencial simularPagoEnLinea(
      @PathVariable java.util.UUID id,
      @Valid @RequestBody com.colegio.shuji.tesoreria.application.dto.in.SimularPagoRequestDto request) {
    return presencialService.simularPagoEnLinea(id, request.metodoPago(), request.referenciaOperacion());
  }

  @PostMapping("")
  @PreAuthorize("hasAnyRole('SECRETARIA','DIRECCION','ADMINISTRADOR')")
  @Operation(summary = "matriculas.solicitar")
  public MatriculaResponseDto solicitarMatricula(@Valid @RequestBody SolicitarMatriculaRequestDto r) {
    return matriculas.solicitar(r);
  }

  @PostMapping("/confirmacion")
  @PreAuthorize("hasAnyRole('DIRECCION','ADMINISTRADOR','SECRETARIA')")
  @Operation(summary = "matriculas.confirmar")
  public MatriculaResponseDto confirmarMatricula(@Valid @RequestBody ConfirmarMatriculaRequestDto r) {
    return matriculas.confirmar(r);
  }

  @PatchMapping("/{id}/estado")
  @PreAuthorize("hasAnyRole('SECRETARIA','ADMINISTRADOR')")
  @Operation(summary = "matriculas.cambiarEstado")
  public MatriculaResponseDto cambiarEstado(
      @PathVariable @Positive Long id,
      @Valid @RequestBody CambiarEstadoMatriculaRequestDto r) {
    return matriculas.cambiarEstado(id, r);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('DIRECCION','ADMINISTRADOR','SECRETARIA')")
  @Operation(summary = "matriculas.ficha")
  public FichaMatriculaResponseDto consultarFicha(@PathVariable @Positive Long id) {
    return matriculas.ficha(id);
  }

  @GetMapping("/seccion/{seccionId}")
  @PreAuthorize("hasAnyRole('DIRECCION','ADMINISTRADOR','SECRETARIA')")
  @Operation(summary = "matriculas.listarPorSeccion")
  public List<MatriculaResponseDto> listarPorSeccion(
      @PathVariable @Positive Integer seccionId,
      @RequestParam(name = "page", defaultValue = "0") @PositiveOrZero int page,
      @RequestParam(name = "size", defaultValue = "20") @Positive @Max(100) int size) {
    return matriculas.listarPorSeccion(seccionId, page, size);
  }

  @GetMapping("/anio/{anioLectivoId}/registros")
  @PreAuthorize("hasAnyRole('DIRECCION','ADMINISTRADOR','SECRETARIA')")
  @Operation(summary = "matriculas.listarPorAnio")
  public List<MatriculaResponseDto> listarPorAnio(@PathVariable @Positive Short anioLectivoId) {
    return matriculas.listarPorAnio(anioLectivoId);
  }

  @GetMapping("/anio/{anioLectivoId}/estudiantes-matriculados")
  @PreAuthorize("hasAnyRole('DIRECCION','ADMINISTRADOR','SECRETARIA')")
  @Operation(summary = "matriculas.listarEstudianteIdsMatriculados")
  public List<Long> listarEstudianteIdsMatriculados(@PathVariable @Positive Short anioLectivoId) {
    return matriculas.listarEstudianteIdsMatriculados(anioLectivoId);
  }

  @PostMapping("/reservas/liberar")
  @PreAuthorize("hasAnyRole('SECRETARIA','ADMINISTRADOR')")
  @Operation(summary = "reservas.liberarVencidas")
  public int liberarReservasVencidas() {
    return reservas.liberarVencidas();
  }
}
