package com.colegio.shuji.asistencia.infrastructure.controller.rest;

import com.colegio.shuji.asistencia.application.dto.in.ImportarLoteBiometricoRequestDto;
import com.colegio.shuji.asistencia.application.dto.in.JustificarInasistenciaRequestDto;
import com.colegio.shuji.asistencia.application.dto.in.RegistrarAsistenciaAulaRequestDto;
import com.colegio.shuji.asistencia.application.dto.out.AsistenciaAulaResponseDto;
import com.colegio.shuji.asistencia.application.dto.out.ConciliacionAsistenciaResponseDto;
import com.colegio.shuji.asistencia.application.dto.out.DiscrepanciaAlertaResponseDto;
import com.colegio.shuji.asistencia.application.dto.out.LoteBiometricoResponseDto;
import com.colegio.shuji.asistencia.application.dto.out.ReporteAsistenciaDiariaResponseDto;
import com.colegio.shuji.asistencia.application.port.in.EjecutarConciliacionDiariaUseCase;
import com.colegio.shuji.asistencia.application.port.in.ProcesarBiometricoUseCase;
import com.colegio.shuji.asistencia.application.port.in.RegistrarListaAulaUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
  public LoteBiometricoResponseDto importarLoteBiometrico(
      @Valid @RequestBody ImportarLoteBiometricoRequestDto r) {
    return biometrico.importar(r);
  }

  @PostMapping("/aula")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','AUXILIAR','TUTOR')")
  @Operation(summary = "aula.registrar")
  public AsistenciaAulaResponseDto registrarAsistenciaAula(
      @Valid @RequestBody RegistrarAsistenciaAulaRequestDto r) {
    return aula.registrar(r);
  }

  @PostMapping("/aula/justificacion")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','AUXILIAR','TUTOR')")
  @Operation(summary = "aula.justificar")
  public AsistenciaAulaResponseDto justificarInasistencia(
      @Valid @RequestBody JustificarInasistenciaRequestDto r) {
    return aula.justificar(r);
  }

  @GetMapping("/aula")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','AUXILIAR','TUTOR')")
  @Operation(summary = "aula.reporte")
  public ReporteAsistenciaDiariaResponseDto obtenerReporteDiario(@RequestParam LocalDate fecha) {
    return aula.reporte(fecha);
  }

  @PostMapping("/conciliacion")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','AUXILIAR','TUTOR')")
  @Operation(summary = "conciliacion.conciliar")
  public List<ConciliacionAsistenciaResponseDto> conciliarAsistencia(
      @RequestParam Short anioId,
      @RequestParam LocalDate fecha,
      @RequestParam boolean turnoCerrado) {
    return conciliacion.conciliar(anioId, fecha, turnoCerrado);
  }

  @GetMapping("/conciliacion/alertas")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','AUXILIAR','TUTOR')")
  @Operation(summary = "conciliacion.alertas")
  public List<DiscrepanciaAlertaResponseDto> consultarAlertas(@RequestParam LocalDate fecha) {
    return conciliacion.alertas(fecha);
  }
}
