package com.colegio.shuji.curriculo.infrastructure.controller.rest;

import com.colegio.shuji.curriculo.application.dto.in.*;
import com.colegio.shuji.curriculo.application.dto.out.*;
import com.colegio.shuji.curriculo.application.port.in.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
  public AreaCurricularResponseDto operacion0(@Valid @RequestBody CrearAreaRequestDto r) {
    return curriculo.crearArea(r);
  }

  @GetMapping("/areas")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','DOCENTE')")
  @Operation(summary = "curriculo.listarAreas")
  public List<AreaCurricularResponseDto> operacion1(@RequestParam Short nivelId) {
    return curriculo.listarAreas(nivelId);
  }

  @PostMapping("/competencias")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "curriculo.crearCompetencia")
  public CompetenciaResponseDto operacion2(@Valid @RequestBody CrearCompetenciaRequestDto r) {
    return curriculo.crearCompetencia(r);
  }

  @GetMapping("/competencias")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','DOCENTE')")
  @Operation(summary = "curriculo.listarCompetencias")
  public List<CompetenciaResponseDto> operacion3(@RequestParam Short areaId) {
    return curriculo.listarCompetencias(areaId);
  }

  @PostMapping("/bloques")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "curriculo.crearBloque")
  public BloqueHorarioResponseDto operacion4(@Valid @RequestBody CrearBloqueRequestDto r) {
    return curriculo.crearBloque(r);
  }

  @GetMapping("/bloques")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','DOCENTE')")
  @Operation(summary = "curriculo.listarBloques")
  public List<BloqueHorarioResponseDto> operacion5() {
    return curriculo.listarBloques();
  }

  @PostMapping("/asignaciones")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "carga.asignarDocente")
  public AsignacionDocenteResponseDto operacion6(@Valid @RequestBody AsignarDocenteRequestDto r) {
    return carga.asignarDocente(r);
  }
}
