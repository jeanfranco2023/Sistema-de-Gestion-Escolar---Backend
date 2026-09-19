package com.colegio.shuji.evaluacion.infrastructure.controller.rest;

import com.colegio.shuji.evaluacion.application.dto.in.ProgramarRefuerzoRequestDto;
import com.colegio.shuji.evaluacion.application.dto.in.RegistrarAsistenciaRefuerzoRequestDto;
import com.colegio.shuji.evaluacion.application.dto.out.InscripcionRefuerzoResponseDto;
import com.colegio.shuji.evaluacion.application.dto.out.SesionRefuerzoResponseDto;
import com.colegio.shuji.evaluacion.application.port.in.DerivarAlumnosRefuerzoUseCase;
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
@RequestMapping("/api/v1/evaluacion/refuerzo")
@Validated
@RequiredArgsConstructor
@Tag(name = "Evaluacion")
public class RefuerzoController {
  private final DerivarAlumnosRefuerzoUseCase refuerzo;

  @PostMapping("")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','DOCENTE')")
  @Operation(summary = "refuerzo.programar")
  public SesionRefuerzoResponseDto programarSesion(@Valid @RequestBody ProgramarRefuerzoRequestDto r) {
    return refuerzo.programar(r);
  }

  @GetMapping("")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','DOCENTE')")
  @Operation(summary = "refuerzo.sesiones")
  public List<SesionRefuerzoResponseDto> listarSesiones(@RequestParam Short periodoId) {
    return refuerzo.sesiones(periodoId);
  }

  @PostMapping("/{id}/derivacion")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','DOCENTE')")
  @Operation(summary = "refuerzo.derivar")
  public List<InscripcionRefuerzoResponseDto> derivarEstudiantes(@PathVariable @Positive Long id) {
    return refuerzo.derivar(id);
  }

  @PostMapping("/asistencia")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','DOCENTE')")
  @Operation(summary = "refuerzo.registrarAsistencia")
  public InscripcionRefuerzoResponseDto registrarAsistenciaRefuerzo(
      @Valid @RequestBody RegistrarAsistenciaRefuerzoRequestDto r) {
    return refuerzo.registrarAsistencia(r);
  }
}
