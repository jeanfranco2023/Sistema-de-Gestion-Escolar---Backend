-- ============================================================================
-- V50: Índices estratégicos para optimización de consultas de alta frecuencia
-- ============================================================================

-- 1. Optimización del calendario de asistencia y reporte diario:
-- Acelera el endpoint /api/v1/asistencia/aula/fechas?desde=...&hasta=...
-- y el reporte por fecha eliminando Seq Scans en asistencias_aula.
CREATE INDEX IF NOT EXISTS idx_asistencias_aula_fecha_sesion
    ON public.asistencias_aula USING btree (fecha_sesion);

-- 2. Optimización de filtros de matrícula por año lectivo:
-- Acelera /api/v1/matriculas/anio/{id}/registros y listados masivos.
CREATE INDEX IF NOT EXISTS idx_matriculas_anio_lectivo_id
    ON public.matriculas USING btree (anio_lectivo_id);

-- 3. Optimización de consulta de secciones por año lectivo:
-- Acelera /api/v1/academico/secciones?anioLectivoId=... al seleccionar grado/sección.
CREATE INDEX IF NOT EXISTS idx_secciones_anio_lectivo_id
    ON public.secciones USING btree (anio_lectivo_id);

-- 4. Optimización de búsqueda de estudiantes por año lectivo matriculado:
CREATE INDEX IF NOT EXISTS idx_matriculas_anio_estudiante
    ON public.matriculas USING btree (anio_lectivo_id, estudiante_id);

-- 5. Optimización de vínculos familiares estudiante-apoderado:
CREATE INDEX IF NOT EXISTS idx_estudiante_apoderados_estudiante
    ON public.estudiante_apoderados USING btree (estudiante_id);
