package com.colegio.shuji.comunicado.application.dto.out;

public record MetricasLecturaResponseDto(
    Long comunicadoId, long destinatarios, long leidos, long acuses) {}
