package com.colegio.shuji.curriculo.infrastructure.controller.rest;

import com.colegio.shuji.curriculo.application.dto.in.*;
import com.colegio.shuji.curriculo.application.dto.out.*;
import com.colegio.shuji.curriculo.application.port.in.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/horarios")
@Validated
@RequiredArgsConstructor
@Tag(name = "Curriculo")
public class HorarioController {
  private final GenerarMallaHorariaUseCase horarios;

  @PostMapping("")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "horarios.programar")
  public HorarioSeccionResponseDto operacion0(@Valid @RequestBody ProgramarHorarioRequestDto r) {
    return horarios.programar(r);
  }

  @GetMapping("/seccion/{id}")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','DOCENTE')")
  @Operation(summary = "horarios.porSeccion")
  public MallaHorariaResponseDto operacion1(
      @PathVariable @Positive Integer id, @RequestParam Short anioId) {
    return horarios.porSeccion(id, anioId);
  }

  @GetMapping("/docente/{id}")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','DOCENTE')")
  @Operation(summary = "horarios.porDocente")
  public MallaHorariaResponseDto operacion2(
      @PathVariable @Positive Long id, @RequestParam Short anioId) {
    return horarios.porDocente(id, anioId);
  }
}
