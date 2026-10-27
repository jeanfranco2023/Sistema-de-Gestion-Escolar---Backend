package com.colegio.shuji.matricula.application.dto.out;

import java.time.LocalDate;
import java.time.OffsetDateTime;
public record SuplenciaResponseDto(Long id,Long estudianteId,Long apoderadoTitularId,String numeroDocumento,
    String nombres,String apellidoPaterno,String apellidoMaterno,String parentesco,String celular,
    LocalDate fechaInicio,LocalDate fechaFin,String estado,String motivoResolucion,
    Long autorizadoPorUsuarioId,OffsetDateTime fechaSolicitud,OffsetDateTime fechaResolucion) {}
