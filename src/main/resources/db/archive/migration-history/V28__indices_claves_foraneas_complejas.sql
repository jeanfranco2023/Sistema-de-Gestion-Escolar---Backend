-- Índices compuestos para claves foráneas usadas en las relaciones del padrón, notas y horarios.
-- Se omite obligaciones_pago porque pertenece al flujo de pagos, que permanece sin cambios.
CREATE INDEX IF NOT EXISTS idx_asignaciones_seccion_anio_nivel_fk
    ON public.asignaciones_docentes (seccion_id, anio_lectivo_id, nivel_id);

CREATE INDEX IF NOT EXISTS idx_calificaciones_asignacion_contexto_fk
    ON public.calificaciones_cneb
       (asignacion_docente_id, seccion_id, area_curricular_id, anio_lectivo_id, docente_usuario_id);

CREATE INDEX IF NOT EXISTS idx_calificaciones_competencia_area_fk
    ON public.calificaciones_cneb (competencia_id, area_curricular_id);

CREATE INDEX IF NOT EXISTS idx_calificaciones_matricula_contexto_fk
    ON public.calificaciones_cneb (matricula_id, anio_lectivo_id, seccion_id);

CREATE INDEX IF NOT EXISTS idx_calificaciones_periodo_anio_fk
    ON public.calificaciones_cneb (periodo_academico_id, anio_lectivo_id);

CREATE INDEX IF NOT EXISTS idx_horarios_asignacion_contexto_fk
    ON public.horarios_seccion (asignacion_docente_id, anio_lectivo_id, seccion_id, docente_usuario_id);

CREATE INDEX IF NOT EXISTS idx_matriculas_seccion_anio_fk
    ON public.matriculas (seccion_id, anio_lectivo_id);

CREATE INDEX IF NOT EXISTS idx_refuerzos_periodo_anio_fk
    ON public.sesiones_refuerzo (periodo_academico_id, anio_lectivo_id);

CREATE INDEX IF NOT EXISTS idx_documentos_version_actual_fk
    ON public.solicitudes_matricula_documentos (version_id, solicitud_id, tipo);
