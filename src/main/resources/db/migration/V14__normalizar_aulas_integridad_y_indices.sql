-- Consolidates safe schema cleanup identified during the database audit.
-- Preflight checks abort before removing the duplicated room-code column or
-- enforcing the receipt-series FK if existing data is inconsistent.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM secciones s
        JOIN aulas a ON a.id = s.aula_id
        WHERE s.aula_fisica IS NOT NULL
          AND s.aula_fisica <> a.codigo
    ) THEN
        RAISE EXCEPTION 'V14 cancelada: aula_fisica difiere del código del aula vinculada. No se eliminó ningún dato; revise esas secciones antes de reintentar.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM comprobantes_pago c
        LEFT JOIN series_comprobante s ON s.serie = c.serie
        WHERE s.serie IS NULL
    ) THEN
        RAISE EXCEPTION 'V14 cancelada: existen comprobantes cuya serie no está registrada en series_comprobante. Registre las series antes de reintentar.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM aulas
        WHERE codigo <> btrim(codigo)
    ) THEN
        RAISE EXCEPTION 'V14 cancelada: existen códigos de aula con espacios externos. Corríjalos preservando las referencias antes de reintentar.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM aulas
        GROUP BY upper(codigo)
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'V14 cancelada: hay códigos de aula duplicados al ignorar mayúsculas. Resuelva las duplicidades antes de reintentar.';
    END IF;
END
$$;

-- aulas.codigo is the single source of truth; aula_id is the persisted relation.
ALTER TABLE secciones
    DROP CONSTRAINT IF EXISTS secciones_anio_lectivo_id_aula_fisica_key,
    DROP COLUMN IF EXISTS aula_fisica;

-- Keep financial history tied to a configured series (B001 boletas, E001 recibos).
ALTER TABLE comprobantes_pago
    ADD CONSTRAINT fk_comprobantes_pago_serie
    FOREIGN KEY (serie) REFERENCES series_comprobante(serie)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

-- Preserve curricular and intake-document history instead of cascading deletes.
ALTER TABLE competencias
    DROP CONSTRAINT IF EXISTS competencias_area_id_fkey,
    ADD CONSTRAINT fk_competencias_area_id
    FOREIGN KEY (area_id) REFERENCES areas_curriculares(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE solicitudes_matricula_documentos
    DROP CONSTRAINT IF EXISTS solicitudes_matricula_documentos_solicitud_id_fkey,
    ADD CONSTRAINT fk_solicitudes_documentos_solicitud
    FOREIGN KEY (solicitud_id) REFERENCES solicitudes_matricula_publica(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

-- Remove only exact duplicates or indexes covered by an existing leading key.
DROP INDEX IF EXISTS idx_estudiante_apoderado_est_apod;
DROP INDEX IF EXISTS idx_estudiante_apoderados_apoderado_id;
DROP INDEX IF EXISTS idx_estudiante_apoderados_estudiante_id;
DROP INDEX IF EXISTS idx_obligaciones_pago_matricula_id;
DROP INDEX IF EXISTS idx_comprobantes_pago_id;

-- Support public-enrollment foreign-key joins and parent-key checks as data grows.
CREATE INDEX IF NOT EXISTS idx_solicitudes_matricula_anio
    ON solicitudes_matricula_publica (anio_lectivo_id);
CREATE INDEX IF NOT EXISTS idx_solicitudes_matricula_seccion_anio
    ON solicitudes_matricula_publica (seccion_id, anio_lectivo_id);
CREATE INDEX IF NOT EXISTS idx_solicitudes_matricula_estudiante
    ON solicitudes_matricula_publica (estudiante_id)
    WHERE estudiante_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_solicitudes_matricula_apoderado
    ON solicitudes_matricula_publica (apoderado_id)
    WHERE apoderado_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_solicitudes_matricula_matricula
    ON solicitudes_matricula_publica (matricula_id)
    WHERE matricula_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_aulas_codigo_normalizado
    ON aulas (upper(codigo));

ALTER TABLE aulas
    ADD CONSTRAINT chk_aulas_codigo_sin_espacios_externos
    CHECK (codigo = btrim(codigo));

-- Intake records contain personal data; only the backend database role should access them.
ALTER TABLE solicitudes_matricula_publica ENABLE ROW LEVEL SECURITY;
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
        REVOKE ALL ON TABLE solicitudes_matricula_publica FROM anon;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
        REVOKE ALL ON TABLE solicitudes_matricula_publica FROM authenticated;
    END IF;
END
$$;
