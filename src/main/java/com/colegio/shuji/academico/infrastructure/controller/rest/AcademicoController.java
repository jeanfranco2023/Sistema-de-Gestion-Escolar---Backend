package com.colegio.shuji.academico.infrastructure.controller.rest;

import com.colegio.shuji.academico.application.dto.in.CrearAnioLectivoRequestDto;
import com.colegio.shuji.academico.application.dto.in.CrearAulaRequestDto;
import com.colegio.shuji.academico.application.dto.in.CrearGradoRequestDto;
import com.colegio.shuji.academico.application.dto.in.CrearPeriodoRequestDto;
import com.colegio.shuji.academico.application.dto.in.CrearSeccionRequestDto;
import com.colegio.shuji.academico.application.dto.out.AnioLectivoResponseDto;
import com.colegio.shuji.academico.application.dto.out.AulaResponseDto;
import com.colegio.shuji.academico.application.dto.out.GradoResponseDto;
import com.colegio.shuji.academico.application.dto.out.NivelResponseDto;
import com.colegio.shuji.academico.application.dto.out.PeriodoResponseDto;
import com.colegio.shuji.academico.application.dto.out.SeccionResponseDto;
import com.colegio.shuji.academico.application.dto.out.VacantesSeccionResponseDto;
import com.colegio.shuji.academico.application.port.in.ConsultarVacantesUseCase;
import com.colegio.shuji.academico.application.port.in.GestionarAnioLectivoUseCase;
import com.colegio.shuji.academico.application.port.in.GestionarAulasUseCase;
import com.colegio.shuji.academico.application.port.in.GestionarSeccionesUseCase;
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
@RequestMapping("/api/v1/academico")
@Validated
@RequiredArgsConstructor
@Tag(name = "Academico")
public class AcademicoController {
  private final GestionarAnioLectivoUseCase calendario;
  private final GestionarSeccionesUseCase estructura;
  private final ConsultarVacantesUseCase cupos;
  private final GestionarAulasUseCase aulas;

  @PostMapping("/aulas")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "estructura.crearAula")
  public AulaResponseDto crearAula(@Valid @RequestBody CrearAulaRequestDto request) {
    return aulas.crearAula(request);
  }

  @GetMapping("/aulas")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "estructura.listarAulas")
  public List<AulaResponseDto> listarAulas() {
    return aulas.listarAulas();
  }

  @PostMapping("/anios")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "calendario.crearAnio")
  public AnioLectivoResponseDto crearAnio(@Valid @RequestBody CrearAnioLectivoRequestDto r) {
    return calendario.crearAnio(r);
  }

  @GetMapping("/anios")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','AUXILIAR','TUTOR')")
  @Operation(summary = "calendario.listarAnios")
  public List<AnioLectivoResponseDto> listarAnios() {
    return calendario.listarAnios();
  }

  @PutMapping("/anios/{id}/cierre")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "calendario.cerrarAnio")
  public void cerrarAnio(@PathVariable @Positive Short id) {
    calendario.cerrarAnio(id);
  }

  @PostMapping("/periodos")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "calendario.crearPeriodo")
  public PeriodoResponseDto crearPeriodo(@Valid @RequestBody CrearPeriodoRequestDto r) {
    return calendario.crearPeriodo(r);
  }

  @GetMapping("/periodos")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "calendario.listarPeriodos")
  public List<PeriodoResponseDto> listarPeriodos(@RequestParam Short anioId) {
    return calendario.listarPeriodos(anioId);
  }

  @PutMapping("/periodos/{id}/cierre")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "calendario.cerrarPeriodo")
  public void cerrarPeriodo(@PathVariable @Positive Short id) {
    calendario.cerrarPeriodo(id);
  }

  @GetMapping("/niveles")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "estructura.listarNiveles")
  public List<NivelResponseDto> listarNiveles() {
    return estructura.listarNiveles();
  }

  @GetMapping("/grados")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "estructura.listarGrados")
  public List<GradoResponseDto> listarGrados(@RequestParam Short nivelId) {
    return estructura.listarGrados(nivelId);
  }

  @PostMapping("/grados")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "estructura.crearGrado")
  public GradoResponseDto crearGrado(@Valid @RequestBody CrearGradoRequestDto r) {
    return estructura.crearGrado(r);
  }

  @PostMapping("/secciones")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "estructura.crearSeccion")
  public SeccionResponseDto crearSeccion(@Valid @RequestBody CrearSeccionRequestDto r) {
    return estructura.crearSeccion(r);
  }

  @GetMapping("/secciones")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "estructura.listarSecciones")
  public List<SeccionResponseDto> listarSecciones(
      @RequestParam Short anioId, @RequestParam(required = false) Short nivelId) {
    return estructura.listarSecciones(anioId, nivelId);
  }

  @GetMapping("/secciones/{id}/vacantes")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "cupos.vacantes")
  public VacantesSeccionResponseDto consultarVacantes(@PathVariable @Positive Integer id) {
    return cupos.vacantes(id);
  }
}
