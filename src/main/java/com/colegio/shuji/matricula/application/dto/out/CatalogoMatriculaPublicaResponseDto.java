package com.colegio.shuji.matricula.application.dto.out;

import com.colegio.shuji.academico.application.dto.out.AnioLectivoResponseDto;
import java.util.List;

public record CatalogoMatriculaPublicaResponseDto(
    List<AnioLectivoResponseDto> anios, List<SeccionMatriculaPublicaOpcionDto> secciones) {}
