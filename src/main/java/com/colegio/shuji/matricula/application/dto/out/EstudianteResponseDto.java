package com.colegio.shuji.matricula.application.dto.out;

import com.colegio.shuji.matricula.domain.enums.*;
import java.time.*;
import java.util.*;

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
