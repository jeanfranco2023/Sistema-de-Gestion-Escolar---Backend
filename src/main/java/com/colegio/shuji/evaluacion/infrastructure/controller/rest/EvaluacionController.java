package com.colegio.shuji.evaluacion.infrastructure.controller.rest;

import com.colegio.shuji.evaluacion.application.dto.in.*;
import com.colegio.shuji.evaluacion.application.dto.out.*;
import com.colegio.shuji.evaluacion.application.port.in.*;
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
@RequestMapping("/api/v1/evaluacion")
@Validated
@RequiredArgsConstructor
@Tag(name = "Evaluacion")
public class EvaluacionController {
  private final RegistrarEvaluacionCnebUseCase registro;
  private final ConsultarLibretaNotasUseCase consulta;

  @PostMapping("/cneb")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','DOCENTE')")
  @Operation(summary = "registro.registrar")
  public List<CalificacionResponseDto> operacion0(
      @Valid @RequestBody RegistrarCalificacionesMasivasRequestDto r) {
    return registro.registrar(r);
  }

  @GetMapping("/libreta/{matriculaId}")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','DOCENTE')")
  @Operation(summary = "consulta.libreta")
  public LibretaNotasResponseDto operacion1(@PathVariable @Positive Long matriculaId) {
    return consulta.libreta(matriculaId);
  }

  @GetMapping("/riesgo")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','DOCENTE')")
  @Operation(summary = "consulta.riesgo")
  public EstudiantesEnRiesgoResponseDto operacion2(@RequestParam Short periodoId) {
    return consulta.riesgo(periodoId);
  }
}
