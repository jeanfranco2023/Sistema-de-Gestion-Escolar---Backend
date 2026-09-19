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
@RequestMapping("/api/v1/evaluacion/refuerzo")
@Validated
@RequiredArgsConstructor
@Tag(name = "Evaluacion")
public class RefuerzoController {
  private final DerivarAlumnosRefuerzoUseCase refuerzo;

  @PostMapping("")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','DOCENTE')")
  @Operation(summary = "refuerzo.programar")
  public SesionRefuerzoResponseDto operacion0(@Valid @RequestBody ProgramarRefuerzoRequestDto r) {
    return refuerzo.programar(r);
  }

  @GetMapping("")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','DOCENTE')")
  @Operation(summary = "refuerzo.sesiones")
  public List<SesionRefuerzoResponseDto> operacion1(@RequestParam Short periodoId) {
    return refuerzo.sesiones(periodoId);
  }

  @PostMapping("/{id}/derivacion")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','DOCENTE')")
  @Operation(summary = "refuerzo.derivar")
  public List<InscripcionRefuerzoResponseDto> operacion2(@PathVariable @Positive Long id) {
    return refuerzo.derivar(id);
  }

  @PostMapping("/asistencia")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','DOCENTE')")
  @Operation(summary = "refuerzo.registrarAsistencia")
  public InscripcionRefuerzoResponseDto operacion3(
      @Valid @RequestBody RegistrarAsistenciaRefuerzoRequestDto r) {
    return refuerzo.registrarAsistencia(r);
  }
}
