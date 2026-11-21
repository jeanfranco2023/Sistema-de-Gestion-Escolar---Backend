-- Mantiene historial de horarios y libera los bloques solo para registros activos.
ALTER TABLE public.horarios_seccion
    ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT TRUE;

DO $$
DECLARE
    restriccion RECORD;
BEGIN
    FOR restriccion IN
        SELECT c.conname
        FROM pg_constraint c
        WHERE c.conrelid = 'public.horarios_seccion'::regclass
          AND c.contype = 'u'
          AND ARRAY(
              SELECT a.attname::TEXT
              FROM unnest(c.conkey) AS columna(attnum)
              JOIN pg_attribute a
                ON a.attrelid = c.conrelid
               AND a.attnum = columna.attnum
              ORDER BY a.attname::TEXT
          ) IN (
              ARRAY['anio_lectivo_id', 'bloque_horario_id', 'dia_semana', 'seccion_id']::TEXT[],
              ARRAY['anio_lectivo_id', 'bloque_horario_id', 'dia_semana', 'docente_usuario_id']::TEXT[]
          )
    LOOP
        EXECUTE format('ALTER TABLE public.horarios_seccion DROP CONSTRAINT %I', restriccion.conname);
    END LOOP;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS ux_horarios_seccion_activo
    ON public.horarios_seccion (seccion_id, dia_semana, bloque_horario_id, anio_lectivo_id)
    WHERE activo IS TRUE;

CREATE UNIQUE INDEX IF NOT EXISTS ux_horarios_docente_activo
    ON public.horarios_seccion (docente_usuario_id, dia_semana, bloque_horario_id, anio_lectivo_id)
    WHERE activo IS TRUE;
