package com.colegio.shuji.matricula.application.dto.out;

import com.colegio.shuji.matricula.domain.enums.EstadoSolicitudMatriculaPublica;
import com.colegio.shuji.matricula.domain.enums.ModalidadIngreso;
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
    Short gradoId,
    Integer seccionId,
    List<DocumentoSolicitudResponseDto> documentos,
    OffsetDateTime creadaAt,
    Long estudianteId,
    Long matriculaId,
    boolean requiereAutorizacionEdad,
    String motivoAutorizacionEdad,
    Long autorizadoPorUsuarioId,
    OffsetDateTime fechaAutorizacionEdad,
    ModalidadIngreso modalidadIngreso,
    String colegioProcedencia) {

  public SolicitudMatriculaPublicaAdminResponseDto(
      UUID id,
      EstadoSolicitudMatriculaPublica estado,
      String nombreEstudiante,
      String documentoEstudiante,
      String nombreApoderado,
      String celularApoderado,
      String emailApoderado,
      Short anioLectivoId,
      Short gradoId,
      Integer seccionId,
      List<DocumentoSolicitudResponseDto> documentos,
      OffsetDateTime creadaAt,
      Long estudianteId,
      Long matriculaId,
      boolean requiereAutorizacionEdad,
      String motivoAutorizacionEdad,
      Long autorizadoPorUsuarioId,
      OffsetDateTime fechaAutorizacionEdad) {
    this(id, estado, nombreEstudiante, documentoEstudiante, nombreApoderado, celularApoderado,
        emailApoderado, anioLectivoId, gradoId, seccionId, documentos, creadaAt,
        estudianteId, matriculaId, requiereAutorizacionEdad, motivoAutorizacionEdad,
        autorizadoPorUsuarioId, fechaAutorizacionEdad, ModalidadIngreso.REGULAR, null);
  }
}
