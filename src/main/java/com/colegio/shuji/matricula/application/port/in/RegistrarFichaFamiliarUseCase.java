package com.colegio.shuji.matricula.application.port.in;

import com.colegio.shuji.matricula.application.dto.in.RegistrarApoderadoRequestDto;
import com.colegio.shuji.matricula.application.dto.in.RegistrarEstudianteRequestDto;
import com.colegio.shuji.matricula.application.dto.in.VincularApoderadoRequestDto;
import com.colegio.shuji.matricula.application.dto.out.ApoderadoResponseDto;
import com.colegio.shuji.matricula.application.dto.out.EstudianteApoderadoResponseDto;
import com.colegio.shuji.matricula.application.dto.out.EstudianteResponseDto;
import com.colegio.shuji.matricula.application.port.out.ReniecServicePort.Identidad;
import java.util.List;
import java.util.Optional;

public interface RegistrarFichaFamiliarUseCase {
  EstudianteResponseDto registrarEstudiante(RegistrarEstudianteRequestDto r);

  ApoderadoResponseDto registrarApoderado(RegistrarApoderadoRequestDto r);

  EstudianteApoderadoResponseDto vincular(VincularApoderadoRequestDto r);

  EstudianteResponseDto consultarEstudiante(Long id);

  List<EstudianteResponseDto> listarEstudiantes();

  ApoderadoResponseDto consultarApoderado(Long id);

  Optional<Identidad> consultarDni(String dni);
}
