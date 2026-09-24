package com.colegio.shuji.matricula.application.dto.out;

public record SeccionMatriculaPublicaOpcionDto(
    Integer id,
    Short anioLectivoId,
    String nivel,
    String grado,
    String letra,
    int vacantesDisponibles) {}
