package com.colegio.shuji.matricula.infrastructure.controller.rest;

import com.colegio.shuji.matricula.application.dto.in.RegistrarApoderadoRequestDto;
import com.colegio.shuji.matricula.application.dto.out.ApoderadoResponseDto;
import com.colegio.shuji.matricula.application.port.in.RegistrarFichaFamiliarUseCase;
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
@RequestMapping("/api/v1/apoderados")
@Validated
@RequiredArgsConstructor
@Tag(name = "Matricula")
public class ApoderadoController {
  private final RegistrarFichaFamiliarUseCase familias;

  @PostMapping("")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "familias.registrarApoderado")
  public ApoderadoResponseDto registrarApoderado(@Valid @RequestBody RegistrarApoderadoRequestDto r) {
    return familias.registrarApoderado(r);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "familias.consultarApoderado")
  public ApoderadoResponseDto consultarApoderado(@PathVariable @Positive Long id) {
    return familias.consultarApoderado(id);
  }
}
