package com.colegio.shuji.curriculo.application.dto.out;
public record AsignacionHorarioOpcionDto(Long asignacionId, Short anioLectivoId, Short anio,
    Integer seccionId, Short nivelId, Short gradoId, String nivel, String grado, String letra,
    Short areaId, String areaNombre, Long docenteUsuarioId, String docenteNombre, String aulaCodigo) {}
