package com.colegio.shuji.matricula.application.port.in;

import com.colegio.shuji.matricula.application.dto.in.CambiarEstadoApoderadoRequestDto;
import com.colegio.shuji.matricula.application.dto.in.CambiarEstadoEstudianteRequestDto;
import com.colegio.shuji.matricula.application.dto.in.RegistrarApoderadoRequestDto;
import com.colegio.shuji.matricula.application.dto.in.RegistrarEstudianteRequestDto;
import com.colegio.shuji.matricula.application.dto.in.VincularApoderadoRequestDto;
import com.colegio.shuji.matricula.application.dto.out.ApoderadoFichaResponseDto;
import com.colegio.shuji.matricula.application.dto.out.ApoderadoResponseDto;
import com.colegio.shuji.matricula.application.dto.out.EstudianteApoderadoResponseDto;
import com.colegio.shuji.matricula.application.dto.out.EstudianteResponseDto;
import com.colegio.shuji.matricula.application.port.out.ReniecServicePort.Identidad;
import com.colegio.shuji.usuario.application.dto.in.CorregirDniRequestDto;
import java.util.List;
import java.util.Optional;

public interface RegistrarFichaFamiliarUseCase {
  EstudianteResponseDto registrarEstudiante(RegistrarEstudianteRequestDto r);

  ApoderadoResponseDto registrarApoderado(RegistrarApoderadoRequestDto r);

  List<ApoderadoResponseDto> listarApoderados();

  List<ApoderadoFichaResponseDto> listarApoderadosPorEstudiante(Long estudianteId);

  EstudianteApoderadoResponseDto vincular(VincularApoderadoRequestDto r);

  EstudianteResponseDto consultarEstudiante(Long id);

  List<EstudianteResponseDto> listarEstudiantes();

  ApoderadoResponseDto consultarApoderado(Long id);

  Optional<Identidad> consultarDni(String dni);

  EstudianteResponseDto corregirDniEstudiante(Long id, CorregirDniRequestDto request);

  EstudianteResponseDto cambiarEstadoEstudiante(Long id, CambiarEstadoEstudianteRequestDto request);

  ApoderadoResponseDto corregirDniApoderado(Long id, CorregirDniRequestDto request);

  ApoderadoResponseDto cambiarEstadoApoderado(Long id, CambiarEstadoApoderadoRequestDto request);
}
