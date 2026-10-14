-- Validaciones automáticas: Gemini, API de DNI y firma criptográfica RENIEC.
ALTER TABLE solicitudes_matricula_publica DROP CONSTRAINT solicitudes_matricula_publica_estado_check;
ALTER TABLE solicitudes_matricula_publica ADD CONSTRAINT solicitudes_matricula_publica_estado_check
  CHECK (estado IN ('DOCUMENTOS_PENDIENTES','DOCUMENTOS_EN_VALIDACION','DOCUMENTOS_OBSERVADOS',
                   'DOCUMENTOS_VALIDADOS','PAGO_PENDIENTE','MATRICULADA','RECHAZADA'));
ALTER TABLE solicitudes_matricula_publica DROP CONSTRAINT solicitudes_matricula_publica_estado_partida_check;
ALTER TABLE solicitudes_matricula_publica DROP CONSTRAINT solicitudes_matricula_publica_estado_dni_c4_check;
ALTER TABLE solicitudes_matricula_publica DROP CONSTRAINT solicitudes_matricula_publica_estado_recibo_check;
ALTER TABLE solicitudes_matricula_publica ADD CONSTRAINT solicitudes_matricula_publica_estado_partida_check
  CHECK (estado_partida IN ('PENDIENTE','EN_VALIDACION','VALIDADO','OBSERVADO'));
ALTER TABLE solicitudes_matricula_publica ADD CONSTRAINT solicitudes_matricula_publica_estado_dni_c4_check
  CHECK (estado_dni_c4 IN ('PENDIENTE','EN_VALIDACION','VALIDADO','OBSERVADO'));
ALTER TABLE solicitudes_matricula_publica ADD CONSTRAINT solicitudes_matricula_publica_estado_recibo_check
  CHECK (estado_recibo IN ('PENDIENTE','EN_VALIDACION','VALIDADO','OBSERVADO'));

CREATE TABLE versiones_documentos_matricula (
  id UUID PRIMARY KEY,
  solicitud_id UUID NOT NULL REFERENCES solicitudes_matricula_publica(id) ON DELETE RESTRICT,
  tipo VARCHAR(30) NOT NULL CHECK (tipo IN ('PARTIDA_NACIMIENTO','DNI_C4','RECIBO_SERVICIO')),
  contenedor VARCHAR(100) NOT NULL,
  ruta_objeto VARCHAR(500) NOT NULL,
  tipo_contenido VARCHAR(30) NOT NULL CHECK (tipo_contenido IN ('application/pdf','image/jpeg','image/png','image/webp')),
  tamano_bytes BIGINT NOT NULL CHECK (tamano_bytes > 0 AND tamano_bytes <= 5242880),
  huella_sha256 CHAR(64) NOT NULL CHECK (huella_sha256 ~ '^[0-9a-f]{64}$'),
  ia_valida BOOLEAN,
  dni_api_coincide BOOLEAN,
  observacion_ia VARCHAR(500),
  estado_firma VARCHAR(20) NOT NULL CHECK (estado_firma IN ('VALIDA','INVALIDA','NO_APLICA','NO_VERIFICABLE','ERROR_SERVICIO')),
  detalle_firma VARCHAR(500) NOT NULL,
  certificado_sha256 CHAR(64) CHECK (certificado_sha256 IS NULL OR certificado_sha256 ~ '^[0-9a-f]{64}$'),
  fecha_carga TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (id, solicitud_id, tipo)
);
CREATE INDEX idx_versiones_documentos_solicitud_tipo ON versiones_documentos_matricula (solicitud_id, tipo, fecha_carga DESC);
ALTER TABLE solicitudes_matricula_documentos ADD COLUMN version_id UUID;
UPDATE solicitudes_matricula_documentos SET version_id = gen_random_uuid();
INSERT INTO versiones_documentos_matricula (
  id, solicitud_id, tipo, contenedor, ruta_objeto, tipo_contenido, tamano_bytes,
  huella_sha256, estado_firma, detalle_firma, fecha_carga)
SELECT version_id, solicitud_id, tipo, contenedor, ruta_objeto, tipo_contenido, tamano_bytes,
  huella_sha256, 'NO_VERIFICABLE', 'Archivo histórico: firma y consulta de DNI no verificadas.', fecha_actualizacion
FROM solicitudes_matricula_documentos;
ALTER TABLE solicitudes_matricula_documentos ALTER COLUMN version_id SET NOT NULL;
ALTER TABLE solicitudes_matricula_documentos ADD CONSTRAINT fk_documento_version_actual
  FOREIGN KEY (version_id, solicitud_id, tipo) REFERENCES versiones_documentos_matricula (id, solicitud_id, tipo) ON DELETE RESTRICT;

-- Preserva expedientes finalizados; no presenta archivos históricos como verificados.
UPDATE solicitudes_matricula_publica
SET estado_partida = CASE WHEN estado_partida = 'VALIDADO' THEN 'EN_VALIDACION' ELSE estado_partida END,
    estado_dni_c4 = CASE WHEN estado_dni_c4 = 'VALIDADO' THEN 'EN_VALIDACION' ELSE estado_dni_c4 END,
    estado_recibo = CASE WHEN estado_recibo = 'VALIDADO' THEN 'EN_VALIDACION' ELSE estado_recibo END,
    estado = CASE WHEN estado = 'DOCUMENTOS_VALIDADOS' THEN 'DOCUMENTOS_EN_VALIDACION' ELSE estado END
WHERE estado NOT IN ('MATRICULADA','RECHAZADA');

CREATE FUNCTION fn_proteger_version_documental() RETURNS TRIGGER
LANGUAGE plpgsql AS $$
BEGIN
  RAISE EXCEPTION 'La evidencia documental es inmutable; registre un nuevo intento de verificación.';
END;
$$;
CREATE TRIGGER trg_version_documental_inmutable BEFORE UPDATE OR DELETE ON versiones_documentos_matricula
FOR EACH ROW EXECUTE FUNCTION fn_proteger_version_documental();
ALTER TABLE versiones_documentos_matricula ENABLE ROW LEVEL SECURITY;
DO $$
DECLARE rol TEXT;
BEGIN
  FOREACH rol IN ARRAY ARRAY['anon','authenticated'] LOOP
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = rol) THEN
      EXECUTE format('REVOKE ALL ON versiones_documentos_matricula FROM %I', rol);
    END IF;
  END LOOP;
END;
$$;
