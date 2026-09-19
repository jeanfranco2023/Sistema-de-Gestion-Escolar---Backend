package com.colegio.shuji.matricula.application.dto.out;

import com.colegio.shuji.matricula.domain.enums.OrigenRegistro;
import com.colegio.shuji.matricula.domain.enums.TipoDocumento;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ApoderadoResponseDto(
    Long id,
    UUID uuid,
    Long usuarioId,
    TipoDocumento tipoDocumento,
    String numeroDocumento,
    String nombres,
    String apellidoPaterno,
    String apellidoMaterno,
    String celular,
    String email,
    String direccion,
    String ubigeoInei,
    Boolean validadoReniec,
    OrigenRegistro origenRegistro,
    OffsetDateTime createdAt) {}
