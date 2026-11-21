-- =============================================================================
-- V58: Índices compuestos/parciales para indicadores por bimestre y tablero de
--      refuerzo académico + sincronización automática obligatoria de logro C.
-- =============================================================================

-- 1. Índices de rendimiento para DashboardController.indicadores() por bimestre
CREATE INDEX IF NOT EXISTS idx_calificaciones_cneb_anio_periodo_mat_logro
    ON public.calificaciones_cneb (anio_lectivo_id, periodo_academico_id, matricula_id)
    INCLUDE (calificacion_cualitativa, area_curricular_id, docente_usuario_id, competencia_id);

CREATE INDEX IF NOT EXISTS idx_asistencias_aula_fecha_matricula_estado
    ON public.asistencias_aula (fecha_sesion, matricula_id)
    INCLUDE (estado);

CREATE INDEX IF NOT EXISTS idx_matriculas_anio_estado_seccion
    ON public.matriculas (anio_lectivo_id, estado_matricula, seccion_id)
    INCLUDE (estudiante_id);

-- 2. Índices parciales y compuestos para RefuerzoService.tablero() y auto-asignación
CREATE INDEX IF NOT EXISTS idx_calificaciones_cneb_riesgo_anio_periodo
    ON public.calificaciones_cneb (anio_lectivo_id, periodo_academico_id, area_curricular_id, docente_usuario_id)
    INCLUDE (matricula_id, seccion_id, competencia_id, calificacion_cualitativa)
    WHERE calificacion_cualitativa IN ('B', 'C');

CREATE INDEX IF NOT EXISTS idx_calificaciones_cneb_c_obligatorio
    ON public.calificaciones_cneb (periodo_academico_id, area_curricular_id, seccion_id, matricula_id)
    WHERE calificacion_cualitativa = 'C';

CREATE INDEX IF NOT EXISTS idx_sesiones_refuerzo_anio_periodo_area_docente
    ON public.sesiones_refuerzo (anio_lectivo_id, periodo_academico_id, area_curricular_id, docente_usuario_id, codigo_aula);

CREATE INDEX IF NOT EXISTS idx_inscripciones_refuerzo_sesion_estado
    ON public.inscripciones_refuerzo (sesion_refuerzo_id, estado_asistencia)
    INCLUDE (estudiante_id, calificacion_origen_id);

-- 3. Asignación automática obligatoria de sesión y tema de refuerzo para
--    calificaciones C previas que aún no contaban con sesión en periodos 1..3
INSERT INTO public.sesiones_refuerzo (
    anio_lectivo_id,
    periodo_academico_id,
    area_curricular_id,
    docente_usuario_id,
    tema_refuerzo,
    fecha_programada,
    hora_inicio,
    hora_fin,
    codigo_aula
)
SELECT DISTINCT
    c.anio_lectivo_id,
    c.periodo_academico_id,
    c.area_curricular_id,
    c.docente_usuario_id,
    LEFT('Refuerzo: ' || a.nombre_area, 40) AS tema_refuerzo,
    CASE EXTRACT(DOW FROM (p.fecha_fin - 7))
        WHEN 6 THEN (p.fecha_fin - 8)
        WHEN 0 THEN (p.fecha_fin - 6)
        ELSE (p.fecha_fin - 7)
    END AS fecha_programada,
    TIME '17:00' AS hora_inicio,
    TIME '17:45' AS hora_fin,
    au.codigo_aula
FROM public.calificaciones_cneb c
JOIN public.periodos_academicos p ON p.id = c.periodo_academico_id
JOIN public.areas_curriculares a ON a.id = c.area_curricular_id
JOIN public.matriculas m ON m.id = c.matricula_id
JOIN public.secciones s ON s.id = m.seccion_id
JOIN public.aulas au ON au.id = s.aula_id
WHERE c.calificacion_cualitativa = 'C'
  AND NOT EXISTS (
      SELECT 1
      FROM public.sesiones_refuerzo sr
      WHERE sr.anio_lectivo_id = c.anio_lectivo_id
        AND sr.periodo_academico_id = c.periodo_academico_id
        AND sr.area_curricular_id = c.area_curricular_id
        AND sr.docente_usuario_id = c.docente_usuario_id
        AND (sr.codigo_aula IS NULL OR sr.codigo_aula = au.codigo_aula)
  );

INSERT INTO public.inscripciones_refuerzo (
    sesion_refuerzo_id,
    estudiante_id,
    calificacion_origen_id,
    estado_asistencia,
    observaciones
)
SELECT
    sr.id AS sesion_refuerzo_id,
    m.estudiante_id,
    MIN(c.id) AS calificacion_origen_id,
    'PENDIENTE' AS estado_asistencia,
    'Asignación automática obligatoria por logro C' AS observaciones
FROM public.calificaciones_cneb c
JOIN public.matriculas m ON m.id = c.matricula_id
JOIN public.secciones s ON s.id = m.seccion_id
JOIN public.aulas au ON au.id = s.aula_id
JOIN public.sesiones_refuerzo sr
  ON sr.anio_lectivo_id = c.anio_lectivo_id
 AND sr.periodo_academico_id = c.periodo_academico_id
 AND sr.area_curricular_id = c.area_curricular_id
 AND sr.docente_usuario_id = c.docente_usuario_id
 AND (sr.codigo_aula IS NULL OR sr.codigo_aula = au.codigo_aula)
WHERE c.calificacion_cualitativa = 'C'
GROUP BY sr.id, m.estudiante_id
ON CONFLICT (sesion_refuerzo_id, estudiante_id) DO NOTHING;

-- 4. Actualizar estadísticas del optimizador PostgreSQL
ANALYZE public.calificaciones_cneb;
ANALYZE public.asistencias_aula;
ANALYZE public.matriculas;
ANALYZE public.sesiones_refuerzo;
ANALYZE public.inscripciones_refuerzo;
