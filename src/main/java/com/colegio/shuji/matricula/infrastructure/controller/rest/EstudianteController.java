package com.colegio.shuji.matricula.infrastructure.controller.rest;

import com.colegio.shuji.matricula.application.dto.in.RegistrarEstudianteRequestDto;
import com.colegio.shuji.matricula.application.dto.in.VincularApoderadoRequestDto;
import com.colegio.shuji.matricula.application.dto.out.EstudianteApoderadoResponseDto;
import com.colegio.shuji.matricula.application.dto.out.EstudianteResponseDto;
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
@RequestMapping("/api/v1/estudiantes")
@Validated
@RequiredArgsConstructor
@Tag(name = "Matricula")
public class EstudianteController {
  private final RegistrarFichaFamiliarUseCase familias;

  @PostMapping("")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "familias.registrarEstudiante")
  public EstudianteResponseDto registrarEstudiante(@Valid @RequestBody RegistrarEstudianteRequestDto r) {
    return familias.registrarEstudiante(r);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "familias.consultarEstudiante")
  public EstudianteResponseDto consultarEstudiante(@PathVariable @Positive Long id) {
    return familias.consultarEstudiante(id);
  }

  @PostMapping("/apoderados")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "familias.vincular")
  public EstudianteApoderadoResponseDto vincularApoderado(
      @Valid @RequestBody VincularApoderadoRequestDto r) {
    return familias.vincular(r);
  }
}
