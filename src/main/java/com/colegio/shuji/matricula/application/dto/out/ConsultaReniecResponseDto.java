package com.colegio.shuji.matricula.application.dto.out;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Datos de identidad obtenidos desde RENIEC")
public record ConsultaReniecResponseDto(
    @Schema(description = "Número de documento (DNI)", example = "12345678")
    String numeroDocumento,

    @Schema(description = "Nombres de la persona", example = "Juan")
    String nombres,

    @Schema(description = "Primer apellido", example = "Perez")
    String apellidoPaterno,

    @Schema(description = "Segundo apellido", example = "Gomez")
    String apellidoMaterno
) {}
