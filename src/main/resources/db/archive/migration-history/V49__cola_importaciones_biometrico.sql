CREATE TABLE IF NOT EXISTS importaciones_biometrico (
    id UUID PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT,
    huella_contenido VARCHAR(64) NOT NULL UNIQUE,
    nombre_archivo VARCHAR(150) NOT NULL,
    payload BYTEA,
    estado VARCHAR(20) NOT NULL CHECK (estado IN ('EN_COLA', 'PROCESANDO', 'COMPLETADO', 'ERROR')),
    total_filas INTEGER NOT NULL CHECK (total_filas BETWEEN 1 AND 10000),
    marcas_validas INTEGER NOT NULL DEFAULT 0 CHECK (marcas_validas >= 0),
    marcas_erroneas INTEGER NOT NULL DEFAULT 0 CHECK (marcas_erroneas >= 0),
    lote_id BIGINT REFERENCES lotes_biometrico(id) ON DELETE SET NULL,
    mensaje_error VARCHAR(500),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    iniciado_en TIMESTAMPTZ,
    finalizado_en TIMESTAMPTZ,
    CONSTRAINT chk_importacion_biometrica_payload_pendiente
        CHECK (estado IN ('EN_COLA', 'PROCESANDO') OR payload IS NULL)
);

CREATE INDEX IF NOT EXISTS idx_importaciones_biometrico_estado_creado
    ON importaciones_biometrico (estado, creado_en);
CREATE INDEX IF NOT EXISTS idx_importaciones_biometrico_usuario_creado
    ON importaciones_biometrico (usuario_id, creado_en DESC);

ALTER TABLE importaciones_biometrico ENABLE ROW LEVEL SECURITY;
