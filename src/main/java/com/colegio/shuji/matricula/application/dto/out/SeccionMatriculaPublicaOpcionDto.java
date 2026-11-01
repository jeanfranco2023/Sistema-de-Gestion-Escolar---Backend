package com.colegio.shuji.matricula.application.dto.out;

public record SeccionMatriculaPublicaOpcionDto(
    Integer id,
    Short anioLectivoId,
    Short gradoId,
    String nivel,
    String grado,
    String letra,
    int vacantesDisponibles,
    int edadMinimaAlCorte,
    int edadMaximaAlCorte) {}
