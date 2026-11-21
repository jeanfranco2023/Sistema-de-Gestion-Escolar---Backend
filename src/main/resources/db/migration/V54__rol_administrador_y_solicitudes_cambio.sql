-- Hito 5: Separación de funciones (SoD) con rol ADMINISTRADOR y gestión de Solicitudes de Cambio (RFC serie CH-)

-- 1. Actualizar constraint chk_roles_codigo si existiera en despliegues heredados e insertar rol ADMINISTRADOR
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_roles_codigo'
    ) THEN
        ALTER TABLE public.roles DROP CONSTRAINT chk_roles_codigo;
    END IF;
END $$;

INSERT INTO public.roles (codigo_rol, nombre_rol)
VALUES ('ADMINISTRADOR', 'Administrador del Sistema')
ON CONFLICT (codigo_rol) DO NOTHING;

-- 2. Alta inicial de cuenta administradora autorizada (conservando intacta la cuenta de Dirección)
INSERT INTO public.usuarios (
    nombre_usuario,
    correo_electronico,
    usuario_password,
    activo,
    debe_cambiar_password,
    nombre_completo,
    estado_laboral
)
SELECT
    'admin',
    'admin@shujikitamura.edu.pe',
    COALESCE(
        (SELECT u.usuario_password
         FROM public.usuarios u
         JOIN public.usuario_roles ur ON ur.usuario_id = u.id
         JOIN public.roles r ON r.id = ur.rol_id
         WHERE r.codigo_rol = 'DIRECCION'
         ORDER BY u.id ASC
         LIMIT 1),
        (SELECT u2.usuario_password
         FROM public.usuarios u2
         ORDER BY u2.id ASC
         LIMIT 1),
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'
    ),
    TRUE,
    FALSE,
    'Administrador del Sistema',
    'ACTIVO'
WHERE NOT EXISTS (
    SELECT 1 FROM public.usuarios
    WHERE nombre_usuario = 'admin' OR correo_electronico = 'admin@shujikitamura.edu.pe'
);

INSERT INTO public.usuario_roles (usuario_id, rol_id)
SELECT u.id, r.id
FROM public.usuarios u
JOIN public.roles r ON r.codigo_rol = 'ADMINISTRADOR'
WHERE u.nombre_usuario = 'admin' OR u.correo_electronico = 'admin@shujikitamura.edu.pe'
ON CONFLICT (usuario_id, rol_id) DO NOTHING;

-- 3. Tablas para gestión de Solicitudes de Cambio (RFC serie CH-)
CREATE TABLE IF NOT EXISTS public.solicitudes_cambio (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(20) NOT NULL UNIQUE,
    titulo VARCHAR(200) NOT NULL,
    modulo VARCHAR(100) NOT NULL,
    motivo_problema TEXT NOT NULL,
    resultado_esperado TEXT NOT NULL,
    alcance_impacto TEXT,
    evidencia_inicial TEXT,
    solicitante_id BIGINT NOT NULL,
    solicitante_nombre VARCHAR(180) NOT NULL,
    solicitante_email VARCHAR(180) NOT NULL,
    estado VARCHAR(40) NOT NULL,
    prioridad VARCHAR(30) NOT NULL DEFAULT 'MEDIA',
    impacto_evaluado TEXT,
    responsable_id BIGINT,
    responsable_nombre VARCHAR(180),
    aprobador_nombre VARCHAR(180),
    registrado_decision_por VARCHAR(180),
    fecha_decision TIMESTAMPTZ,
    motivo_decision TEXT,
    resumen_cambio TEXT,
    enlace_evidencia_tecnica VARCHAR(500),
    plan_reversion TEXT,
    prueba_posterior TEXT,
    resultado_prueba VARCHAR(120),
    verificacion_cierre TEXT,
    cerrado_por_nombre VARCHAR(180),
    idempotency_key VARCHAR(120) UNIQUE,
    fecha_solicitud TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_cierre TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS public.solicitudes_cambio_eventos (
    id BIGSERIAL PRIMARY KEY,
    solicitud_id BIGINT NOT NULL REFERENCES public.solicitudes_cambio(id) ON DELETE CASCADE,
    actor_id BIGINT,
    actor_nombre VARCHAR(180) NOT NULL,
    actor_rol VARCHAR(60) NOT NULL,
    estado_anterior VARCHAR(40),
    estado_nuevo VARCHAR(40),
    tipo_evento VARCHAR(60) NOT NULL,
    comentario VARCHAR(2000),
    visible_direccion BOOLEAN NOT NULL DEFAULT TRUE,
    evidencia_url VARCHAR(500),
    fecha_evento TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.solicitudes_cambio_notificaciones (
    id BIGSERIAL PRIMARY KEY,
    solicitud_id BIGINT NOT NULL REFERENCES public.solicitudes_cambio(id) ON DELETE CASCADE,
    evento_id BIGINT,
    destinatario_email VARCHAR(180) NOT NULL,
    asunto VARCHAR(255) NOT NULL,
    cuerpo_resumen TEXT,
    estado_envio VARCHAR(30) NOT NULL,
    intentos INT NOT NULL DEFAULT 1,
    ultimo_error TEXT,
    clave_idempotencia VARCHAR(160) NOT NULL UNIQUE,
    fecha_intento TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_solicitudes_cambio_solicitante ON public.solicitudes_cambio(solicitante_id, fecha_solicitud DESC);
CREATE INDEX IF NOT EXISTS idx_solicitudes_cambio_estado ON public.solicitudes_cambio(estado, fecha_solicitud DESC);
CREATE INDEX IF NOT EXISTS idx_solicitudes_cambio_eventos_solicitud ON public.solicitudes_cambio_eventos(solicitud_id, fecha_evento ASC);
CREATE INDEX IF NOT EXISTS idx_solicitudes_cambio_notif_solicitud ON public.solicitudes_cambio_notificaciones(solicitud_id, fecha_intento DESC);

ALTER TABLE public.solicitudes_cambio ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.solicitudes_cambio_eventos ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.solicitudes_cambio_notificaciones ENABLE ROW LEVEL SECURITY;
