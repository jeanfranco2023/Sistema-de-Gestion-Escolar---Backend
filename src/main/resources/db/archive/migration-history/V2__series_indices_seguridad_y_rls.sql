-- ==============================================================================
-- MIGRACIÓN FLYWAY V2: SERIES ATÓMICAS, ÍNDICES DE CONCURRENCIA, SEGURIDAD Y RLS
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. TABLA PARA CONTADORES SECUENCIALES ATÓMICOS DE COMPROBANTES (SUNAT)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS series_comprobante (
    serie VARCHAR(4) PRIMARY KEY CHECK (serie ~ '^[BE][0-9]{3}$'),
    ultimo_correlativo INT NOT NULL DEFAULT 0 CHECK (ultimo_correlativo >= 0),
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);

INSERT INTO series_comprobante (serie, ultimo_correlativo) 
VALUES ('B001', 0), ('E001', 0) 
ON CONFLICT (serie) DO NOTHING;

-- ------------------------------------------------------------------------------
-- 2. ÍNDICES DE RENDIMIENTO PARA ALTA CONCURRENCIA Y PREVENCIÓN DE N+1
-- ------------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_estudiante_apoderado_est_apod ON estudiante_apoderados (estudiante_id, apoderado_id);
CREATE INDEX IF NOT EXISTS idx_marcas_porteria_fecha_hora ON marcas_biometrico_porteria (fecha_hora);
CREATE INDEX IF NOT EXISTS idx_marcas_porteria_dni_leido ON marcas_biometrico_porteria (dni_leido);
CREATE INDEX IF NOT EXISTS idx_obligaciones_pago_matricula_id ON obligaciones_pago (matricula_id);
CREATE INDEX IF NOT EXISTS idx_matriculas_estudiante_id ON matriculas (estudiante_id);
CREATE INDEX IF NOT EXISTS idx_estudiante_apoderados_apoderado_id ON estudiante_apoderados (apoderado_id);
CREATE INDEX IF NOT EXISTS idx_estudiante_apoderados_estudiante_id ON estudiante_apoderados (estudiante_id);

-- ------------------------------------------------------------------------------
-- 3. PROTECCIÓN CONTRA ACCESO DIRECTO VÍA SUPABASE DATA API (POSTGREST) Y RLS
-- ------------------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
        REVOKE ALL ON ALL TABLES IN SCHEMA public FROM anon;
        REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM anon;
        REVOKE ALL ON ALL ROUTINES IN SCHEMA public FROM anon;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
        REVOKE ALL ON ALL TABLES IN SCHEMA public FROM authenticated;
        REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM authenticated;
        REVOKE ALL ON ALL ROUTINES IN SCHEMA public FROM authenticated;
    END IF;
END
$$;

-- Habilitar Row Level Security (RLS) en todas las tablas del esquema público
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN (
        SELECT tablename 
        FROM pg_tables 
        WHERE schemaname = 'public' 
          AND tablename NOT IN ('flyway_schema_history')
    ) LOOP
        EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY;', r.tablename);
    END LOOP;
END
$$;
