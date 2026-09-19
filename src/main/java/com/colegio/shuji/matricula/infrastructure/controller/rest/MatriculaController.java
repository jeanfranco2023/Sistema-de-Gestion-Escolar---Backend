package com.colegio.shuji.matricula.infrastructure.controller.rest;

import com.colegio.shuji.matricula.application.dto.in.ConfirmarMatriculaRequestDto;
import com.colegio.shuji.matricula.application.dto.in.SolicitarMatriculaRequestDto;
import com.colegio.shuji.matricula.application.dto.out.FichaMatriculaResponseDto;
import com.colegio.shuji.matricula.application.dto.out.MatriculaResponseDto;
import com.colegio.shuji.matricula.application.port.in.LiberarReservasVencidasUseCase;
import com.colegio.shuji.matricula.application.port.in.ProcesarMatriculaUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
  public MatriculaResponseDto solicitarMatricula(@Valid @RequestBody SolicitarMatriculaRequestDto r) {
    return matriculas.solicitar(r);
  }

  @PostMapping("/confirmacion")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "matriculas.confirmar")
  public MatriculaResponseDto confirmarMatricula(@Valid @RequestBody ConfirmarMatriculaRequestDto r) {
    return matriculas.confirmar(r);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "matriculas.ficha")
  public FichaMatriculaResponseDto consultarFicha(@PathVariable @Positive Long id) {
    return matriculas.ficha(id);
  }

  @PostMapping("/reservas/liberar")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "reservas.liberarVencidas")
  public int liberarReservasVencidas() {
    return reservas.liberarVencidas();
  }
}
