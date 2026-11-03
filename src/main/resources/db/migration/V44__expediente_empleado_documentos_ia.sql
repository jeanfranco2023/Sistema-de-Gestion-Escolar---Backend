-- Creacion de tabla para expedientes de empleados y validacion documental asistida por IA
CREATE TABLE IF NOT EXISTS expedientes_empleados_documentos (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT,
    tipo_documento VARCHAR(30) NOT NULL CHECK (tipo_documento IN ('CV', 'TITULO_PROFESIONAL', 'ANTECEDENTES_POLICIALES', 'ANTECEDENTES_PENALES', 'OTRO')),
    nombre_archivo VARCHAR(200) NOT NULL,
    ruta_objeto VARCHAR(255) NOT NULL,
    tipo_contenido VARCHAR(50) NOT NULL DEFAULT 'application/pdf',
    tamano_bytes BIGINT NOT NULL CHECK (tamano_bytes > 0),
    estado_ia VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado_ia IN ('PENDIENTE', 'VALIDADO_IA', 'OBSERVADO_IA')),
    observaciones_ia TEXT,
    metadatos_ia JSONB,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_validacion_ia TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_expedientes_emp_doc_usuario ON expedientes_empleados_documentos(usuario_id);
CREATE INDEX IF NOT EXISTS idx_expedientes_emp_doc_estado ON expedientes_empleados_documentos(estado_ia);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trg_auditar_expedientes_empleados_documentos'
    ) THEN
        CREATE TRIGGER trg_auditar_expedientes_empleados_documentos
            AFTER INSERT OR UPDATE OR DELETE ON expedientes_empleados_documentos
            FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambios_seguro();
    END IF;
END $$;
