package com.colegio.shuji.usuario.application.dto.in;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Solicitud formal de corrección de DNI de un usuario institucional")
public record CorregirDniRequestDto(
    @Schema(description = "Nuevo número de DNI (8 dígitos)", example = "71234567")
    @NotBlank(message = "El nuevo DNI es obligatorio")
    @Pattern(regexp = "^\\d{8}$", message = "El DNI debe contener exactamente 8 dígitos numéricos")
    String nuevoDni,

    @Schema(description = "Motivo de la corrección para auditoría", example = "Error tipográfico en digitación de contrato")
    @NotBlank(message = "El motivo de la corrección es obligatorio para fines de auditoría")
    String motivo
) {}
