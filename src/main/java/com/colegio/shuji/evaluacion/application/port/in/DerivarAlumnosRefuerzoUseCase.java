package com.colegio.shuji.evaluacion.application.port.in;

import com.colegio.shuji.evaluacion.application.dto.in.ProgramarRefuerzoRequestDto;
import com.colegio.shuji.evaluacion.application.dto.in.RegistrarAsistenciaRefuerzoRequestDto;
import com.colegio.shuji.evaluacion.application.dto.out.InscripcionRefuerzoDetalleResponseDto;
import com.colegio.shuji.evaluacion.application.dto.out.InscripcionRefuerzoResponseDto;
import com.colegio.shuji.evaluacion.application.dto.out.SesionRefuerzoResponseDto;
import com.colegio.shuji.evaluacion.application.dto.out.TableroRefuerzoResponseDto;
import java.util.List;

public interface DerivarAlumnosRefuerzoUseCase {
  SesionRefuerzoResponseDto programar(ProgramarRefuerzoRequestDto r);

  List<InscripcionRefuerzoResponseDto> derivar(Long sesionId);

  InscripcionRefuerzoResponseDto registrarAsistencia(RegistrarAsistenciaRefuerzoRequestDto r);

  List<SesionRefuerzoResponseDto> sesiones(Short periodoId);

  List<InscripcionRefuerzoDetalleResponseDto> inscripciones(Long sesionId);

  TableroRefuerzoResponseDto tablero(Short anioId, Short periodoId);
}
