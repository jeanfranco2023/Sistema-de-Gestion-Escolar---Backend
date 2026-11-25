package com.colegio.shuji.curriculo.infrastructure.controller.rest;

import com.colegio.shuji.curriculo.application.dto.in.AsignarDocenteRequestDto;
import com.colegio.shuji.curriculo.application.dto.in.CrearAreaRequestDto;
import com.colegio.shuji.curriculo.application.dto.in.CrearBloqueRequestDto;
import com.colegio.shuji.curriculo.application.dto.in.CrearCompetenciaRequestDto;
import com.colegio.shuji.curriculo.application.dto.in.ReasignarDocenteRequestDto;
import com.colegio.shuji.curriculo.application.dto.out.AreaCurricularResponseDto;
import com.colegio.shuji.curriculo.application.dto.out.AsignacionDocenteResponseDto;
import com.colegio.shuji.curriculo.application.dto.out.BloqueHorarioResponseDto;
import com.colegio.shuji.curriculo.application.dto.out.CompetenciaResponseDto;
import com.colegio.shuji.curriculo.application.dto.out.DocenteOpcionResponseDto;
import com.colegio.shuji.curriculo.application.port.in.AsignarCargaDocenteUseCase;
import com.colegio.shuji.curriculo.application.port.in.GestionarCurriculoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
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
@RequestMapping("/api/v1/curriculo")
@Validated
@RequiredArgsConstructor
@Tag(name = "Curriculo")
public class CurriculoController {
  private final GestionarCurriculoUseCase curriculo;
  private final AsignarCargaDocenteUseCase carga;

  @PostMapping("/areas")
  @PreAuthorize("hasAnyRole('DIRECCION','ADMINISTRADOR')")
  @Operation(summary = "curriculo.crearArea")
  public AreaCurricularResponseDto crearArea(@Valid @RequestBody CrearAreaRequestDto r) {
    return curriculo.crearArea(r);
  }

  @GetMapping("/areas")
  @PreAuthorize("hasAnyRole('DIRECCION','ADMINISTRADOR','SECRETARIA','DOCENTE')")
  @Operation(summary = "curriculo.listarAreas")
  public List<AreaCurricularResponseDto> listarAreas(@RequestParam(required = false) Short nivelId) {
    return curriculo.listarAreas(nivelId);
  }

  @GetMapping("/docentes")
  @PreAuthorize("hasAnyRole('DIRECCION','ADMINISTRADOR')")
  public List<DocenteOpcionResponseDto> listarDocentes(@RequestParam @Positive Short areaId) {
    return carga.listarDocentesPorArea(areaId);
  }

  @PostMapping("/competencias")
  @PreAuthorize("hasAnyRole('DIRECCION','ADMINISTRADOR')")
  @Operation(summary = "curriculo.crearCompetencia")
  public CompetenciaResponseDto crearCompetencia(@Valid @RequestBody CrearCompetenciaRequestDto r) {
    return curriculo.crearCompetencia(r);
  }

  @GetMapping("/competencias")
  @PreAuthorize("hasAnyRole('DIRECCION','ADMINISTRADOR','DOCENTE')")
  @Operation(summary = "curriculo.listarCompetencias")
  public List<CompetenciaResponseDto> listarCompetencias(@RequestParam Short areaId) {
    return curriculo.listarCompetencias(areaId);
  }

  @PostMapping("/bloques")
  @PreAuthorize("hasAnyRole('DIRECCION','ADMINISTRADOR')")
  @Operation(summary = "curriculo.crearBloque")
  public BloqueHorarioResponseDto crearBloque(@Valid @RequestBody CrearBloqueRequestDto r) {
    return curriculo.crearBloque(r);
  }

  @GetMapping("/bloques")
  @PreAuthorize("hasAnyRole('DIRECCION','ADMINISTRADOR','DOCENTE')")
  @Operation(summary = "curriculo.listarBloques")
  public List<BloqueHorarioResponseDto> listarBloques() {
    return curriculo.listarBloques();
  }

  @PostMapping("/asignaciones")
  @PreAuthorize("hasAnyRole('DIRECCION','ADMINISTRADOR')")
  @Operation(summary = "carga.asignarDocente")
  public AsignacionDocenteResponseDto asignarDocente(@Valid @RequestBody AsignarDocenteRequestDto r) {
    return carga.asignarDocente(r);
  }

  @PutMapping("/asignaciones/{id}/docente")
  @PreAuthorize("hasAnyRole('DIRECCION','ADMINISTRADOR')")
  @Operation(summary = "carga.reasignarDocente")
  public AsignacionDocenteResponseDto reasignarDocente(
      @PathVariable @Positive Long id, @Valid @RequestBody ReasignarDocenteRequestDto r) {
    return carga.reasignarDocente(id, r.docenteUsuarioId());
  }

  @GetMapping("/asignaciones")
  @PreAuthorize("hasAnyRole('DIRECCION','ADMINISTRADOR','DOCENTE')")
  public List<AsignacionDocenteResponseDto> listarAsignaciones(
      @RequestParam(required = false) Short anioId) {
    return carga.listarAsignaciones(anioId);
  }
}
