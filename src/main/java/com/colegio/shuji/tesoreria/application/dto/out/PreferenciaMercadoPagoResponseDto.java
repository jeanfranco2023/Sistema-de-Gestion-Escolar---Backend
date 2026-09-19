package com.colegio.shuji.tesoreria.application.dto.out;

public record PreferenciaMercadoPagoResponseDto(
    String preferenceId,
    String initPoint,
    String sandboxInitPoint,
    String publicKey) {}
