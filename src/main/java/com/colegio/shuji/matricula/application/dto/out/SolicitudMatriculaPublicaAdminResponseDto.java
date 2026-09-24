package com.colegio.shuji.matricula.application.dto.out;

import com.colegio.shuji.matricula.domain.enums.EstadoSolicitudMatriculaPublica;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SolicitudMatriculaPublicaAdminResponseDto(
    UUID id,
    EstadoSolicitudMatriculaPublica estado,
    String nombreEstudiante,
    String documentoEstudiante,
    String nombreApoderado,
    String celularApoderado,
    String emailApoderado,
    Short anioLectivoId,
    Integer seccionId,
    List<DocumentoSolicitudResponseDto> documentos,
    OffsetDateTime creadaAt,
    Long estudianteId,
    Long matriculaId) {}
