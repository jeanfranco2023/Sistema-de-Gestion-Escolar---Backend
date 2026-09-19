package com.colegio.shuji.asistencia.infrastructure.controller.rest;

import com.colegio.shuji.asistencia.application.dto.in.*;
import com.colegio.shuji.asistencia.application.dto.out.*;
import com.colegio.shuji.asistencia.application.port.in.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/asistencia")
@Validated
@RequiredArgsConstructor
@Tag(name = "Asistencia")
public class AsistenciaController {
  private final ProcesarBiometricoUseCase biometrico;
  private final RegistrarListaAulaUseCase aula;
  private final EjecutarConciliacionDiariaUseCase conciliacion;

  @PostMapping("/biometrico/importar")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','AUXILIAR','TUTOR')")
  @Operation(summary = "biometrico.importar")
  public LoteBiometricoResponseDto operacion0(
      @Valid @RequestBody ImportarLoteBiometricoRequestDto r) {
    return biometrico.importar(r);
  }

  @PostMapping("/aula")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','AUXILIAR','TUTOR')")
  @Operation(summary = "aula.registrar")
  public AsistenciaAulaResponseDto operacion1(
      @Valid @RequestBody RegistrarAsistenciaAulaRequestDto r) {
    return aula.registrar(r);
  }

  @PostMapping("/aula/justificacion")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','AUXILIAR','TUTOR')")
  @Operation(summary = "aula.justificar")
  public AsistenciaAulaResponseDto operacion2(
      @Valid @RequestBody JustificarInasistenciaRequestDto r) {
    return aula.justificar(r);
  }

  @GetMapping("/aula")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','AUXILIAR','TUTOR')")
  @Operation(summary = "aula.reporte")
  public ReporteAsistenciaDiariaResponseDto operacion3(@RequestParam LocalDate fecha) {
    return aula.reporte(fecha);
  }

  @PostMapping("/conciliacion")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','AUXILIAR','TUTOR')")
  @Operation(summary = "conciliacion.conciliar")
  public List<ConciliacionAsistenciaResponseDto> operacion4(
      @RequestParam Short anioId,
      @RequestParam LocalDate fecha,
      @RequestParam boolean turnoCerrado) {
    return conciliacion.conciliar(anioId, fecha, turnoCerrado);
  }

  @GetMapping("/conciliacion/alertas")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','AUXILIAR','TUTOR')")
  @Operation(summary = "conciliacion.alertas")
  public List<DiscrepanciaAlertaResponseDto> operacion5(@RequestParam LocalDate fecha) {
    return conciliacion.alertas(fecha);
  }
}
