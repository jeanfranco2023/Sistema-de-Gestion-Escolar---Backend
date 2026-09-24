-- Solicitud anónima con revisión documental, reserva temporal de vacante y pago verificado.
-- Los bytes de los documentos no se guardan en la base; solo se conserva el resultado de revisión.
CREATE TABLE IF NOT EXISTS solicitudes_matricula_publica (
    id UUID PRIMARY KEY,
    token_hash CHAR(64) NOT NULL UNIQUE,
    estado VARCHAR(30) NOT NULL CHECK (estado IN (
        'DOCUMENTOS_PENDIENTES', 'DOCUMENTOS_OBSERVADOS', 'DOCUMENTOS_VALIDADOS',
        'PAGO_PENDIENTE', 'MATRICULADA', 'RECHAZADA'
    )),
    anio_lectivo_id SMALLINT NOT NULL REFERENCES anios_lectivos(id) ON DELETE RESTRICT,
    seccion_id INT NOT NULL,
    numero_documento_estudiante VARCHAR(8) NOT NULL CHECK (numero_documento_estudiante ~ '^[0-9]{8}$'),
    nombres_estudiante VARCHAR(100) NOT NULL,
    apellido_paterno_estudiante VARCHAR(80) NOT NULL,
    apellido_materno_estudiante VARCHAR(80) NOT NULL,
    fecha_nacimiento_estudiante DATE NOT NULL,
    genero_estudiante CHAR(1) NOT NULL CHECK (genero_estudiante IN ('M', 'F')),
    numero_documento_apoderado VARCHAR(8) NOT NULL CHECK (numero_documento_apoderado ~ '^[0-9]{8}$'),
    nombres_apoderado VARCHAR(100) NOT NULL,
    apellido_paterno_apoderado VARCHAR(80) NOT NULL,
    apellido_materno_apoderado VARCHAR(80) NOT NULL,
    celular_apoderado VARCHAR(9) NOT NULL CHECK (celular_apoderado ~ '^9[0-9]{8}$'),
    email_apoderado VARCHAR(100),
    direccion_apoderado VARCHAR(200) NOT NULL,
    ubigeo_apoderado VARCHAR(6) NOT NULL CHECK (ubigeo_apoderado ~ '^[0-9]{6}$'),
    parentesco VARCHAR(30) NOT NULL CHECK (parentesco IN ('PADRE', 'MADRE', 'TUTOR_LEGAL', 'ABUELO_A', 'OTRO')),
    consentimiento_gemini BOOLEAN NOT NULL CHECK (consentimiento_gemini IS TRUE),
    estado_partida VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado_partida IN ('PENDIENTE', 'VALIDADO', 'OBSERVADO')),
    observacion_partida VARCHAR(500),
    estado_dni_c4 VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado_dni_c4 IN ('PENDIENTE', 'VALIDADO', 'OBSERVADO')),
    observacion_dni_c4 VARCHAR(500),
    estado_recibo VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado_recibo IN ('PENDIENTE', 'VALIDADO', 'OBSERVADO')),
    observacion_recibo VARCHAR(500),
    pago_preferencia_id VARCHAR(100),
    pago_enlace VARCHAR(1000),
    pago_id VARCHAR(100),
    pago_monto NUMERIC(10,2) NOT NULL DEFAULT 1.00 CHECK (pago_monto = 1.00),
    pago_expira_at TIMESTAMPTZ,
    vacante_reservada BOOLEAN NOT NULL DEFAULT FALSE,
    estudiante_id BIGINT REFERENCES estudiantes(id) ON DELETE RESTRICT,
    apoderado_id BIGINT REFERENCES apoderados(id) ON DELETE RESTRICT,
    matricula_id BIGINT REFERENCES matriculas(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (seccion_id, anio_lectivo_id) REFERENCES secciones(id, anio_lectivo_id) ON DELETE RESTRICT,
    CHECK (NOT vacante_reservada OR pago_expira_at IS NOT NULL),
    CHECK (estado != 'MATRICULADA' OR (estudiante_id IS NOT NULL AND apoderado_id IS NOT NULL AND matricula_id IS NOT NULL AND pago_id IS NOT NULL))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_solicitud_matricula_dni_activa
    ON solicitudes_matricula_publica (numero_documento_estudiante)
    WHERE estado NOT IN ('MATRICULADA', 'RECHAZADA');
CREATE INDEX IF NOT EXISTS idx_solicitudes_matricula_estado_fecha
    ON solicitudes_matricula_publica (estado, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_solicitudes_matricula_reservas
    ON solicitudes_matricula_publica (pago_expira_at)
    WHERE vacante_reservada IS TRUE;
