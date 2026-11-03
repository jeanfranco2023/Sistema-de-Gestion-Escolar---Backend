package com.colegio.shuji.evaluacion.application.dto.out;

public record RankingEstudianteResponseDto(
    Integer puesto,
    Long matriculaId,
    Long estudianteId,
    String tipoDocumento,
    String numeroDocumento,
    String nombres,
    String apellidos,
    String nombreCompleto,
    Double puntajePromedioInterno,
    Double porcentajeAsistencia,
    Integer totalIncidencias,
    Integer cantAd,
    Integer cantA,
    Integer cantB,
    Integer cantC,
    String mencionHonor,
    String beneficioInstitucional
) {}
