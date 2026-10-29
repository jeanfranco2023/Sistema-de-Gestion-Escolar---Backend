package com.colegio.shuji.matricula.infrastructure.controller.rest;

import com.colegio.shuji.matricula.application.port.in.ConsultarPortalFamiliarUseCase;
import com.colegio.shuji.matricula.application.dto.out.HijoPortalResponseDto;
import com.colegio.shuji.asistencia.application.dto.out.AsistenciaAulaResponseDto;
import com.colegio.shuji.academico.application.dto.out.PeriodoResponseDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portal-familiar")
@RequiredArgsConstructor
public class PortalFamiliarController {
  private final ConsultarPortalFamiliarUseCase consulta;
  private final com.colegio.shuji.academico.application.port.in.GestionarAnioLectivoUseCase calendario;

  @GetMapping("/periodos-seguimiento")
  @PreAuthorize("hasAnyRole('DIRECCION','TUTOR','AUXILIAR')")
  public List<PeriodoResponseDto> periodosSeguimiento(@org.springframework.web.bind.annotation.RequestParam Short anioId) {
    return consulta.periodosSeguimiento(anioId);
  }
  @GetMapping("/hijos-propios")
  @PreAuthorize("hasRole('APODERADO')")
  public List<HijoPortalResponseDto> hijosPropios() { return consulta.hijosPropios(); }

  @GetMapping("/matriculas/{id}/calificaciones")
  @PreAuthorize("hasAnyRole('DIRECCION','APODERADO')")
  public List<com.colegio.shuji.evaluacion.application.dto.out.CalificacionPortalResponseDto> calificaciones(@PathVariable Long id) {
    return consulta.calificaciones(id);
  }
  @GetMapping("/hijos")
  @PreAuthorize("hasAnyRole('DIRECCION','APODERADO')")
  public List<HijoPortalResponseDto> hijos() { return consulta.hijos(); }

  @GetMapping("/alumnos-seguimiento")
  @PreAuthorize("hasAnyRole('DIRECCION','TUTOR','AUXILIAR')")
  public List<HijoPortalResponseDto> seguimiento() { return consulta.alumnosSeguimiento(); }

  @GetMapping("/seguimiento/riesgo-calificaciones")
  @PreAuthorize("hasRole('TUTOR')")
  public List<com.colegio.shuji.evaluacion.application.dto.out.CalificacionRiesgoTutorResponseDto>
      calificacionesRiesgoTutor() {
    return consulta.calificacionesRiesgoTutor();
  }

  @GetMapping("/matriculas/{id}/asistencia")
  @PreAuthorize("hasAnyRole('DIRECCION','APODERADO')")
  public List<AsistenciaAulaResponseDto> asistencia(@PathVariable Long id) { return consulta.asistencia(id); }

  @GetMapping("/matriculas/{id}/periodos")
  @PreAuthorize("hasAnyRole('DIRECCION','APODERADO')")
  public List<PeriodoResponseDto> periodos(@PathVariable Long id) { return consulta.periodos(id); }
}
