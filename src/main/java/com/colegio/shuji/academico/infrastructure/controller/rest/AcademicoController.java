package com.colegio.shuji.academico.infrastructure.controller.rest;

import com.colegio.shuji.academico.application.dto.in.*;
import com.colegio.shuji.academico.application.dto.out.*;
import com.colegio.shuji.academico.application.port.in.*;
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
@RequestMapping("/api/v1/academico")
@Validated
@RequiredArgsConstructor
@Tag(name = "Academico")
public class AcademicoController {
  private final GestionarAnioLectivoUseCase calendario;
  private final GestionarSeccionesUseCase estructura;
  private final ConsultarVacantesUseCase cupos;

  @PostMapping("/anios")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "calendario.crearAnio")
  public AnioLectivoResponseDto operacion0(@Valid @RequestBody CrearAnioLectivoRequestDto r) {
    return calendario.crearAnio(r);
  }

  @GetMapping("/anios")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "calendario.listarAnios")
  public List<AnioLectivoResponseDto> operacion1() {
    return calendario.listarAnios();
  }

  @PutMapping("/anios/{id}/cierre")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "calendario.cerrarAnio")
  public void operacion2(@PathVariable @Positive Short id) {
    calendario.cerrarAnio(id);
  }

  @PostMapping("/periodos")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "calendario.crearPeriodo")
  public PeriodoResponseDto operacion3(@Valid @RequestBody CrearPeriodoRequestDto r) {
    return calendario.crearPeriodo(r);
  }

  @GetMapping("/periodos")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "calendario.listarPeriodos")
  public List<PeriodoResponseDto> operacion4(@RequestParam Short anioId) {
    return calendario.listarPeriodos(anioId);
  }

  @PutMapping("/periodos/{id}/cierre")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "calendario.cerrarPeriodo")
  public void operacion5(@PathVariable @Positive Short id) {
    calendario.cerrarPeriodo(id);
  }

  @GetMapping("/niveles")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "estructura.listarNiveles")
  public List<NivelResponseDto> operacion6() {
    return estructura.listarNiveles();
  }

  @GetMapping("/grados")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "estructura.listarGrados")
  public List<GradoResponseDto> operacion7(@RequestParam Short nivelId) {
    return estructura.listarGrados(nivelId);
  }

  @PostMapping("/grados")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "estructura.crearGrado")
  public GradoResponseDto operacion8(@Valid @RequestBody CrearGradoRequestDto r) {
    return estructura.crearGrado(r);
  }

  @PostMapping("/secciones")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "estructura.crearSeccion")
  public SeccionResponseDto operacion9(@Valid @RequestBody CrearSeccionRequestDto r) {
    return estructura.crearSeccion(r);
  }

  @GetMapping("/secciones")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "estructura.listarSecciones")
  public List<SeccionResponseDto> operacion10(
      @RequestParam Short anioId, @RequestParam(required = false) Short nivelId) {
    return estructura.listarSecciones(anioId, nivelId);
  }

  @GetMapping("/secciones/{id}/vacantes")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "cupos.vacantes")
  public VacantesSeccionResponseDto operacion11(@PathVariable @Positive Integer id) {
    return cupos.vacantes(id);
  }
}
