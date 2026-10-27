package com.colegio.shuji.matricula.application.service;

import static com.colegio.shuji.shared.domain.model.Reglas.requerido;

import com.colegio.shuji.matricula.application.port.in.ConsultarPortalFamiliarUseCase;
import com.colegio.shuji.matricula.application.port.out.ConsultaPortalDatosPort;
import com.colegio.shuji.matricula.application.port.out.AccesoFamiliarPort;
import com.colegio.shuji.matricula.application.port.out.MatriculaRepositoryPort;
import com.colegio.shuji.matricula.application.dto.out.HijoPortalResponseDto;
import com.colegio.shuji.academico.application.dto.out.PeriodoResponseDto;
import com.colegio.shuji.academico.application.port.in.GestionarAnioLectivoUseCase;
import com.colegio.shuji.asistencia.application.dto.out.AsistenciaAulaResponseDto;
import com.colegio.shuji.shared.application.port.out.ActorActualPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortalFamiliarService implements ConsultarPortalFamiliarUseCase {
  private final ConsultaPortalDatosPort datos;
  private final AccesoFamiliarPort acceso;
  private final MatriculaRepositoryPort matriculas;
  private final GestionarAnioLectivoUseCase calendario;
  private final ActorActualPort actor;

  private boolean gestion() { return actor.tieneRol("DIRECCION") || actor.tieneRol("SECRETARIA"); }

  public List<HijoPortalResponseDto> hijos() { return datos.hijos(gestion() ? null : actor.usuarioId()); }
  public List<HijoPortalResponseDto> hijosPropios() { return datos.hijos(actor.usuarioId()); }

  public List<HijoPortalResponseDto> alumnosSeguimiento() {
    if (!gestion() && !actor.tieneRol("TUTOR") && !actor.tieneRol("AUXILIAR")) throw new AccessDeniedException("Se requiere un rol de seguimiento escolar");
    return datos.hijos(null);
  }

  private com.colegio.shuji.matricula.domain.model.Matricula autorizar(Long id) {
    var matricula = requerido(matriculas.buscarPorId(id));
    if (!gestion() && !acceso.tieneCustodia(matricula.getEstudianteId(), actor.usuarioId()))
      throw new AccessDeniedException("La matrícula no corresponde a un hijo bajo tu custodia");
    return matricula;
  }

  public List<AsistenciaAulaResponseDto> asistencia(Long matriculaId) {
    autorizar(matriculaId); return datos.asistencia(matriculaId);
  }

  public List<PeriodoResponseDto> periodos(Long matriculaId) {
    var matricula = autorizar(matriculaId); return calendario.listarPeriodos(matricula.getAnioLectivoId());
  }

  public List<com.colegio.shuji.evaluacion.application.dto.out.CalificacionPortalResponseDto> calificaciones(Long matriculaId) {
    autorizar(matriculaId); return datos.calificaciones(matriculaId);
  }
}
