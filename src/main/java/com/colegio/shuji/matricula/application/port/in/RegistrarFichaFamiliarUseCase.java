package com.colegio.shuji.matricula.application.port.in;

import com.colegio.shuji.matricula.application.dto.in.RegistrarApoderadoRequestDto;
import com.colegio.shuji.matricula.application.dto.in.RegistrarEstudianteRequestDto;
import com.colegio.shuji.matricula.application.dto.in.VincularApoderadoRequestDto;
import com.colegio.shuji.matricula.application.dto.out.ApoderadoResponseDto;
import com.colegio.shuji.matricula.application.dto.out.EstudianteApoderadoResponseDto;
import com.colegio.shuji.matricula.application.dto.out.EstudianteResponseDto;

public interface RegistrarFichaFamiliarUseCase {
  EstudianteResponseDto registrarEstudiante(RegistrarEstudianteRequestDto r);

  ApoderadoResponseDto registrarApoderado(RegistrarApoderadoRequestDto r);

  EstudianteApoderadoResponseDto vincular(VincularApoderadoRequestDto r);

  EstudianteResponseDto consultarEstudiante(Long id);

  ApoderadoResponseDto consultarApoderado(Long id);
}
