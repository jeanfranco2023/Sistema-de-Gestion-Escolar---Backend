-- 1. Unicidad de matriculas activas y solicitudes de admision activas
CREATE UNIQUE INDEX IF NOT EXISTS uq_matriculas_estudiante_anio_activa
    ON matriculas (estudiante_id, anio_lectivo_id)
    WHERE estado_matricula IN ('MATRICULADO', 'RESERVADA_TEMPORAL', 'PENDIENTE_ASIGNACION');

CREATE UNIQUE INDEX IF NOT EXISTS uq_solicitud_matricula_dni_activa
    ON solicitudes_matricula_publica (numero_documento_estudiante)
    WHERE estado NOT IN ('MATRICULADA', 'RECHAZADA');

CREATE INDEX IF NOT EXISTS idx_solicitudes_matricula_huella_token
    ON solicitudes_matricula_publica (huella_token);

-- 2. Garantia de estados para borrado logico formal en estudiantes y apoderados
ALTER TABLE estudiantes
    ADD COLUMN IF NOT EXISTS estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_estudiantes_estado'
    ) THEN
        ALTER TABLE estudiantes
            ADD CONSTRAINT chk_estudiantes_estado
            CHECK (estado IN ('ACTIVO', 'SUSPENDIDO', 'RETIRADO', 'EGRESADO', 'INACTIVO'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_estudiantes_estado ON estudiantes(estado);

ALTER TABLE apoderados
    ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_apoderados_estado'
    ) THEN
        ALTER TABLE apoderados
            ADD CONSTRAINT chk_apoderados_estado
            CHECK (estado IN ('ACTIVO', 'SUSPENDIDO', 'INACTIVO'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_apoderados_estado ON apoderados(estado);
CREATE INDEX IF NOT EXISTS idx_apoderados_activo ON apoderados(activo);

-- 3. Indices de soporte para calculos analiticos de rendimiento academico
CREATE INDEX IF NOT EXISTS idx_calificaciones_cneb_matricula_cualitativa
    ON calificaciones_cneb (matricula_id, calificacion_cualitativa);

CREATE INDEX IF NOT EXISTS idx_asistencias_aula_matricula_estado
    ON asistencias_aula (matricula_id, estado);

CREATE INDEX IF NOT EXISTS idx_incidencias_matricula_falta_estado
    ON incidencias_conductuales (matricula_id, tipo_falta, estado);

-- 4. Vista analitica institucional para calculo y ranking de merito escolar CNEB
CREATE OR REPLACE VIEW v_ranking_merito_cneb AS
WITH calif AS (
    SELECT
        matricula_id,
        COUNT(*)::INT AS total_calificaciones,
        ROUND(AVG(CASE calificacion_cualitativa
            WHEN 'AD' THEN 4.0
            WHEN 'A' THEN 3.0
            WHEN 'B' THEN 2.0
            WHEN 'C' THEN 1.0
            ELSE 0.0 END), 2) AS promedio_cneb,
        COUNT(*) FILTER (WHERE calificacion_cualitativa = 'AD')::INT AS cant_ad,
        COUNT(*) FILTER (WHERE calificacion_cualitativa = 'A')::INT AS cant_a,
        COUNT(*) FILTER (WHERE calificacion_cualitativa = 'B')::INT AS cant_b,
        COUNT(*) FILTER (WHERE calificacion_cualitativa = 'C')::INT AS cant_c
    FROM calificaciones_cneb
    GROUP BY matricula_id
),
asist AS (
    SELECT
        matricula_id,
        COUNT(*)::INT AS total_asistencias,
        COUNT(*) FILTER (WHERE estado = 'PRESENTE')::INT AS total_presente,
        COUNT(*) FILTER (WHERE estado = 'TARDANZA')::INT AS total_tardanza,
        COUNT(*) FILTER (WHERE estado = 'FALTA_INJUSTIFICADA')::INT AS total_falta_injustificada,
        COUNT(*) FILTER (WHERE estado = 'FALTA_JUSTIFICADA')::INT AS total_falta_justificada,
        ROUND(
            (COUNT(*) FILTER (WHERE estado IN ('PRESENTE', 'TARDANZA', 'FALTA_JUSTIFICADA'))::NUMERIC
             / NULLIF(COUNT(*), 0)::NUMERIC) * 100.0, 2
        ) AS porcentaje_asistencia
    FROM asistencias_aula
    GROUP BY matricula_id
),
incid AS (
    SELECT
        matricula_id,
        COUNT(*)::INT AS total_incidencias,
        COUNT(*) FILTER (WHERE tipo_falta = 'LEVE')::INT AS incidencias_leves,
        COUNT(*) FILTER (WHERE tipo_falta = 'GRAVE')::INT AS incidencias_graves,
        COUNT(*) FILTER (WHERE tipo_falta = 'MUY_GRAVE')::INT AS incidencias_muy_graves,
        COUNT(*) FILTER (WHERE estado = 'ABIERTA')::INT AS incidencias_abiertas
    FROM incidencias_conductuales
    GROUP BY matricula_id
)
SELECT
    m.id AS matricula_id,
    m.anio_lectivo_id,
    sec.grado_id,
    g.numero_grado,
    niv.codigo_nivel,
    m.seccion_id,
    sec.letra AS letra_seccion,
    m.estudiante_id,
    e.tipo_documento,
    e.numero_documento,
    e.nombres_estudiante,
    e.apellido_paterno_estudiante,
    e.apellido_materno_estudiante,
    m.estado_matricula,
    COALESCE(c.total_calificaciones, 0) AS total_calificaciones,
    COALESCE(c.promedio_cneb, 0.0) AS promedio_cneb,
    COALESCE(c.cant_ad, 0) AS cant_ad,
    COALESCE(c.cant_a, 0) AS cant_a,
    COALESCE(c.cant_b, 0) AS cant_b,
    COALESCE(c.cant_c, 0) AS cant_c,
    COALESCE(a.total_asistencias, 0) AS total_asistencias,
    COALESCE(a.total_presente, 0) AS total_presente,
    COALESCE(a.total_tardanza, 0) AS total_tardanza,
    COALESCE(a.total_falta_injustificada, 0) AS total_falta_injustificada,
    COALESCE(a.total_falta_justificada, 0) AS total_falta_justificada,
    COALESCE(a.porcentaje_asistencia, 100.0) AS porcentaje_asistencia,
    COALESCE(i.total_incidencias, 0) AS total_incidencias,
    COALESCE(i.incidencias_leves, 0) AS incidencias_leves,
    COALESCE(i.incidencias_graves, 0) AS incidencias_graves,
    COALESCE(i.incidencias_muy_graves, 0) AS incidencias_muy_graves,
    COALESCE(i.incidencias_abiertas, 0) AS incidencias_abiertas,
    DENSE_RANK() OVER (
        PARTITION BY m.anio_lectivo_id, sec.grado_id
        ORDER BY COALESCE(c.promedio_cneb, 0.0) DESC, COALESCE(c.cant_ad, 0) DESC, COALESCE(c.cant_a, 0) DESC
    ) AS puesto_grado,
    DENSE_RANK() OVER (
        PARTITION BY m.anio_lectivo_id, m.seccion_id
        ORDER BY COALESCE(c.promedio_cneb, 0.0) DESC, COALESCE(c.cant_ad, 0) DESC, COALESCE(c.cant_a, 0) DESC
    ) AS puesto_seccion
FROM matriculas m
JOIN estudiantes e ON e.id = m.estudiante_id
LEFT JOIN secciones sec ON sec.id = m.seccion_id
LEFT JOIN grados g ON g.id = sec.grado_id
LEFT JOIN niveles niv ON niv.id = g.nivel_id
LEFT JOIN calif c ON c.matricula_id = m.id
LEFT JOIN asist a ON a.matricula_id = m.id
LEFT JOIN incid i ON i.matricula_id = m.id;
