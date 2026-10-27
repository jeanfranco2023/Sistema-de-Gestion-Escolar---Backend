package com.colegio.shuji.matricula.application.port.in;

import com.colegio.shuji.matricula.application.dto.out.HijoPortalResponseDto;
import com.colegio.shuji.asistencia.application.dto.out.AsistenciaAulaResponseDto;
import com.colegio.shuji.academico.application.dto.out.PeriodoResponseDto;
import java.util.List;

public interface ConsultarPortalFamiliarUseCase {
  List<HijoPortalResponseDto> hijos();
  List<HijoPortalResponseDto> hijosPropios();
  List<HijoPortalResponseDto> alumnosSeguimiento();
  List<AsistenciaAulaResponseDto> asistencia(Long matriculaId);
  List<PeriodoResponseDto> periodos(Long matriculaId);
  List<com.colegio.shuji.evaluacion.application.dto.out.CalificacionPortalResponseDto> calificaciones(Long matriculaId);
}
