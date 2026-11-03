package com.colegio.shuji.matricula.infrastructure.controller.rest;

import com.colegio.shuji.matricula.application.dto.in.CambiarEstadoApoderadoRequestDto;
import com.colegio.shuji.matricula.application.dto.in.RegistrarApoderadoRequestDto;
import com.colegio.shuji.matricula.application.dto.out.ApoderadoResponseDto;
import com.colegio.shuji.matricula.application.port.in.RegistrarFichaFamiliarUseCase;
import com.colegio.shuji.usuario.application.dto.in.CorregirDniRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
  @PreAuthorize("hasRole('SECRETARIA')")
  @Operation(summary = "familias.registrarApoderado")
  public ApoderadoResponseDto registrarApoderado(@Valid @RequestBody RegistrarApoderadoRequestDto r) {
    return familias.registrarApoderado(r);
  }

  @GetMapping("")
  @PreAuthorize("hasRole('SECRETARIA')")
  @Operation(summary = "familias.listarApoderados")
  public List<ApoderadoResponseDto> listarApoderados() {
    return familias.listarApoderados();
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "familias.consultarApoderado")
  public ApoderadoResponseDto consultarApoderado(@PathVariable @Positive Long id) {
    return familias.consultarApoderado(id);
  }

  @PatchMapping("/{id}/dni")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "Corregir DNI del apoderado con auditoría")
  public ApoderadoResponseDto corregirDni(
      @PathVariable @Positive Long id,
      @Valid @RequestBody CorregirDniRequestDto request) {
    return familias.corregirDniApoderado(id, request);
  }

  @PatchMapping("/{id}/estado")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "Cambiar estado del apoderado (borrado lógico formal)")
  public ApoderadoResponseDto cambiarEstado(
      @PathVariable @Positive Long id,
      @Valid @RequestBody CambiarEstadoApoderadoRequestDto request) {
    return familias.cambiarEstadoApoderado(id, request);
  }
}
