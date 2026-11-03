package com.colegio.shuji.evaluacion.application.port.in;

import com.colegio.shuji.evaluacion.application.dto.out.CuadroHonorResponseDto;

public interface ConsultarRankingAcademicoUseCase {

  CuadroHonorResponseDto obtenerRankingSeccion(Integer seccionId);

  CuadroHonorResponseDto obtenerCuadroHonor(Short gradoId, Short anioLectivoId);
}
