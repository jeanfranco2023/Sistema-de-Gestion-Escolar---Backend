package com.colegio.shuji.convivencia.infrastructure.controller.rest;

import com.colegio.shuji.convivencia.application.dto.in.*;
import com.colegio.shuji.convivencia.application.dto.out.*;
import com.colegio.shuji.convivencia.application.port.in.*;
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
@RequestMapping("/api/v1/convivencia")
@Validated
@RequiredArgsConstructor
@Tag(name = "Convivencia")
public class ConvivenciaController {
  private final RegistrarIncidenciaUseCase registro;
  private final GestionarIncidenciasUseCase gestion;

  @PostMapping("/incidencias")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','AUXILIAR','TUTOR')")
  @Operation(summary = "registro.registrar")
  public IncidenciaResponseDto operacion0(@Valid @RequestBody RegistrarIncidenciaRequestDto r) {
    return registro.registrar(r);
  }

  @PutMapping("/incidencias/{id}/estado")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','AUXILIAR','TUTOR')")
  @Operation(summary = "gestion.actualizar")
  public IncidenciaResponseDto operacion1(
      @PathVariable @Positive Long id, @Valid @RequestBody ActualizarEstadoIncidenciaRequestDto r) {
    return gestion.actualizar(id, r);
  }

  @GetMapping("/matricula/{id}")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','AUXILIAR','TUTOR')")
  @Operation(summary = "gestion.historial")
  public HistorialConductualEstudianteResponseDto operacion2(@PathVariable @Positive Long id) {
    return gestion.historial(id);
  }

  @GetMapping("/incidencias/citaciones")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','AUXILIAR','TUTOR')")
  @Operation(summary = "gestion.citaciones")
  public List<IncidenciaResponseDto> operacion3() {
    return gestion.citaciones();
  }
}
