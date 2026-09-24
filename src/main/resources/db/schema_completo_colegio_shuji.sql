-- ==============================================================================
-- COLEGIO SHUJI KITAMURA - ESQUEMA COMPLETO POSTGRESQL / SUPABASE
-- Generado: 2026-09-20
-- Consolidación ordenada de migraciones Flyway V1 a V5.
-- Uso: instalación desde cero sobre una base vacía.
-- La aplicación debe seguir usando Flyway en entornos existentes.
-- ==============================================================================


-- >>> INICIO V1__schema_colegio_shuji.sql

-- ==============================================================================
-- Proyecto Integrador II: Plataforma Web I.E.P. Shuji Kitamura
-- Script de Migración V1 (Versión Definitiva con Integridad Relacional Máxima)
-- Contexto: Perú (RENIEC, CNEB - MINEDU, SUNAT, MTC, SIAGIE)
-- Total: Exactamente 33 Tablas de Dominio + Triggers de Vacantes, Finanzas y Auditoría
-- ==============================================================================

-- 1. Extensiones Oficiales de PostgreSQL
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "unaccent";

-- ------------------------------------------------------------------------------
-- 2. MÓDULO DE SEGURIDAD, USUARIOS Y RBAC (M1)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS roles (
    id SMALLSERIAL PRIMARY KEY,
    codigo VARCHAR(30) UNIQUE NOT NULL,
    nombre VARCHAR(50) NOT NULL,
    descripcion VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS usuarios (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(60) NOT NULL,
    activo BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS usuario_roles (
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    rol_id SMALLINT NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    PRIMARY KEY (usuario_id, rol_id)
);

CREATE TABLE IF NOT EXISTS sesiones (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    ip_address INET,
    user_agent TEXT,
    expira_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS auditoria_cambios (
    id BIGSERIAL PRIMARY KEY,
    tabla_afectada VARCHAR(50) NOT NULL,
    registro_id BIGINT NOT NULL,
    accion VARCHAR(10) NOT NULL CHECK (accion IN ('INSERT', 'UPDATE', 'DELETE')),
    datos_anteriores JSONB,
    datos_nuevos JSONB,
    usuario_id BIGINT REFERENCES usuarios(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- ------------------------------------------------------------------------------
-- 3. MÓDULO DE PARAMETRIZACIÓN ESCOLAR Y ESTRUCTURA ACADÉMICA (M2)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS anios_lectivos (
    id SMALLSERIAL PRIMARY KEY,
    anio SMALLINT UNIQUE NOT NULL CHECK (anio >= 2024),
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    abierto BOOLEAN DEFAULT TRUE NOT NULL,
    CHECK (fecha_fin > fecha_inicio)
);

CREATE TABLE IF NOT EXISTS periodos_academicos (
    id SMALLSERIAL PRIMARY KEY,
    anio_lectivo_id SMALLINT NOT NULL REFERENCES anios_lectivos(id) ON DELETE RESTRICT,
    numero_periodo SMALLINT NOT NULL CHECK (numero_periodo BETWEEN 1 AND 4),
    nombre VARCHAR(30) NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    cerrado BOOLEAN DEFAULT FALSE NOT NULL,
    CHECK (fecha_fin > fecha_inicio),
    UNIQUE (anio_lectivo_id, numero_periodo),
    UNIQUE (id, anio_lectivo_id) -- Llave compuesta para coherencia temporal de notas
);

CREATE TABLE IF NOT EXISTS niveles (
    id SMALLSERIAL PRIMARY KEY,
    codigo VARCHAR(20) UNIQUE NOT NULL CHECK (codigo IN ('INICIAL', 'PRIMARIA', 'SECUNDARIA')),
    nombre VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS grados (
    id SMALLSERIAL PRIMARY KEY,
    nivel_id SMALLINT NOT NULL REFERENCES niveles(id) ON DELETE RESTRICT,
    numero_grado SMALLINT NOT NULL CHECK (numero_grado BETWEEN 1 AND 6),
    nombre VARCHAR(50) NOT NULL,
    UNIQUE (nivel_id, numero_grado),
    UNIQUE (id, nivel_id) -- Llave compuesta para coherencia de nivel
);

CREATE TABLE IF NOT EXISTS secciones (
    id SERIAL PRIMARY KEY,
    anio_lectivo_id SMALLINT NOT NULL REFERENCES anios_lectivos(id) ON DELETE RESTRICT,
    grado_id SMALLINT NOT NULL,
    nivel_id SMALLINT NOT NULL,
    letra CHAR(1) NOT NULL CHECK (letra ~ '^[A-Z]$'),
    cupo_maximo SMALLINT NOT NULL CHECK (cupo_maximo > 0),
    vacantes_ocupadas SMALLINT DEFAULT 0 NOT NULL CHECK (vacantes_ocupadas >= 0 AND vacantes_ocupadas <= cupo_maximo),
    aula_fisica VARCHAR(30),
    FOREIGN KEY (grado_id, nivel_id) REFERENCES grados(id, nivel_id) ON DELETE RESTRICT,
    UNIQUE (anio_lectivo_id, grado_id, letra),
    UNIQUE (anio_lectivo_id, aula_fisica), -- Impide que dos secciones ocupen la misma aula física en el mismo año
    UNIQUE (id, anio_lectivo_id), -- Coherencia año-sección para matrícula
    UNIQUE (id, anio_lectivo_id, nivel_id) -- Coherencia año-sección-nivel para asignaciones
);

-- ------------------------------------------------------------------------------
-- 4. MÓDULO DE COMUNIDAD ESCOLAR, FAMILIAS Y MATRÍCULA (M2 / M3)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS apoderados (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    usuario_id BIGINT UNIQUE REFERENCES usuarios(id) ON DELETE SET NULL,
    tipo_documento VARCHAR(10) DEFAULT 'DNI' NOT NULL CHECK (tipo_documento IN ('DNI', 'CE', 'PASAPORTE')),
    numero_documento VARCHAR(15) NOT NULL,
    nombres VARCHAR(100) NOT NULL,
    apellido_paterno VARCHAR(80) NOT NULL,
    apellido_materno VARCHAR(80) NOT NULL,
    celular VARCHAR(9) NOT NULL CHECK (celular ~ '^9[0-9]{8}$'),
    email VARCHAR(100),
    direccion VARCHAR(200) NOT NULL,
    ubigeo_inei VARCHAR(6) NOT NULL CHECK (ubigeo_inei ~ '^[0-9]{6}$'),
    validado_reniec BOOLEAN DEFAULT FALSE NOT NULL,
    origen_registro VARCHAR(20) DEFAULT 'RENIEC_API' NOT NULL CHECK (origen_registro IN ('RENIEC_API', 'MANUAL_CONTINGENCIA')),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE (tipo_documento, numero_documento)
);

CREATE TABLE IF NOT EXISTS estudiantes (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    tipo_documento VARCHAR(10) DEFAULT 'DNI' NOT NULL CHECK (tipo_documento IN ('DNI', 'CE', 'PASAPORTE')),
    numero_documento VARCHAR(15) NOT NULL,
    nombres VARCHAR(100) NOT NULL,
    apellido_paterno VARCHAR(80) NOT NULL,
    apellido_materno VARCHAR(80) NOT NULL,
    fecha_nacimiento DATE NOT NULL,
    genero CHAR(1) NOT NULL CHECK (genero IN ('M', 'F')),
    codigo_estudiante_siagie VARCHAR(14) UNIQUE,
    validado_reniec BOOLEAN DEFAULT FALSE NOT NULL,
    origen_registro VARCHAR(20) DEFAULT 'RENIEC_API' NOT NULL CHECK (origen_registro IN ('RENIEC_API', 'MANUAL_CONTINGENCIA')),
    grupo_sanguineo VARCHAR(5),
    alergias_condiciones TEXT,
    activo BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE (tipo_documento, numero_documento)
);

CREATE TABLE IF NOT EXISTS estudiante_apoderados (
    id BIGSERIAL PRIMARY KEY,
    estudiante_id BIGINT NOT NULL REFERENCES estudiantes(id) ON DELETE RESTRICT,
    apoderado_id BIGINT NOT NULL REFERENCES apoderados(id) ON DELETE RESTRICT,
    parentesco VARCHAR(30) NOT NULL CHECK (parentesco IN ('PADRE', 'MADRE', 'TUTOR_LEGAL', 'ABUELO_A', 'OTRO')),
    es_responsable_economico BOOLEAN DEFAULT FALSE NOT NULL,
    tiene_custodia BOOLEAN DEFAULT TRUE NOT NULL,
    permite_recojo BOOLEAN DEFAULT TRUE NOT NULL,
    UNIQUE (estudiante_id, apoderado_id)
);

CREATE TABLE IF NOT EXISTS matriculas (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    anio_lectivo_id SMALLINT NOT NULL,
    estudiante_id BIGINT NOT NULL REFERENCES estudiantes(id) ON DELETE RESTRICT,
    seccion_id INT NOT NULL,
    fecha_matricula TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    estado_matricula VARCHAR(25) DEFAULT 'SOLICITADA' NOT NULL CHECK (estado_matricula IN ('SOLICITADA', 'RESERVADA_TEMPORAL', 'MATRICULADO', 'TRASLADADO', 'RETIRADO', 'CANCELADA')),
    reserva_expira_at TIMESTAMPTZ,
    observaciones TEXT,
    CHECK (estado_matricula != 'RESERVADA_TEMPORAL' OR reserva_expira_at IS NOT NULL),
    UNIQUE (anio_lectivo_id, estudiante_id),
    UNIQUE (id, anio_lectivo_id), -- Para relacionar con periodos de notas
    UNIQUE (id, anio_lectivo_id, seccion_id), -- Llave compuesta para coherencia sección-alumno-nota
    FOREIGN KEY (seccion_id, anio_lectivo_id) REFERENCES secciones(id, anio_lectivo_id) ON DELETE RESTRICT
);

-- ------------------------------------------------------------------------------
-- 5. MÓDULO DE OFERTA CURRICULAR, ASIGNACIÓN DOCENTE Y HORARIOS (M4 / M6)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS areas_curriculares (
    id SMALLSERIAL PRIMARY KEY,
    nivel_id SMALLINT NOT NULL REFERENCES niveles(id) ON DELETE RESTRICT,
    codigo VARCHAR(20) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    UNIQUE (nivel_id, codigo),
    UNIQUE (id, nivel_id) -- Llave compuesta para garantizar nivel consistente
);

CREATE TABLE IF NOT EXISTS competencias (
    id SMALLSERIAL PRIMARY KEY,
    area_id SMALLINT NOT NULL REFERENCES areas_curriculares(id) ON DELETE CASCADE,
    numero_orden SMALLINT NOT NULL CHECK (numero_orden > 0),
    nombre VARCHAR(150) NOT NULL,
    descripcion TEXT,
    UNIQUE (id, area_id) -- Llave compuesta para coherencia área-competencia en notas
);

CREATE TABLE IF NOT EXISTS asignaciones_docentes (
    id BIGSERIAL PRIMARY KEY,
    docente_usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT,
    seccion_id INT NOT NULL,
    anio_lectivo_id SMALLINT NOT NULL,
    nivel_id SMALLINT NOT NULL,
    area_curricular_id SMALLINT NOT NULL,
    FOREIGN KEY (seccion_id, anio_lectivo_id, nivel_id) REFERENCES secciones(id, anio_lectivo_id, nivel_id) ON DELETE RESTRICT,
    FOREIGN KEY (area_curricular_id, nivel_id) REFERENCES areas_curriculares(id, nivel_id) ON DELETE RESTRICT,
    UNIQUE (seccion_id, area_curricular_id, anio_lectivo_id),
    UNIQUE (id, anio_lectivo_id, seccion_id, docente_usuario_id), -- Valida sección de horario estrictamente
    UNIQUE (id, seccion_id, area_curricular_id, anio_lectivo_id, docente_usuario_id) -- Valida notas estrictamente
);

CREATE TABLE IF NOT EXISTS bloques_horarios (
    id SMALLSERIAL PRIMARY KEY,
    numero_bloque SMALLINT UNIQUE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    es_recreo BOOLEAN DEFAULT FALSE NOT NULL,
    CHECK (hora_fin > hora_inicio)
);

CREATE TABLE IF NOT EXISTS horarios_seccion (
    id BIGSERIAL PRIMARY KEY,
    anio_lectivo_id SMALLINT NOT NULL REFERENCES anios_lectivos(id) ON DELETE RESTRICT,
    seccion_id INT NOT NULL,
    docente_usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT,
    asignacion_docente_id BIGINT NOT NULL,
    dia_semana SMALLINT NOT NULL CHECK (dia_semana BETWEEN 1 AND 5), -- 1: Lunes a 5: Viernes
    bloque_horario_id SMALLINT NOT NULL REFERENCES bloques_horarios(id) ON DELETE RESTRICT,
    -- Valida que la asignación docente pertenezca a la misma sección, año y docente exactos
    FOREIGN KEY (asignacion_docente_id, anio_lectivo_id, seccion_id, docente_usuario_id) 
        REFERENCES asignaciones_docentes(id, anio_lectivo_id, seccion_id, docente_usuario_id) ON DELETE RESTRICT,
    UNIQUE (seccion_id, dia_semana, bloque_horario_id, anio_lectivo_id), -- Previene colisión de aulas
    UNIQUE (docente_usuario_id, dia_semana, bloque_horario_id, anio_lectivo_id) -- Previene colisión de docentes
);

-- ------------------------------------------------------------------------------
-- 6. MÓDULO DE TESORERÍA, OBLIGACIONES Y PASARELA DE PAGOS (M3)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS conceptos_cobro (
    id SMALLSERIAL PRIMARY KEY,
    codigo VARCHAR(30) UNIQUE NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    tipo_concepto VARCHAR(20) NOT NULL CHECK (tipo_concepto IN ('MATRICULA', 'PENSION', 'CERTIFICADO', 'OTRO')),
    monto_sugerido NUMERIC(10, 2) NOT NULL CHECK (monto_sugerido >= 0.00),
    UNIQUE (id, tipo_concepto)
);

CREATE TABLE IF NOT EXISTS obligaciones_pago (
    id BIGSERIAL PRIMARY KEY,
    matricula_id BIGINT NOT NULL REFERENCES matriculas(id) ON DELETE RESTRICT,
    concepto_id SMALLINT NOT NULL,
    tipo_concepto VARCHAR(20) NOT NULL,
    numero_cuota SMALLINT NOT NULL CHECK (numero_cuota BETWEEN 0 AND 10), -- 0: Matrícula, 1 a 10: Pensiones
    descripcion VARCHAR(150) NOT NULL,
    fecha_vencimiento DATE NOT NULL,
    monto_base NUMERIC(10, 2) NOT NULL CHECK (monto_base >= 0.00),
    monto_mora NUMERIC(10, 2) DEFAULT 0.00 NOT NULL CHECK (monto_mora >= 0.00),
    monto_descuento NUMERIC(10, 2) DEFAULT 0.00 NOT NULL CHECK (monto_descuento >= 0.00),
    total_pagado NUMERIC(10, 2) DEFAULT 0.00 NOT NULL CHECK (total_pagado >= 0.00),
    saldo_pendiente NUMERIC(10, 2) GENERATED ALWAYS AS (monto_base + monto_mora - monto_descuento - total_pagado) STORED,
    estado VARCHAR(25) DEFAULT 'PENDIENTE' NOT NULL CHECK (estado IN ('PENDIENTE', 'PAGADO_TOTAL', 'PAGADO_PARCIAL', 'VENCIDO', 'CANCELADO_POR_TRASLADO')),
    CHECK (total_pagado <= (monto_base + monto_mora - monto_descuento)),
    CHECK (
        (tipo_concepto = 'MATRICULA' AND numero_cuota = 0) OR
        (tipo_concepto = 'PENSION' AND numero_cuota BETWEEN 1 AND 10) OR
        (tipo_concepto NOT IN ('MATRICULA', 'PENSION'))
    ),
    FOREIGN KEY (concepto_id, tipo_concepto) REFERENCES conceptos_cobro(id, tipo_concepto) ON DELETE RESTRICT,
    UNIQUE (matricula_id, concepto_id, numero_cuota)
);

CREATE TABLE IF NOT EXISTS pagos_transacciones (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    obligacion_pago_id BIGINT NOT NULL REFERENCES obligaciones_pago(id) ON DELETE RESTRICT,
    pasarela_proveedor VARCHAR(30) NOT NULL CHECK (pasarela_proveedor IN ('CULQI', 'NIUBIZ', 'MERCADO_PAGO', 'CAJA_EFECTIVO', 'TRANSFERENCIA')),
    pasarela_transaccion_id VARCHAR(100) UNIQUE NOT NULL, -- Idempotencia estricta
    metodo_pago VARCHAR(30) NOT NULL CHECK (metodo_pago IN ('TARJETA_CREDITO', 'TARJETA_DEBITO', 'YAPE', 'PLIN', 'TRANSFERENCIA', 'EFECTIVO')),
    monto_pagado NUMERIC(10, 2) NOT NULL CHECK (monto_pagado > 0.00),
    fecha_pago TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    estado_pago VARCHAR(20) DEFAULT 'APROBADO' NOT NULL CHECK (estado_pago IN ('APROBADO', 'RECHAZADO', 'REVERTIDO')),
    payload_webhook JSONB
);

CREATE TABLE IF NOT EXISTS comprobantes_pago (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    pago_transaccion_id BIGINT UNIQUE NOT NULL REFERENCES pagos_transacciones(id) ON DELETE RESTRICT,
    tipo_comprobante VARCHAR(20) DEFAULT 'RECIBO_INTERNO' NOT NULL CHECK (tipo_comprobante IN ('BOLETA', 'RECIBO_INTERNO')),
    serie VARCHAR(4) NOT NULL CHECK (serie ~ '^[BE][0-9]{3}$'),
    correlativo INT NOT NULL CHECK (correlativo > 0),
    fecha_emision TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    monto_total NUMERIC(10, 2) NOT NULL CHECK (monto_total > 0.00),
    estado_comprobante VARCHAR(20) DEFAULT 'EMITIDO' NOT NULL CHECK (estado_comprobante IN ('EMITIDO', 'ANULADO')),
    fecha_anulacion TIMESTAMPTZ,
    motivo_anulacion VARCHAR(255),
    url_pdf_comprobante VARCHAR(255),
    CHECK (
        (estado_comprobante = 'EMITIDO' AND fecha_anulacion IS NULL AND motivo_anulacion IS NULL)
        OR
        (estado_comprobante = 'ANULADO' AND fecha_anulacion IS NOT NULL AND motivo_anulacion IS NOT NULL)
    ),
    UNIQUE (serie, correlativo)
);

-- ------------------------------------------------------------------------------
-- 7. MÓDULO DE ASISTENCIA, BIOMETRÍA Y CONCILIACIÓN DE AULA (M5)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS lotes_biometrico (
    id BIGSERIAL PRIMARY KEY,
    nombre_archivo VARCHAR(150) NOT NULL,
    total_filas INT NOT NULL CHECK (total_filas >= 0),
    marcas_validas INT NOT NULL CHECK (marcas_validas >= 0),
    marcas_erroneas INT NOT NULL CHECK (marcas_erroneas >= 0),
    importado_por_usuario_id BIGINT REFERENCES usuarios(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS marcas_biometrico_porteria (
    id BIGSERIAL PRIMARY KEY,
    lote_id BIGINT REFERENCES lotes_biometrico(id) ON DELETE SET NULL,
    dni_leido VARCHAR(15) NOT NULL,
    fecha_hora TIMESTAMPTZ NOT NULL,
    dispositivo_codigo VARCHAR(30) DEFAULT 'PORTERIA_01',
    estudiante_id BIGINT REFERENCES estudiantes(id) ON DELETE SET NULL,
    estado_procesamiento VARCHAR(25) DEFAULT 'PENDIENTE' NOT NULL CHECK (estado_procesamiento IN ('PENDIENTE', 'CONCILIADO', 'DNI_NO_IDENTIFICADO', 'DUPLICADO')),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS asistencias_aula (
    id BIGSERIAL PRIMARY KEY,
    matricula_id BIGINT NOT NULL REFERENCES matriculas(id) ON DELETE RESTRICT,
    fecha_sesion DATE NOT NULL,
    hora_registro TIME NOT NULL,
    estado VARCHAR(25) NOT NULL CHECK (estado IN ('PRESENTE', 'TARDANZA', 'FALTA_INJUSTIFICADA', 'FALTA_JUSTIFICADA')),
    auxiliar_usuario_id BIGINT REFERENCES usuarios(id) ON DELETE RESTRICT,
    justificada BOOLEAN DEFAULT FALSE NOT NULL,
    motivo_justificacion TEXT,
    documento_sustento_url VARCHAR(255),
    UNIQUE (matricula_id, fecha_sesion)
);

CREATE TABLE IF NOT EXISTS conciliaciones_asistencia (
    id BIGSERIAL PRIMARY KEY,
    fecha DATE NOT NULL,
    estudiante_id BIGINT NOT NULL REFERENCES estudiantes(id) ON DELETE RESTRICT,
    marco_porteria BOOLEAN NOT NULL,
    presente_aula BOOLEAN NOT NULL,
    tipo_discrepancia VARCHAR(35) NOT NULL CHECK (tipo_discrepancia IN ('ASISTENCIA_CONCILIADA', 'DISCREPANCIA_FUGA', 'DISCREPANCIA_OMISION_PORTERIA', 'PENDIENTE_CIERRE_TURNO')),
    alerta_notificada BOOLEAN DEFAULT FALSE NOT NULL,
    UNIQUE (fecha, estudiante_id)
);

CREATE TABLE IF NOT EXISTS incidencias_conductuales (
    id BIGSERIAL PRIMARY KEY,
    matricula_id BIGINT NOT NULL REFERENCES matriculas(id) ON DELETE RESTRICT,
    fecha_incidencia DATE NOT NULL,
    tipo_falta VARCHAR(20) NOT NULL CHECK (tipo_falta IN ('LEVE', 'GRAVE', 'MUY_GRAVE')),
    descripcion TEXT NOT NULL,
    reportado_por_usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT,
    requiere_citacion BOOLEAN DEFAULT FALSE NOT NULL,
    estado VARCHAR(20) DEFAULT 'ABIERTA' NOT NULL CHECK (estado IN ('ABIERTA', 'ATENDIDA', 'CERRADA')),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- ------------------------------------------------------------------------------
-- 8. MÓDULO DE EVALUACIÓN CNEB Y REFUERZO ESCOLAR (M4)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS calificaciones_cneb (
    id BIGSERIAL PRIMARY KEY,
    matricula_id BIGINT NOT NULL,
    anio_lectivo_id SMALLINT NOT NULL,
    seccion_id INT NOT NULL,
    periodo_academico_id SMALLINT NOT NULL,
    asignacion_docente_id BIGINT NOT NULL,
    area_curricular_id SMALLINT NOT NULL,
    docente_usuario_id BIGINT NOT NULL,
    competencia_id SMALLINT NOT NULL,
    calificacion_cualitativa VARCHAR(2) NOT NULL CHECK (calificacion_cualitativa IN ('AD', 'A', 'B', 'C')),
    conclusion_descriptiva TEXT,
    sugerencia_ia_utilizada BOOLEAN DEFAULT FALSE NOT NULL,
    requiere_refuerzo BOOLEAN GENERATED ALWAYS AS (calificacion_cualitativa = 'C') STORED,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    -- 1. Coherencia Matrícula: valida año y sección del alumno
    FOREIGN KEY (matricula_id, anio_lectivo_id, seccion_id) 
        REFERENCES matriculas(id, anio_lectivo_id, seccion_id) ON DELETE RESTRICT,
    -- 2. Coherencia Periodo: valida año lectivo del bimestre
    FOREIGN KEY (periodo_academico_id, anio_lectivo_id) 
        REFERENCES periodos_academicos(id, anio_lectivo_id) ON DELETE RESTRICT,
    -- 3. Coherencia Asignación Docente: valida que el docente esté asignado a esa sección, área y año exactos
    FOREIGN KEY (asignacion_docente_id, seccion_id, area_curricular_id, anio_lectivo_id, docente_usuario_id) 
        REFERENCES asignaciones_docentes(id, seccion_id, area_curricular_id, anio_lectivo_id, docente_usuario_id) ON DELETE RESTRICT,
    -- 4. Coherencia Competencia: valida que la competencia pertenezca al área curricular asignada
    FOREIGN KEY (competencia_id, area_curricular_id) 
        REFERENCES competencias(id, area_id) ON DELETE RESTRICT,
    UNIQUE (matricula_id, competencia_id, periodo_academico_id)
);

CREATE TABLE IF NOT EXISTS sesiones_refuerzo (
    id BIGSERIAL PRIMARY KEY,
    anio_lectivo_id SMALLINT NOT NULL,
    periodo_academico_id SMALLINT NOT NULL,
    area_curricular_id SMALLINT NOT NULL REFERENCES areas_curriculares(id) ON DELETE RESTRICT,
    docente_usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT,
    tema VARCHAR(150) NOT NULL,
    fecha_programada DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    aula_asignada VARCHAR(30),
    FOREIGN KEY (periodo_academico_id, anio_lectivo_id) 
        REFERENCES periodos_academicos(id, anio_lectivo_id) ON DELETE RESTRICT,
    CHECK (hora_fin > hora_inicio)
);

CREATE TABLE IF NOT EXISTS inscripciones_refuerzo (
    id BIGSERIAL PRIMARY KEY,
    sesion_refuerzo_id BIGINT NOT NULL REFERENCES sesiones_refuerzo(id) ON DELETE RESTRICT,
    estudiante_id BIGINT NOT NULL REFERENCES estudiantes(id) ON DELETE RESTRICT,
    calificacion_origen_id BIGINT REFERENCES calificaciones_cneb(id) ON DELETE SET NULL,
    estado_asistencia VARCHAR(20) DEFAULT 'PENDIENTE' NOT NULL CHECK (estado_asistencia IN ('PENDIENTE', 'ASISTIO', 'FALTO', 'JUSTIFICADO')),
    observaciones TEXT,
    UNIQUE (sesion_refuerzo_id, estudiante_id)
);

-- ------------------------------------------------------------------------------
-- 9. MÓDULO DE COMUNICADOS INSTITUCIONALES (M6)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS comunicados_oficiales (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(150) NOT NULL,
    contenido TEXT NOT NULL,
    remitente_usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT,
    fecha_publicacion TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    requiere_acuse BOOLEAN DEFAULT FALSE NOT NULL
);

CREATE TABLE IF NOT EXISTS comunicado_destinatarios (
    id BIGSERIAL PRIMARY KEY,
    comunicado_id BIGINT NOT NULL REFERENCES comunicados_oficiales(id) ON DELETE RESTRICT,
    apoderado_id BIGINT NOT NULL REFERENCES apoderados(id) ON DELETE RESTRICT,
    leido BOOLEAN DEFAULT FALSE NOT NULL,
    fecha_lectura TIMESTAMPTZ,
    acuse_confirmado BOOLEAN DEFAULT FALSE NOT NULL,
    fecha_acuse TIMESTAMPTZ,
    UNIQUE (comunicado_id, apoderado_id)
);

-- ------------------------------------------------------------------------------
-- 10. ÍNDICES B-TREE EN LLAVES FORÁNEAS (Rendimiento y Escalabilidad)
-- ------------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_sesiones_usuario ON sesiones (usuario_id);
CREATE INDEX IF NOT EXISTS idx_estudiantes_apellidos ON estudiantes (apellido_paterno, apellido_materno);
CREATE INDEX IF NOT EXISTS idx_estudiante_apoderados_apod ON estudiante_apoderados (apoderado_id);
CREATE INDEX IF NOT EXISTS idx_matriculas_seccion ON matriculas (seccion_id, estado_matricula);
CREATE INDEX IF NOT EXISTS idx_competencias_area ON competencias (area_id);
CREATE INDEX IF NOT EXISTS idx_asignaciones_docente ON asignaciones_docentes (docente_usuario_id);
CREATE INDEX IF NOT EXISTS idx_asignaciones_seccion ON asignaciones_docentes (seccion_id);
CREATE INDEX IF NOT EXISTS idx_horarios_asignacion ON horarios_seccion (asignacion_docente_id);
CREATE INDEX IF NOT EXISTS idx_horarios_bloque ON horarios_seccion (bloque_horario_id);
CREATE INDEX IF NOT EXISTS idx_horarios_docente ON horarios_seccion (docente_usuario_id, dia_semana);
CREATE INDEX IF NOT EXISTS idx_obligaciones_matricula ON obligaciones_pago (matricula_id);
CREATE INDEX IF NOT EXISTS idx_obligaciones_concepto ON obligaciones_pago (concepto_id);
CREATE INDEX IF NOT EXISTS idx_obligaciones_estado_venc ON obligaciones_pago (estado, fecha_vencimiento);
CREATE INDEX IF NOT EXISTS idx_pagos_obligacion_id ON pagos_transacciones (obligacion_pago_id);
CREATE INDEX IF NOT EXISTS idx_comprobantes_pago_id ON comprobantes_pago (pago_transaccion_id);
CREATE INDEX IF NOT EXISTS idx_marcas_lote ON marcas_biometrico_porteria (lote_id);
CREATE INDEX IF NOT EXISTS idx_marcas_porteria_dni_fecha ON marcas_biometrico_porteria (dni_leido, fecha_hora);
CREATE INDEX IF NOT EXISTS idx_incidencias_matricula ON incidencias_conductuales (matricula_id);
CREATE INDEX IF NOT EXISTS idx_incidencias_reportado ON incidencias_conductuales (reportado_por_usuario_id);
CREATE INDEX IF NOT EXISTS idx_calificaciones_docente ON calificaciones_cneb (docente_usuario_id);
CREATE INDEX IF NOT EXISTS idx_calificaciones_competencia ON calificaciones_cneb (competencia_id);
CREATE INDEX IF NOT EXISTS idx_calificaciones_periodo ON calificaciones_cneb (periodo_academico_id);
CREATE INDEX IF NOT EXISTS idx_calificaciones_refuerzo ON calificaciones_cneb (periodo_academico_id, requiere_refuerzo) WHERE requiere_refuerzo = TRUE;
CREATE INDEX IF NOT EXISTS idx_sesiones_refuerzo_periodo ON sesiones_refuerzo (periodo_academico_id);
CREATE INDEX IF NOT EXISTS idx_sesiones_refuerzo_area ON sesiones_refuerzo (area_curricular_id);
CREATE INDEX IF NOT EXISTS idx_inscripciones_refuerzo_est ON inscripciones_refuerzo (estudiante_id);
CREATE INDEX IF NOT EXISTS idx_comunicados_remitente ON comunicados_oficiales (remitente_usuario_id);
CREATE INDEX IF NOT EXISTS idx_comunicado_dest_apod ON comunicado_destinatarios (apoderado_id);

-- ------------------------------------------------------------------------------
-- 11. TRIGGERS AUTOMÁTICOS: VACANTES, FINANZAS, AUDITORÍA Y UPDATED_AT
-- ------------------------------------------------------------------------------

-- 11.1 Auto-actualización de updated_at
CREATE OR REPLACE FUNCTION fn_auto_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_usuarios_updated_at
BEFORE UPDATE ON usuarios
FOR EACH ROW EXECUTE FUNCTION fn_auto_updated_at();

CREATE OR REPLACE TRIGGER trg_calificaciones_updated_at
BEFORE UPDATE ON calificaciones_cneb
FOR EACH ROW EXECUTE FUNCTION fn_auto_updated_at();

-- 11.2 Control Atómico de Vacantes de Matrícula (Incremento / Decremento / Cambio de Sección)
CREATE OR REPLACE FUNCTION fn_gestionar_vacante_matricula()
RETURNS TRIGGER AS $$
DECLARE
    v_consume_antiguo BOOLEAN := FALSE;
    v_consume_nuevo BOOLEAN := FALSE;
BEGIN
    IF (TG_OP = 'UPDATE') THEN
        v_consume_antiguo := (OLD.estado_matricula IN ('RESERVADA_TEMPORAL', 'MATRICULADO'));
    END IF;
    v_consume_nuevo := (NEW.estado_matricula IN ('RESERVADA_TEMPORAL', 'MATRICULADO'));

    -- Caso A: Pasa a consumir vacante (Insert o activación de solicitud)
    IF (NOT v_consume_antiguo AND v_consume_nuevo) THEN
        UPDATE secciones
        SET vacantes_ocupadas = vacantes_ocupadas + 1
        WHERE id = NEW.seccion_id AND vacantes_ocupadas < cupo_maximo;
        
        IF NOT FOUND THEN
            RAISE EXCEPTION 'No hay vacantes disponibles en la sección seleccionada (Cupo máximo alcanzado)';
        END IF;

    -- Caso B: Pasa de consumir a no consumir (Cancelada, Trasladada, Retirada)
    ELSIF (v_consume_antiguo AND NOT v_consume_nuevo) THEN
        UPDATE secciones
        SET vacantes_ocupadas = vacantes_ocupadas - 1
        WHERE id = OLD.seccion_id;

    -- Caso C: Cambio de sección mientras se consume vacante
    ELSIF (v_consume_antiguo AND v_consume_nuevo AND OLD.seccion_id != NEW.seccion_id) THEN
        UPDATE secciones
        SET vacantes_ocupadas = vacantes_ocupadas - 1
        WHERE id = OLD.seccion_id;

        UPDATE secciones
        SET vacantes_ocupadas = vacantes_ocupadas + 1
        WHERE id = NEW.seccion_id AND vacantes_ocupadas < cupo_maximo;

        IF NOT FOUND THEN
            RAISE EXCEPTION 'No hay vacantes disponibles en la nueva sección de destino';
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_gestionar_vacante_matricula
AFTER INSERT OR UPDATE OF estado_matricula, seccion_id ON matriculas
FOR EACH ROW EXECUTE FUNCTION fn_gestionar_vacante_matricula();

-- Procedimiento almacenado para liberar reservas vencidas automáticamente
CREATE OR REPLACE FUNCTION fn_liberar_reservas_expiradas()
RETURNS INTEGER AS $$
DECLARE
    v_afectadas INTEGER;
BEGIN
    WITH expiradas AS (
        UPDATE matriculas
        SET estado_matricula = 'CANCELADA'
        WHERE estado_matricula = 'RESERVADA_TEMPORAL'
          AND reserva_expira_at < CURRENT_TIMESTAMP
        RETURNING id
    )
    SELECT count(*) INTO v_afectadas FROM expiradas;
    RETURN v_afectadas;
END;
$$ LANGUAGE plpgsql;

-- 11.3 Control Financiero de Pagos y Reversiones (INSERT y UPDATE)
CREATE OR REPLACE FUNCTION fn_aplicar_pago_obligacion()
RETURNS TRIGGER AS $$
DECLARE
    v_monto_total_deuda NUMERIC(10, 2);
    v_total_pagado_actual NUMERIC(10, 2);
    v_delta NUMERIC(10, 2) := 0.00;
BEGIN
    IF (TG_OP = 'INSERT') THEN
        IF NEW.estado_pago = 'APROBADO' THEN
            v_delta := NEW.monto_pagado;
        END IF;
    ELSIF (TG_OP = 'UPDATE') THEN
        -- De no aprobado a APROBADO
        IF (OLD.estado_pago != 'APROBADO' AND NEW.estado_pago = 'APROBADO') THEN
            v_delta := NEW.monto_pagado;
        -- De APROBADO a REVERTIDO o RECHAZADO
        ELSIF (OLD.estado_pago = 'APROBADO' AND NEW.estado_pago IN ('REVERTIDO', 'RECHAZADO')) THEN
            v_delta := -OLD.monto_pagado;
        -- Modificación de importe en pago aprobado
        ELSIF (OLD.estado_pago = 'APROBADO' AND NEW.estado_pago = 'APROBADO') THEN
            v_delta := NEW.monto_pagado - OLD.monto_pagado;
        END IF;
    END IF;

    IF v_delta != 0.00 THEN
        SELECT (monto_base + monto_mora - monto_descuento), total_pagado
        INTO v_monto_total_deuda, v_total_pagado_actual
        FROM obligaciones_pago
        WHERE id = NEW.obligacion_pago_id
        FOR UPDATE;

        IF (v_total_pagado_actual + v_delta) > v_monto_total_deuda THEN
            RAISE EXCEPTION 'El pago de S/ % excede el saldo pendiente de la obligación (Deuda: S/ %, Total pagado previo: S/ %)',
                v_delta, v_monto_total_deuda, v_total_pagado_actual;
        END IF;

        IF (v_total_pagado_actual + v_delta) < 0.00 THEN
            RAISE EXCEPTION 'La reversión genera un total pagado negativo en la obligación';
        END IF;

        UPDATE obligaciones_pago
        SET total_pagado = total_pagado + v_delta,
            estado = CASE 
                WHEN (total_pagado + v_delta) = v_monto_total_deuda THEN 'PAGADO_TOTAL'
                WHEN (total_pagado + v_delta) > 0.00 THEN 'PAGADO_PARCIAL'
                ELSE 'PENDIENTE'
            END
        WHERE id = NEW.obligacion_pago_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_aplicar_pago_obligacion
AFTER INSERT OR UPDATE OF estado_pago, monto_pagado ON pagos_transacciones
FOR EACH ROW EXECUTE FUNCTION fn_aplicar_pago_obligacion();

-- 11.4 Inmutabilidad de Imputación de Pagos (Evita desincronización de obligaciones)
CREATE OR REPLACE FUNCTION fn_prevenir_cambio_obligacion_pago()
RETURNS TRIGGER AS $$
BEGIN
    IF (NEW.obligacion_pago_id != OLD.obligacion_pago_id) THEN
        RAISE EXCEPTION 'Operación rechazada: La obligación_pago_id de un pago registrado es inmutable. Si hubo un error contable, anule o revierta el pago original y registre uno nuevo.';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_prevenir_cambio_obligacion_pago
BEFORE UPDATE OF obligacion_pago_id ON pagos_transacciones
FOR EACH ROW EXECUTE FUNCTION fn_prevenir_cambio_obligacion_pago();

-- 11.5 Ciclo de Vida Financiero y Anulación Automática de Comprobantes
CREATE OR REPLACE FUNCTION fn_validar_transicion_pago()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.estado_pago = 'REVERTIDO' AND NEW.estado_pago <> 'REVERTIDO' THEN
        RAISE EXCEPTION 'Operación rechazada: un pago REVERTIDO es definitivo. Registre una nueva transacción para cualquier cobro posterior.';
    END IF;

    IF OLD.estado_pago = 'REVERTIDO' AND NEW.monto_pagado <> OLD.monto_pagado THEN
        RAISE EXCEPTION 'Operación rechazada: no se puede modificar el importe de un pago REVERTIDO.';
    END IF;

    IF OLD.estado_pago = 'APROBADO' AND NEW.estado_pago = 'RECHAZADO' THEN
        RAISE EXCEPTION 'Operación rechazada: un pago APROBADO debe pasar a REVERTIDO para conservar su trazabilidad contable.';
    END IF;

    IF OLD.estado_pago = 'APROBADO'
       AND NEW.estado_pago = 'APROBADO'
       AND NEW.monto_pagado <> OLD.monto_pagado
       AND EXISTS (SELECT 1 FROM comprobantes_pago WHERE pago_transaccion_id = OLD.id) THEN
        RAISE EXCEPTION 'Operación rechazada: no se puede modificar el importe de un pago que ya tiene comprobante emitido. Revierta el pago y registre uno nuevo.';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_validar_transicion_pago
BEFORE UPDATE OF estado_pago, monto_pagado ON pagos_transacciones
FOR EACH ROW EXECUTE FUNCTION fn_validar_transicion_pago();

CREATE OR REPLACE FUNCTION fn_validar_comprobante_pago()
RETURNS TRIGGER AS $$
DECLARE
    v_estado_pago VARCHAR(20);
    v_monto_pagado NUMERIC(10, 2);
BEGIN
    SELECT estado_pago, monto_pagado
    INTO v_estado_pago, v_monto_pagado
    FROM pagos_transacciones
    WHERE id = NEW.pago_transaccion_id
    FOR UPDATE;

    IF v_estado_pago IS DISTINCT FROM 'APROBADO' THEN
        RAISE EXCEPTION 'Solo se puede emitir un comprobante para un pago APROBADO.';
    END IF;

    IF NEW.estado_comprobante <> 'EMITIDO' THEN
        RAISE EXCEPTION 'Un comprobante nuevo debe emitirse con estado EMITIDO.';
    END IF;

    IF NEW.monto_total <> v_monto_pagado THEN
        RAISE EXCEPTION 'El monto del comprobante debe coincidir con el monto aprobado de la transacción.';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_validar_comprobante_pago
BEFORE INSERT ON comprobantes_pago
FOR EACH ROW EXECUTE FUNCTION fn_validar_comprobante_pago();

CREATE OR REPLACE FUNCTION fn_validar_estado_comprobante_pago()
RETURNS TRIGGER AS $$
DECLARE
    v_estado_pago VARCHAR(20);
BEGIN
    SELECT estado_pago
    INTO v_estado_pago
    FROM pagos_transacciones
    WHERE id = NEW.pago_transaccion_id
    FOR UPDATE;

    IF OLD.estado_comprobante = 'ANULADO' AND NEW.estado_comprobante <> 'ANULADO' THEN
        RAISE EXCEPTION 'Operación rechazada: un comprobante ANULADO no puede volver a emitirse.';
    END IF;

    IF NEW.estado_comprobante = 'ANULADO' AND v_estado_pago NOT IN ('REVERTIDO', 'RECHAZADO') THEN
        RAISE EXCEPTION 'Operación rechazada: solo se puede anular un comprobante cuando su pago está REVERTIDO o RECHAZADO.';
    END IF;

    IF OLD.estado_comprobante = 'ANULADO'
       AND (NEW.fecha_anulacion IS DISTINCT FROM OLD.fecha_anulacion
            OR NEW.motivo_anulacion IS DISTINCT FROM OLD.motivo_anulacion) THEN
        RAISE EXCEPTION 'Operación rechazada: los datos de anulación de un comprobante son inmutables.';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_validar_estado_comprobante_pago
BEFORE UPDATE OF estado_comprobante, fecha_anulacion, motivo_anulacion ON comprobantes_pago
FOR EACH ROW EXECUTE FUNCTION fn_validar_estado_comprobante_pago();

CREATE OR REPLACE FUNCTION fn_prevenir_cambio_pago_comprobante()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.pago_transaccion_id <> OLD.pago_transaccion_id THEN
        RAISE EXCEPTION 'Operación rechazada: la transacción vinculada a un comprobante es inmutable.';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_prevenir_cambio_pago_comprobante
BEFORE UPDATE OF pago_transaccion_id ON comprobantes_pago
FOR EACH ROW EXECUTE FUNCTION fn_prevenir_cambio_pago_comprobante();

CREATE OR REPLACE FUNCTION fn_anular_comprobante_pago()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.estado_pago = 'APROBADO' AND NEW.estado_pago IN ('REVERTIDO', 'RECHAZADO') THEN
        UPDATE comprobantes_pago
        SET estado_comprobante = 'ANULADO',
            fecha_anulacion = CURRENT_TIMESTAMP,
            motivo_anulacion = 'Pago ' || NEW.estado_pago || ' en la transacción ' || NEW.id
        WHERE pago_transaccion_id = NEW.id
          AND estado_comprobante = 'EMITIDO';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_anular_comprobante_pago
AFTER UPDATE OF estado_pago ON pagos_transacciones
FOR EACH ROW EXECUTE FUNCTION fn_anular_comprobante_pago();

-- 11.6 Auditoría de Cambios Activa en Tablas Críticas con Registro de Actor
CREATE OR REPLACE FUNCTION fn_auditar_cambios()
RETURNS TRIGGER AS $$
DECLARE
    v_usuario_id BIGINT;
    v_setting_val TEXT;
BEGIN
    -- Captura el ID del usuario autenticado en la sesión/transacción de Spring Boot
    v_setting_val := current_setting('app.current_user_id', true);
    IF v_setting_val IS NOT NULL AND v_setting_val ~ '^[0-9]+$' THEN
        v_usuario_id := v_setting_val::BIGINT;
    ELSE
        v_usuario_id := NULL;
    END IF;

    IF (TG_OP = 'INSERT') THEN
        INSERT INTO auditoria_cambios (tabla_afectada, registro_id, accion, datos_nuevos, usuario_id)
        VALUES (TG_TABLE_NAME, NEW.id, 'INSERT', to_jsonb(NEW), v_usuario_id);
        RETURN NEW;
    ELSIF (TG_OP = 'UPDATE') THEN
        INSERT INTO auditoria_cambios (tabla_afectada, registro_id, accion, datos_anteriores, datos_nuevos, usuario_id)
        VALUES (TG_TABLE_NAME, NEW.id, 'UPDATE', to_jsonb(OLD), to_jsonb(NEW), v_usuario_id);
        RETURN NEW;
    ELSIF (TG_OP = 'DELETE') THEN
        INSERT INTO auditoria_cambios (tabla_afectada, registro_id, accion, datos_anteriores, usuario_id)
        VALUES (TG_TABLE_NAME, OLD.id, 'DELETE', to_jsonb(OLD), v_usuario_id);
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_auditar_calificaciones
AFTER INSERT OR UPDATE OR DELETE ON calificaciones_cneb
FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambios();

CREATE OR REPLACE TRIGGER trg_auditar_matriculas
AFTER INSERT OR UPDATE OR DELETE ON matriculas
FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambios();

CREATE OR REPLACE TRIGGER trg_auditar_obligaciones
AFTER INSERT OR UPDATE OR DELETE ON obligaciones_pago
FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambios();

CREATE OR REPLACE TRIGGER trg_auditar_pagos
AFTER INSERT OR UPDATE OR DELETE ON pagos_transacciones
FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambios();

CREATE OR REPLACE TRIGGER trg_auditar_comprobantes
AFTER INSERT OR UPDATE OR DELETE ON comprobantes_pago
FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambios();

-- ------------------------------------------------------------------------------
-- 12. DATOS SEMILLA BÁSICOS (Catálogos y Parámetros Iniciales)
-- ------------------------------------------------------------------------------
INSERT INTO roles (codigo, nombre, descripcion) VALUES
    ('DIRECCION', 'Dirección General', 'Acceso estratégico, reportería y configuración institucional'),
    ('SECRETARIA', 'Secretaría Académica', 'Gestión de matrículas, vacantes, expedientes y caja'),
    ('DOCENTE', 'Plana Docente', 'Registro de notas CNEB, asistencia a refuerzo y comunicados'),
    ('AUXILIAR', 'Auxiliar de Educación', 'Pase de lista diario, importación biométrica e incidencias'),
    ('TUTOR', 'Tutor de Aula', 'Seguimiento formativo, conciliación de ausencias y citaciones'),
    ('APODERADO', 'Padre de Familia / Tutor', 'Acceso al portal para pago de pensiones y boletas')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO niveles (codigo, nombre) VALUES
    ('INICIAL', 'Educación Inicial'),
    ('PRIMARIA', 'Educación Primaria'),
    ('SECUNDARIA', 'Educación Secundaria')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO conceptos_cobro (codigo, nombre, tipo_concepto, monto_sugerido) VALUES
    ('MATRICULA_ANUAL', 'Derecho de Matrícula y Admisión Anual', 'MATRICULA', 350.00),
    ('PENSION_MENSUAL', 'Pensión Escolar de Enseñanza (Cuotas 1 a 10)', 'PENSION', 380.00),
    ('CONSTANCIA_ESTUDIOS', 'Emisión de Constancia de Estudios', 'CERTIFICADO', 25.00)
ON CONFLICT (codigo) DO NOTHING;

-- Contraseña sin hash para el usuario de práctica (cambiar antes de cualquier uso real): Admin2026!
INSERT INTO usuarios (username, email, password_hash, activo)
VALUES ('fq94289@gmail.com', 'fq94289@gmail.com', '$2a$10$m.2CkRxR18uYeRfeZKj8uOc3tcA77GRnQ1TVnhS9.GaOKLr2IMUa2', TRUE)
ON CONFLICT (email) DO NOTHING;

INSERT INTO usuario_roles (usuario_id, rol_id)
SELECT u.id, r.id
FROM usuarios u
JOIN roles r ON r.codigo = 'DIRECCION'
WHERE u.email = 'fq94289@gmail.com'
ON CONFLICT (usuario_id, rol_id) DO NOTHING;


-- <<< FIN V1__schema_colegio_shuji.sql

-- >>> INICIO V2__series_indices_seguridad_y_rls.sql

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

-- <<< FIN V2__series_indices_seguridad_y_rls.sql

-- >>> INICIO V3__aulas_e_idempotencia_biometrica.sql

-- Catálogo institucional de ambientes físicos sin cambiar el contrato actual de secciones.
CREATE TABLE aulas (
    id SERIAL PRIMARY KEY,
    codigo VARCHAR(30) UNIQUE NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    ubicacion VARCHAR(150),
    capacidad SMALLINT NOT NULL CHECK (capacidad > 0),
    activa BOOLEAN DEFAULT TRUE NOT NULL
);

UPDATE secciones
SET aula_fisica = 'SIN-ASIGNAR-' || id
WHERE aula_fisica IS NULL OR btrim(aula_fisica) = '';

INSERT INTO aulas (codigo, nombre, capacidad)
SELECT upper(btrim(s.aula_fisica)),
       'Aula ' || upper(btrim(s.aula_fisica)),
       max(s.cupo_maximo)
FROM secciones s
GROUP BY upper(btrim(s.aula_fisica))
ON CONFLICT (codigo) DO NOTHING;

ALTER TABLE secciones ADD COLUMN aula_id INT;

UPDATE secciones s
SET aula_id = a.id,
    aula_fisica = a.codigo
FROM aulas a
WHERE a.codigo = upper(btrim(s.aula_fisica));

ALTER TABLE secciones ALTER COLUMN aula_id SET NOT NULL;
ALTER TABLE secciones
    ADD CONSTRAINT fk_secciones_aula
    FOREIGN KEY (aula_id) REFERENCES aulas(id) ON DELETE RESTRICT;
CREATE UNIQUE INDEX uq_secciones_anio_aula_id ON secciones (anio_lectivo_id, aula_id);

-- Identidad estable del contenido para impedir reprocesar el mismo lote.
ALTER TABLE lotes_biometrico ADD COLUMN hash_contenido VARCHAR(64);
UPDATE lotes_biometrico
SET hash_contenido = encode(
    digest(id::text || '|' || nombre_archivo || '|' || created_at::text, 'sha256'),
    'hex');
ALTER TABLE lotes_biometrico ALTER COLUMN hash_contenido SET NOT NULL;
ALTER TABLE lotes_biometrico
    ADD CONSTRAINT uq_lotes_biometrico_hash_contenido UNIQUE (hash_contenido);

UPDATE marcas_biometrico_porteria
SET dispositivo_codigo = 'PORTERIA_01'
WHERE dispositivo_codigo IS NULL OR btrim(dispositivo_codigo) = '';
ALTER TABLE marcas_biometrico_porteria ALTER COLUMN dispositivo_codigo SET NOT NULL;

WITH repetidas AS (
    SELECT id,
           row_number() OVER (
               PARTITION BY dni_leido, fecha_hora, dispositivo_codigo
               ORDER BY id) AS numero
    FROM marcas_biometrico_porteria
    WHERE estado_procesamiento <> 'DUPLICADO'
)
UPDATE marcas_biometrico_porteria m
SET estado_procesamiento = 'DUPLICADO'
FROM repetidas r
WHERE m.id = r.id AND r.numero > 1;

CREATE UNIQUE INDEX uq_marcas_porteria_evento_valido
ON marcas_biometrico_porteria (dni_leido, fecha_hora, dispositivo_codigo)
WHERE estado_procesamiento <> 'DUPLICADO';

-- La Data API no debe exponer el nuevo catálogo; el acceso continúa por el backend.
ALTER TABLE aulas ENABLE ROW LEVEL SECURITY;
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
        REVOKE ALL ON aulas FROM anon;
        REVOKE ALL ON SEQUENCE aulas_id_seq FROM anon;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
        REVOKE ALL ON aulas FROM authenticated;
        REVOKE ALL ON SEQUENCE aulas_id_seq FROM authenticated;
    END IF;
END
$$;

-- <<< FIN V3__aulas_e_idempotencia_biometrica.sql

-- >>> INICIO V4__default_privileges_supabase.sql

-- ==============================================================================
-- MIGRACIÓN FLYWAY V4: REVOCACIÓN DE PRIVILEGIOS POR DEFECTO PARA POSTGREST
-- ==============================================================================

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
        ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON TABLES FROM anon;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON SEQUENCES FROM anon;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON ROUTINES FROM anon;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
        ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON TABLES FROM authenticated;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON SEQUENCES FROM authenticated;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON ROUTINES FROM authenticated;
    END IF;
END
$$;

-- <<< FIN V4__default_privileges_supabase.sql

-- >>> INICIO V5__indices_claves_foraneas.sql

-- =============================================================================
-- MIGRACIÓN FLYWAY V5: ÍNDICES DE SOPORTE PARA CLAVES FORÁNEAS
-- =============================================================================
-- PostgreSQL no crea automáticamente índices sobre las columnas que referencian
-- una FK. Estos índices aceleran joins y la comprobación de RESTRICT/SET NULL al
-- modificar filas padre. Se conservan las migraciones históricas V1-V4 intactas.

CREATE INDEX IF NOT EXISTS idx_usuario_roles_rol_id
    ON usuario_roles (rol_id);

CREATE INDEX IF NOT EXISTS idx_auditoria_cambios_usuario_id
    ON auditoria_cambios (usuario_id);

CREATE INDEX IF NOT EXISTS idx_secciones_grado_nivel
    ON secciones (grado_id, nivel_id);

CREATE INDEX IF NOT EXISTS idx_secciones_aula_id
    ON secciones (aula_id);

CREATE INDEX IF NOT EXISTS idx_asignaciones_area_nivel
    ON asignaciones_docentes (area_curricular_id, nivel_id);

CREATE INDEX IF NOT EXISTS idx_horarios_anio_lectivo_id
    ON horarios_seccion (anio_lectivo_id);

CREATE INDEX IF NOT EXISTS idx_lotes_biometrico_importado_por
    ON lotes_biometrico (importado_por_usuario_id);

CREATE INDEX IF NOT EXISTS idx_marcas_porteria_estudiante_id
    ON marcas_biometrico_porteria (estudiante_id);

CREATE INDEX IF NOT EXISTS idx_asistencias_aula_auxiliar_id
    ON asistencias_aula (auxiliar_usuario_id);

CREATE INDEX IF NOT EXISTS idx_conciliaciones_estudiante_id
    ON conciliaciones_asistencia (estudiante_id);

CREATE INDEX IF NOT EXISTS idx_calificaciones_asignacion_periodo
    ON calificaciones_cneb (asignacion_docente_id, periodo_academico_id);

CREATE INDEX IF NOT EXISTS idx_sesiones_refuerzo_docente_id
    ON sesiones_refuerzo (docente_usuario_id);

CREATE INDEX IF NOT EXISTS idx_inscripciones_calificacion_origen_id
    ON inscripciones_refuerzo (calificacion_origen_id);

-- <<< FIN V5__indices_claves_foraneas.sql
