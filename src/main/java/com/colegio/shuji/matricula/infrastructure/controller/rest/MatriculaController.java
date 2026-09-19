package com.colegio.shuji.matricula.infrastructure.controller.rest;

import com.colegio.shuji.matricula.application.dto.in.*;
import com.colegio.shuji.matricula.application.dto.out.*;
import com.colegio.shuji.matricula.application.port.in.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/matriculas")
@Validated
@RequiredArgsConstructor
@Tag(name = "Matricula")
public class MatriculaController {
  private final ProcesarMatriculaUseCase matriculas;
  private final LiberarReservasVencidasUseCase reservas;

  @PostMapping("")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "matriculas.solicitar")
  public MatriculaResponseDto operacion0(@Valid @RequestBody SolicitarMatriculaRequestDto r) {
    return matriculas.solicitar(r);
  }

  @PostMapping("/confirmacion")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "matriculas.confirmar")
  public MatriculaResponseDto operacion1(@Valid @RequestBody ConfirmarMatriculaRequestDto r) {
    return matriculas.confirmar(r);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "matriculas.ficha")
  public FichaMatriculaResponseDto operacion2(@PathVariable @Positive Long id) {
    return matriculas.ficha(id);
  }

  @PostMapping("/reservas/liberar")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "reservas.liberarVencidas")
  public int operacion3() {
    return reservas.liberarVencidas();
  }
}
