package com.colegio.shuji.academico.application.port.in;

import com.colegio.shuji.academico.application.dto.in.CrearGradoRequestDto;
import com.colegio.shuji.academico.application.dto.in.CrearSeccionRequestDto;
import com.colegio.shuji.academico.application.dto.out.GradoResponseDto;
import com.colegio.shuji.academico.application.dto.out.NivelResponseDto;
import com.colegio.shuji.academico.application.dto.out.SeccionResponseDto;
import java.util.List;

public interface GestionarSeccionesUseCase {
  SeccionResponseDto crearSeccion(CrearSeccionRequestDto r);

  List<SeccionResponseDto> listarSecciones(Short anioId, Short nivelId);

  List<NivelResponseDto> listarNiveles();

  List<GradoResponseDto> listarGrados(Short nivelId);

  GradoResponseDto crearGrado(CrearGradoRequestDto r);
}
