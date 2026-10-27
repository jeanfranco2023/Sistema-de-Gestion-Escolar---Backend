package com.colegio.shuji.matricula.application.dto.out;

public record HijoPortalResponseDto(Long estudianteId, Long matriculaId, Short anioLectivoId, Short anio,
    Integer seccionId, String nombres, String apellidoPaterno, String apellidoMaterno,
    String numeroDocumento, String nivel, String grado, String seccion) {}
