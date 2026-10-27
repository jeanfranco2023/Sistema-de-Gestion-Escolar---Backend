package com.colegio.shuji.matricula.application.port.out;

import com.colegio.shuji.matricula.application.dto.out.HijoPortalResponseDto;
import com.colegio.shuji.asistencia.application.dto.out.AsistenciaAulaResponseDto;
import java.util.List;

public interface ConsultaPortalDatosPort {
  List<HijoPortalResponseDto> hijos(Long usuarioId);
  List<AsistenciaAulaResponseDto> asistencia(Long matriculaId);
  List<com.colegio.shuji.evaluacion.application.dto.out.CalificacionPortalResponseDto> calificaciones(Long matriculaId);
}
