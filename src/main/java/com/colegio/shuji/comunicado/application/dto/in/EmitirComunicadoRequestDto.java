package com.colegio.shuji.comunicado.application.dto.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EmitirComunicadoRequestDto(
    @NotBlank @Size(max = 150) String titulo,
    @NotBlank @Size(max = 20000) String contenido,
    @NotNull Boolean requiereAcuse,
    @NotNull Short anioLectivoId,
    Short nivelId,
    Integer seccionId) {}
