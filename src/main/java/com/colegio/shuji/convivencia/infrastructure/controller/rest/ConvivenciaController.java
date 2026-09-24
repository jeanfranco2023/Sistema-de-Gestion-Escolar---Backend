package com.colegio.shuji.convivencia.infrastructure.controller.rest;

import com.colegio.shuji.convivencia.application.dto.in.ActualizarEstadoIncidenciaRequestDto;
import com.colegio.shuji.convivencia.application.dto.in.RegistrarIncidenciaRequestDto;
import com.colegio.shuji.convivencia.application.dto.out.HistorialConductualEstudianteResponseDto;
import com.colegio.shuji.convivencia.application.dto.out.IncidenciaResponseDto;
import com.colegio.shuji.convivencia.application.port.in.GestionarIncidenciasUseCase;
import com.colegio.shuji.convivencia.application.port.in.RegistrarIncidenciaUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/convivencia")
@Validated
@RequiredArgsConstructor
@Tag(name = "Convivencia")
public class ConvivenciaController {
  private final RegistrarIncidenciaUseCase registro;
  private final GestionarIncidenciasUseCase gestion;

  @GetMapping("/incidencias")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','AUXILIAR','TUTOR')")
  public List<IncidenciaResponseDto> listarIncidencias() {
    return gestion.listar();
  }

  @PostMapping("/incidencias")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','AUXILIAR','TUTOR')")
  @Operation(summary = "registro.registrar")
  public IncidenciaResponseDto registrarIncidencia(@Valid @RequestBody RegistrarIncidenciaRequestDto r) {
    return registro.registrar(r);
  }

  @PutMapping("/incidencias/{id}/estado")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','AUXILIAR','TUTOR')")
  @Operation(summary = "gestion.actualizar")
  public IncidenciaResponseDto actualizarEstadoIncidencia(
      @PathVariable @Positive Long id, @Valid @RequestBody ActualizarEstadoIncidenciaRequestDto r) {
    return gestion.actualizar(id, r);
  }

  @GetMapping("/matricula/{id}")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','AUXILIAR','TUTOR')")
  @Operation(summary = "gestion.historial")
  public HistorialConductualEstudianteResponseDto consultarHistorialConductual(@PathVariable @Positive Long id) {
    return gestion.historial(id);
  }

  @GetMapping("/incidencias/citaciones")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','AUXILIAR','TUTOR')")
  @Operation(summary = "gestion.citaciones")
  public List<IncidenciaResponseDto> listarCitaciones(
      @RequestParam(name = "page", defaultValue = "0") @PositiveOrZero int page,
      @RequestParam(name = "size", defaultValue = "20") @Positive @Max(100) int size) {
    List<IncidenciaResponseDto> todas = gestion.citaciones();
    if (todas == null || todas.isEmpty()) {
      return List.of();
    }
    int start = Math.min(Math.max(0, page) * Math.max(1, size), todas.size());
    int end = Math.min(start + Math.max(1, size), todas.size());
    return todas.subList(start, end);
  }
}
