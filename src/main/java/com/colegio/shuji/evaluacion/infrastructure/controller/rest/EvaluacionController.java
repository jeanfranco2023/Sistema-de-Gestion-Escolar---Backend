package com.colegio.shuji.evaluacion.infrastructure.controller.rest;

import com.colegio.shuji.evaluacion.application.dto.in.RegistrarCalificacionesMasivasRequestDto;
import com.colegio.shuji.evaluacion.application.dto.out.CalificacionResponseDto;
import com.colegio.shuji.evaluacion.application.dto.out.FilaCalificacionResponseDto;
import com.colegio.shuji.evaluacion.application.dto.out.EstudiantesEnRiesgoResponseDto;
import com.colegio.shuji.evaluacion.application.dto.out.LibretaNotasResponseDto;
import com.colegio.shuji.evaluacion.application.port.in.ConsultarLibretaNotasUseCase;
import com.colegio.shuji.evaluacion.application.port.in.RegistrarEvaluacionCnebUseCase;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/evaluacion")
@Validated
@RequiredArgsConstructor
@Tag(name = "Evaluacion")
public class EvaluacionController {
  private final RegistrarEvaluacionCnebUseCase registro;
  private final ConsultarLibretaNotasUseCase consulta;
  private final com.colegio.shuji.evaluacion.application.port.in.SugerirConclusionDescriptivaUseCase sugerirIa;
  private final com.colegio.shuji.evaluacion.application.port.in.ImportarCalificacionesUseCase importacion;
  private final com.colegio.shuji.evaluacion.application.port.in.GenerarLibretaPdfUseCase pdf;
  private final com.colegio.shuji.evaluacion.application.port.in.DescargarPlantillaCnebUseCase plantilla;

  @GetMapping("/cneb/plantilla")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','DOCENTE')")
  public org.springframework.http.ResponseEntity<byte[]> plantilla(@RequestParam Long asignacionDocenteId,
      @RequestParam Short periodoId, @RequestParam Short competenciaId) {
    return org.springframework.http.ResponseEntity.ok()
        .contentType(org.springframework.http.MediaType.parseMediaType("text/csv;charset=UTF-8"))
        .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"plantilla_cneb.csv\"")
        .body(plantilla.descargar(asignacionDocenteId, periodoId, competenciaId));
  }

  @GetMapping("/libreta/{matriculaId}/pdf")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','DOCENTE','APODERADO')")
  public org.springframework.http.ResponseEntity<byte[]> descargarLibretaPdf(@PathVariable @Positive Long matriculaId) {
    return org.springframework.http.ResponseEntity.ok().contentType(org.springframework.http.MediaType.APPLICATION_PDF)
        .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"libreta_" + matriculaId + ".pdf\"")
        .body(pdf.generar(matriculaId));
  }

  @PostMapping(value = "/cneb/carga-masiva", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','DOCENTE')")
  public com.colegio.shuji.evaluacion.application.dto.out.ImportacionCalificacionesResponseDto cargarPlanilla(
      @RequestParam("archivo") org.springframework.web.multipart.MultipartFile archivo,
      @RequestParam Short periodoId, @RequestParam Integer seccionId, @RequestParam Long asignacionDocenteId,
      @RequestParam(defaultValue = "false") boolean confirmar) throws java.io.IOException {
    if (archivo.isEmpty() || archivo.getSize() > 5 * 1024 * 1024) {
      throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Planilla vacía o mayor a 5 MB");
    }
    var filas = com.colegio.shuji.evaluacion.infrastructure.adapter.LectorPlanillaCalificaciones.leer(archivo.getBytes(), archivo.getOriginalFilename());
    return importacion.importar(filas, periodoId, seccionId, asignacionDocenteId, confirmar);
  }

  @PostMapping("/ia/sugerir-conclusion")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','DOCENTE')")
  @Operation(summary = "evaluacion.sugerirIa")
  public com.colegio.shuji.evaluacion.application.dto.out.SugerenciaConclusionDescriptivaResponseDto sugerirConclusionIa(
      @Valid @RequestBody com.colegio.shuji.evaluacion.application.dto.in.SugerirConclusionDescriptivaRequestDto request) {
    return sugerirIa.sugerir(request);
  }

  @PostMapping("/cneb")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','DOCENTE')")
  @Operation(summary = "registro.registrar")
  public List<CalificacionResponseDto> registrarCalificacionesMasivas(
      @Valid @RequestBody RegistrarCalificacionesMasivasRequestDto r) {
    return registro.registrar(r);
  }

  @GetMapping("/cneb")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','DOCENTE')")
  public List<CalificacionResponseDto> listarCalificaciones(
      @RequestParam Long asignacionDocenteId, @RequestParam Short periodoAcademicoId) {
    return registro.listar(asignacionDocenteId, periodoAcademicoId);
  }

  @GetMapping("/cneb/matriz")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','DOCENTE')")
  public List<FilaCalificacionResponseDto> listarMatriz(
      @RequestParam Long asignacionDocenteId, @RequestParam Short periodoAcademicoId,
      @RequestParam Short competenciaId) {
    return registro.listarMatriz(asignacionDocenteId, periodoAcademicoId, competenciaId);
  }

  @GetMapping("/libreta/{matriculaId}")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','DOCENTE','APODERADO')")
  @Operation(summary = "consulta.libreta")
  public LibretaNotasResponseDto consultarLibretaNotas(@PathVariable @Positive Long matriculaId) {
    return consulta.libreta(matriculaId);
  }

  @GetMapping("/riesgo")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','DOCENTE')")
  @Operation(summary = "consulta.riesgo")
  public EstudiantesEnRiesgoResponseDto consultarEstudiantesEnRiesgo(@RequestParam Short periodoId) {
    return consulta.riesgo(periodoId);
  }
}
