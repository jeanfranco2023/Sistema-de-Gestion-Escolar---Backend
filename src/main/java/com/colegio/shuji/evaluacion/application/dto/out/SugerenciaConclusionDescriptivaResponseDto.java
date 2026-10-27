package com.colegio.shuji.evaluacion.application.dto.out;

public record SugerenciaConclusionDescriptivaResponseDto(
    String conclusionDescriptiva, String aspectoMejora, String recomendacionApoderado, boolean generadoPorIa) {}
