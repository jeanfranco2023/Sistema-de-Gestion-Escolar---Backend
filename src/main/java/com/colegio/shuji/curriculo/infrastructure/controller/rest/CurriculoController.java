package com.colegio.shuji.curriculo.infrastructure.controller.rest;

import com.colegio.shuji.curriculo.application.dto.in.AsignarDocenteRequestDto;
import com.colegio.shuji.curriculo.application.dto.in.CrearAreaRequestDto;
import com.colegio.shuji.curriculo.application.dto.in.CrearBloqueRequestDto;
import com.colegio.shuji.curriculo.application.dto.in.CrearCompetenciaRequestDto;
import com.colegio.shuji.curriculo.application.dto.out.AreaCurricularResponseDto;
import com.colegio.shuji.curriculo.application.dto.out.AsignacionDocenteResponseDto;
import com.colegio.shuji.curriculo.application.dto.out.BloqueHorarioResponseDto;
import com.colegio.shuji.curriculo.application.dto.out.CompetenciaResponseDto;
import com.colegio.shuji.curriculo.application.port.in.AsignarCargaDocenteUseCase;
import com.colegio.shuji.curriculo.application.port.in.GestionarCurriculoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "curriculo.crearArea")
  public AreaCurricularResponseDto crearArea(@Valid @RequestBody CrearAreaRequestDto r) {
    return curriculo.crearArea(r);
  }

  @GetMapping("/areas")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','DOCENTE')")
  @Operation(summary = "curriculo.listarAreas")
  public List<AreaCurricularResponseDto> listarAreas(@RequestParam Short nivelId) {
    return curriculo.listarAreas(nivelId);
  }

  @PostMapping("/competencias")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "curriculo.crearCompetencia")
  public CompetenciaResponseDto crearCompetencia(@Valid @RequestBody CrearCompetenciaRequestDto r) {
    return curriculo.crearCompetencia(r);
  }

  @GetMapping("/competencias")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','DOCENTE')")
  @Operation(summary = "curriculo.listarCompetencias")
  public List<CompetenciaResponseDto> listarCompetencias(@RequestParam Short areaId) {
    return curriculo.listarCompetencias(areaId);
  }

  @PostMapping("/bloques")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "curriculo.crearBloque")
  public BloqueHorarioResponseDto crearBloque(@Valid @RequestBody CrearBloqueRequestDto r) {
    return curriculo.crearBloque(r);
  }

  @GetMapping("/bloques")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','DOCENTE')")
  @Operation(summary = "curriculo.listarBloques")
  public List<BloqueHorarioResponseDto> listarBloques() {
    return curriculo.listarBloques();
  }

  @PostMapping("/asignaciones")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "carga.asignarDocente")
  public AsignacionDocenteResponseDto asignarDocente(@Valid @RequestBody AsignarDocenteRequestDto r) {
    return carga.asignarDocente(r);
  }

  @GetMapping("/asignaciones")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','DOCENTE')")
  public List<AsignacionDocenteResponseDto> listarAsignaciones() {
    return carga.listarAsignaciones();
  }
}
