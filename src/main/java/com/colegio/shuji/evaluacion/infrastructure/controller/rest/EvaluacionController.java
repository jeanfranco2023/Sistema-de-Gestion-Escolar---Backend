package com.colegio.shuji.evaluacion.infrastructure.controller.rest;

import com.colegio.shuji.evaluacion.application.dto.in.RegistrarCalificacionesMasivasRequestDto;
import com.colegio.shuji.evaluacion.application.dto.out.CalificacionResponseDto;
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

  @PostMapping("/cneb")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','DOCENTE')")
  @Operation(summary = "registro.registrar")
  public List<CalificacionResponseDto> registrarCalificacionesMasivas(
      @Valid @RequestBody RegistrarCalificacionesMasivasRequestDto r) {
    return registro.registrar(r);
  }

  @GetMapping("/libreta/{matriculaId}")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','DOCENTE')")
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
