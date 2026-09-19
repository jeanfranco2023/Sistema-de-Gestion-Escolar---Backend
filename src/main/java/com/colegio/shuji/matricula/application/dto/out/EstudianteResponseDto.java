package com.colegio.shuji.matricula.application.dto.out;

import com.colegio.shuji.matricula.domain.enums.Genero;
import com.colegio.shuji.matricula.domain.enums.OrigenRegistro;
import com.colegio.shuji.matricula.domain.enums.TipoDocumento;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record EstudianteResponseDto(
    Long id,
    UUID uuid,
    TipoDocumento tipoDocumento,
    String numeroDocumento,
    String nombres,
    String apellidoPaterno,
    String apellidoMaterno,
    LocalDate fechaNacimiento,
    Genero genero,
    String codigoEstudianteSiagie,
    Boolean validadoReniec,
    OrigenRegistro origenRegistro,
    String grupoSanguineo,
    String alergiasCondiciones,
    Boolean activo,
    OffsetDateTime createdAt) {}
