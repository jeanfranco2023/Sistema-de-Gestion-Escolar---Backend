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
@RequestMapping("/api/v1/apoderados")
@Validated
@RequiredArgsConstructor
@Tag(name = "Matricula")
public class ApoderadoController {
  private final RegistrarFichaFamiliarUseCase familias;

  @PostMapping("")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "familias.registrarApoderado")
  public ApoderadoResponseDto operacion0(@Valid @RequestBody RegistrarApoderadoRequestDto r) {
    return familias.registrarApoderado(r);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "familias.consultarApoderado")
  public ApoderadoResponseDto operacion1(@PathVariable @Positive Long id) {
    return familias.consultarApoderado(id);
  }
}
