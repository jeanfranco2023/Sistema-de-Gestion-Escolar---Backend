package com.colegio.shuji.academico.application.dto.out;

public record AulaResponseDto(
    Integer id, String codigo, String nombre, String ubicacion, Short capacidad, Boolean activa) {}
