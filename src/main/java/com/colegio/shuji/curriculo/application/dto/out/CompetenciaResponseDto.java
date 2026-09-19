package com.colegio.shuji.curriculo.application.dto.out;

public record CompetenciaResponseDto(
    Short id, Short areaId, Short numeroOrden, String nombre, String descripcion) {}
