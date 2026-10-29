package com.colegio.shuji.curriculo.application.port.in;

import com.colegio.shuji.curriculo.application.dto.in.ProgramarHorarioRequestDto;
import com.colegio.shuji.curriculo.application.dto.in.ProgramarMallaHorariaGradoRequestDto;
import com.colegio.shuji.curriculo.application.dto.out.HorarioSeccionResponseDto;
import com.colegio.shuji.curriculo.application.dto.out.MallaHorariaResponseDto;
import com.colegio.shuji.curriculo.application.dto.out.MallaHorariaGradoResponseDto;
import java.util.List;

public interface GenerarMallaHorariaUseCase {
  HorarioSeccionResponseDto programar(ProgramarHorarioRequestDto r);

  HorarioSeccionResponseDto actualizar(Long id, ProgramarHorarioRequestDto r);

  HorarioSeccionResponseDto retirar(Long id);

  MallaHorariaResponseDto porSeccion(Integer id, Short anioId);

  MallaHorariaResponseDto porDocente(Long id, Short anioId);

  MallaHorariaGradoResponseDto programarGrado(ProgramarMallaHorariaGradoRequestDto request);

  MallaHorariaGradoResponseDto actualizarGrado(Long id, ProgramarMallaHorariaGradoRequestDto request);

  MallaHorariaGradoResponseDto retirarGrado(Long id);

  List<MallaHorariaGradoResponseDto> porGrado(Short gradoId, Short anioId);
}
