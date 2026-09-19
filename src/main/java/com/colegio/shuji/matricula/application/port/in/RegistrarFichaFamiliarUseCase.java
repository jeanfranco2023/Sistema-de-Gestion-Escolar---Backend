package com.colegio.shuji.matricula.application.port.in;

import com.colegio.shuji.matricula.application.dto.in.*;
import com.colegio.shuji.matricula.application.dto.out.*;

public interface RegistrarFichaFamiliarUseCase {
  EstudianteResponseDto registrarEstudiante(RegistrarEstudianteRequestDto r);

  ApoderadoResponseDto registrarApoderado(RegistrarApoderadoRequestDto r);

  EstudianteApoderadoResponseDto vincular(VincularApoderadoRequestDto r);

  EstudianteResponseDto consultarEstudiante(Long id);

  ApoderadoResponseDto consultarApoderado(Long id);
}
