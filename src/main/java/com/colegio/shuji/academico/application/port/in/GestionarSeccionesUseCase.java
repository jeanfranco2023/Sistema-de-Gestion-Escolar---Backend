package com.colegio.shuji.academico.application.port.in;

import com.colegio.shuji.academico.application.dto.in.*;
import com.colegio.shuji.academico.application.dto.out.*;
import java.util.*;

public interface GestionarSeccionesUseCase {
  SeccionResponseDto crearSeccion(CrearSeccionRequestDto r);

  List<SeccionResponseDto> listarSecciones(Short anioId, Short nivelId);

  List<NivelResponseDto> listarNiveles();

  List<GradoResponseDto> listarGrados(Short nivelId);

  GradoResponseDto crearGrado(CrearGradoRequestDto r);
}
