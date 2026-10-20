-- COLEGIO SHUJI KITAMURA — ESTRUCTURA V22 (2026-10-20)
-- PostgreSQL 17 / Supabase. Define directamente el estado final de las 38 tablas.
-- No concatena versiones históricas ni incluye atributos eliminados.
-- roles: id, codigo_rol VARCHAR(20), nombre_rol VARCHAR(30).
-- Solo para crear una base VACÍA o consultar/importar su modelo.
-- No ejecutar sobre la base existente, que puede tener migraciones pendientes.
-- Sin usuarios de práctica, credenciales, datos personales ni historial Flyway.
-- Si se inicializa una base con este archivo, registrar el baseline V22 en Flyway
-- antes de iniciar el backend; para bases existentes usar las migraciones pendientes.

CREATE SCHEMA IF NOT EXISTS extensions;
CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA extensions;
CREATE EXTENSION IF NOT EXISTS unaccent WITH SCHEMA extensions;
SET search_path TO public, extensions;

-- 1. ESTRUCTURA: funciones, tablas, secuencias, restricciones, índices, triggers y RLS.
CREATE SCHEMA IF NOT EXISTS public;

COMMENT ON SCHEMA public IS 'standard public schema';

CREATE FUNCTION public.fn_actualizar_fecha() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.fecha_actualizacion = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;

CREATE FUNCTION public.fn_anular_comprobante_pago() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;

CREATE FUNCTION public.fn_aplicar_pago_obligacion() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;

CREATE FUNCTION public.fn_auditar_cambios() RETURNS trigger
    LANGUAGE plpgsql
    AS $_$
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
$_$;

CREATE FUNCTION public.fn_gestionar_vacante_matricula() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;

CREATE FUNCTION public.fn_liberar_reservas_expiradas() RETURNS integer
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_afectadas INTEGER;
BEGIN
    WITH expiradas AS (
        UPDATE matriculas
        SET estado_matricula = 'CANCELADA'
        WHERE estado_matricula = 'RESERVADA_TEMPORAL'
          AND fecha_expiracion_reserva < CURRENT_TIMESTAMP
        RETURNING id
    )
    SELECT count(*) INTO v_afectadas FROM expiradas;
    RETURN v_afectadas;
END;
$$;

CREATE FUNCTION public.fn_prevenir_cambio_obligacion_pago() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF (NEW.obligacion_pago_id != OLD.obligacion_pago_id) THEN
        RAISE EXCEPTION 'Operación rechazada: La obligación_pago_id de un pago registrado es inmutable. Si hubo un error contable, anule o revierta el pago original y registre uno nuevo.';
    END IF;
    RETURN NEW;
END;
$$;

CREATE FUNCTION public.fn_prevenir_cambio_pago_comprobante() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF NEW.pago_transaccion_id <> OLD.pago_transaccion_id THEN
        RAISE EXCEPTION 'Operación rechazada: la transacción vinculada a un comprobante es inmutable.';
    END IF;
    RETURN NEW;
END;
$$;

CREATE FUNCTION public.fn_validar_comprobante_pago() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;

CREATE FUNCTION public.fn_validar_estado_comprobante_pago() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;

CREATE FUNCTION public.fn_validar_transicion_pago() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;

CREATE TABLE public.anios_lectivos (
    id smallint NOT NULL,
    anio smallint NOT NULL,
    fecha_inicio date NOT NULL,
    fecha_fin date NOT NULL,
    abierto boolean DEFAULT true NOT NULL,
    CONSTRAINT anios_lectivos_anio_check CHECK ((anio >= 2024)),
    CONSTRAINT anios_lectivos_check CHECK ((fecha_fin > fecha_inicio))
);

CREATE SEQUENCE public.anios_lectivos_id_seq
    AS smallint
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.anios_lectivos_id_seq OWNED BY public.anios_lectivos.id;

CREATE TABLE public.apoderados (
    id bigint NOT NULL,
    usuario_id bigint,
    tipo_documento character varying(3) DEFAULT 'DNI'::character varying NOT NULL,
    numero_documento character varying(8) NOT NULL,
    nombres_apoderado character varying(20) NOT NULL,
    apellido_paterno_apoderado character varying(20) NOT NULL,
    apellido_materno_apoderado character varying(20) NOT NULL,
    celular character varying(9) NOT NULL,
    correo_electronico character varying(50),
    direccion character varying(150) NOT NULL,
    ubigeo_inei character varying(6) NOT NULL,
    validado_reniec boolean DEFAULT false NOT NULL,
    origen_registro character varying(20) DEFAULT 'RENIEC_API'::character varying NOT NULL,
    fecha_creacion timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT apoderados_celular_check CHECK (((celular)::text ~ '^9[0-9]{8}$'::text)),
    CONSTRAINT apoderados_origen_registro_check CHECK ((origen_registro IN ('RENIEC_API', 'MANUAL_CONTINGENCIA'))),
    CONSTRAINT apoderados_tipo_documento_check CHECK ((tipo_documento = 'DNI')),
    CONSTRAINT apoderados_ubigeo_inei_check CHECK (((ubigeo_inei)::text ~ '^[0-9]{6}$'::text)),
    CONSTRAINT chk_apoderados_dni_8_digitos CHECK (((numero_documento)::text ~ '^[0-9]{8}$'::text))
);

CREATE SEQUENCE public.apoderados_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.apoderados_id_seq OWNED BY public.apoderados.id;

CREATE TABLE public.areas_curriculares (
    id smallint NOT NULL,
    nivel_id smallint NOT NULL,
    codigo_area character varying(20) NOT NULL,
    nombre_area character varying(60) NOT NULL
);

CREATE SEQUENCE public.areas_curriculares_id_seq
    AS smallint
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.areas_curriculares_id_seq OWNED BY public.areas_curriculares.id;

CREATE TABLE public.asignaciones_docentes (
    id bigint NOT NULL,
    docente_usuario_id bigint NOT NULL,
    seccion_id integer NOT NULL,
    anio_lectivo_id smallint NOT NULL,
    nivel_id smallint NOT NULL,
    area_curricular_id smallint NOT NULL
);

CREATE SEQUENCE public.asignaciones_docentes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.asignaciones_docentes_id_seq OWNED BY public.asignaciones_docentes.id;

CREATE TABLE public.asistencias_aula (
    id bigint NOT NULL,
    matricula_id bigint NOT NULL,
    fecha_sesion date NOT NULL,
    hora_registro time without time zone NOT NULL,
    estado character varying(25) NOT NULL,
    auxiliar_usuario_id bigint,
    justificada boolean DEFAULT false NOT NULL,
    motivo_justificacion text,
    documento_sustento_url character varying(255),
    CONSTRAINT asistencias_aula_estado_check CHECK ((estado IN ('PRESENTE', 'TARDANZA', 'FALTA_INJUSTIFICADA', 'FALTA_JUSTIFICADA'))),
    CONSTRAINT chk_asistencias_justificacion_longitud CHECK ((char_length(motivo_justificacion) <= 4000))
);

CREATE SEQUENCE public.asistencias_aula_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.asistencias_aula_id_seq OWNED BY public.asistencias_aula.id;

CREATE TABLE public.auditoria_cambios (
    id bigint NOT NULL,
    tabla_afectada character varying(50) NOT NULL,
    registro_id bigint NOT NULL,
    accion character varying(10) NOT NULL,
    datos_anteriores jsonb,
    datos_nuevos jsonb,
    usuario_id bigint,
    fecha_creacion timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT auditoria_cambios_accion_check CHECK ((accion IN ('INSERT', 'UPDATE', 'DELETE')))
);

CREATE SEQUENCE public.auditoria_cambios_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.auditoria_cambios_id_seq OWNED BY public.auditoria_cambios.id;

CREATE TABLE public.aulas (
    id integer NOT NULL,
    codigo_aula character varying(30) NOT NULL,
    nombre_aula character varying(40) NOT NULL,
    ubicacion character varying(60),
    capacidad smallint NOT NULL,
    activa boolean DEFAULT true NOT NULL,
    CONSTRAINT aulas_capacidad_check CHECK ((capacidad > 0)),
    CONSTRAINT chk_aulas_codigo_sin_espacios_externos CHECK (((codigo_aula)::text = btrim((codigo_aula)::text)))
);

CREATE SEQUENCE public.aulas_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.aulas_id_seq OWNED BY public.aulas.id;

CREATE TABLE public.bloques_horarios (
    id smallint NOT NULL,
    numero_bloque smallint NOT NULL,
    hora_inicio time without time zone NOT NULL,
    hora_fin time without time zone NOT NULL,
    es_recreo boolean DEFAULT false NOT NULL,
    CONSTRAINT bloques_horarios_check CHECK ((hora_fin > hora_inicio))
);

CREATE SEQUENCE public.bloques_horarios_id_seq
    AS smallint
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.bloques_horarios_id_seq OWNED BY public.bloques_horarios.id;

CREATE TABLE public.calificaciones_cneb (
    id bigint NOT NULL,
    matricula_id bigint NOT NULL,
    anio_lectivo_id smallint NOT NULL,
    seccion_id integer NOT NULL,
    periodo_academico_id smallint NOT NULL,
    asignacion_docente_id bigint NOT NULL,
    area_curricular_id smallint NOT NULL,
    docente_usuario_id bigint NOT NULL,
    competencia_id smallint NOT NULL,
    calificacion_cualitativa character varying(2) NOT NULL,
    conclusion_descriptiva text,
    sugerencia_ia_utilizada boolean DEFAULT false NOT NULL,
    requiere_refuerzo boolean GENERATED ALWAYS AS (((calificacion_cualitativa)::text = 'C'::text)) STORED,
    fecha_creacion timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    fecha_actualizacion timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT calificaciones_cneb_calificacion_cualitativa_check CHECK ((calificacion_cualitativa IN ('AD', 'A', 'B', 'C'))),
    CONSTRAINT chk_calificaciones_conclusion_longitud CHECK ((char_length(conclusion_descriptiva) <= 4000))
);

CREATE SEQUENCE public.calificaciones_cneb_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.calificaciones_cneb_id_seq OWNED BY public.calificaciones_cneb.id;

CREATE TABLE public.competencias (
    id smallint NOT NULL,
    area_id smallint NOT NULL,
    numero_orden smallint NOT NULL,
    nombre_competencia character varying(50) NOT NULL,
    descripcion text,
    CONSTRAINT chk_competencias_descripcion_longitud CHECK ((char_length(descripcion) <= 4000)),
    CONSTRAINT competencias_numero_orden_check CHECK ((numero_orden > 0))
);

CREATE SEQUENCE public.competencias_id_seq
    AS smallint
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.competencias_id_seq OWNED BY public.competencias.id;

CREATE TABLE public.comprobantes_pago (
    id bigint NOT NULL,
    pago_transaccion_id bigint NOT NULL,
    tipo_comprobante character varying(20) DEFAULT 'RECIBO_INTERNO'::character varying NOT NULL,
    serie character varying(4) NOT NULL,
    correlativo integer NOT NULL,
    fecha_emision timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    monto_total numeric(10,2) NOT NULL,
    estado_comprobante character varying(20) DEFAULT 'EMITIDO'::character varying NOT NULL,
    fecha_anulacion timestamp with time zone,
    motivo_anulacion character varying(255),
    url_pdf_comprobante character varying(255),
    CONSTRAINT comprobantes_pago_check CHECK (((((estado_comprobante)::text = 'EMITIDO'::text) AND (fecha_anulacion IS NULL) AND (motivo_anulacion IS NULL)) OR (((estado_comprobante)::text = 'ANULADO'::text) AND (fecha_anulacion IS NOT NULL) AND (motivo_anulacion IS NOT NULL)))),
    CONSTRAINT comprobantes_pago_correlativo_check CHECK ((correlativo > 0)),
    CONSTRAINT comprobantes_pago_estado_comprobante_check CHECK ((estado_comprobante IN ('EMITIDO', 'ANULADO'))),
    CONSTRAINT comprobantes_pago_monto_total_check CHECK ((monto_total > 0.00)),
    CONSTRAINT comprobantes_pago_serie_check CHECK (((serie)::text ~ '^[BE][0-9]{3}$'::text)),
    CONSTRAINT comprobantes_pago_tipo_comprobante_check CHECK ((tipo_comprobante IN ('BOLETA', 'RECIBO_INTERNO')))
);

CREATE SEQUENCE public.comprobantes_pago_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.comprobantes_pago_id_seq OWNED BY public.comprobantes_pago.id;

CREATE TABLE public.comunicado_destinatarios (
    id bigint NOT NULL,
    comunicado_id bigint NOT NULL,
    apoderado_id bigint NOT NULL,
    leido boolean DEFAULT false NOT NULL,
    fecha_lectura timestamp with time zone,
    acuse_confirmado boolean DEFAULT false NOT NULL,
    fecha_acuse timestamp with time zone
);

CREATE SEQUENCE public.comunicado_destinatarios_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.comunicado_destinatarios_id_seq OWNED BY public.comunicado_destinatarios.id;

CREATE TABLE public.comunicados_oficiales (
    id bigint NOT NULL,
    titulo_comunicado character varying(40) NOT NULL,
    contenido text NOT NULL,
    remitente_usuario_id bigint NOT NULL,
    fecha_publicacion timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    requiere_acuse boolean DEFAULT false NOT NULL,
    CONSTRAINT chk_comunicados_contenido_longitud CHECK ((char_length(contenido) <= 20000))
);

CREATE SEQUENCE public.comunicados_oficiales_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.comunicados_oficiales_id_seq OWNED BY public.comunicados_oficiales.id;

CREATE TABLE public.conceptos_cobro (
    id smallint NOT NULL,
    codigo_concepto character varying(30) NOT NULL,
    nombre_concepto character varying(50) NOT NULL,
    tipo_concepto character varying(20) NOT NULL,
    monto_sugerido numeric(10,2) NOT NULL,
    CONSTRAINT conceptos_cobro_monto_sugerido_check CHECK ((monto_sugerido >= 0.00)),
    CONSTRAINT conceptos_cobro_tipo_concepto_check CHECK ((tipo_concepto IN ('MATRICULA', 'PENSION', 'CERTIFICADO', 'OTRO')))
);

CREATE SEQUENCE public.conceptos_cobro_id_seq
    AS smallint
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.conceptos_cobro_id_seq OWNED BY public.conceptos_cobro.id;

CREATE TABLE public.conciliaciones_asistencia (
    id bigint NOT NULL,
    fecha date NOT NULL,
    estudiante_id bigint NOT NULL,
    marco_porteria boolean NOT NULL,
    presente_aula boolean NOT NULL,
    tipo_discrepancia character varying(35) NOT NULL,
    alerta_notificada boolean DEFAULT false NOT NULL,
    CONSTRAINT conciliaciones_asistencia_tipo_discrepancia_check CHECK ((tipo_discrepancia IN ('ASISTENCIA_CONCILIADA', 'DISCREPANCIA_FUGA', 'DISCREPANCIA_OMISION_PORTERIA', 'PENDIENTE_CIERRE_TURNO')))
);

CREATE SEQUENCE public.conciliaciones_asistencia_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.conciliaciones_asistencia_id_seq OWNED BY public.conciliaciones_asistencia.id;

CREATE TABLE public.estudiante_apoderados (
    id bigint NOT NULL,
    estudiante_id bigint NOT NULL,
    apoderado_id bigint NOT NULL,
    parentesco character varying(30) NOT NULL,
    es_responsable_economico boolean DEFAULT false NOT NULL,
    tiene_custodia boolean DEFAULT true NOT NULL,
    permite_recojo boolean DEFAULT true NOT NULL,
    CONSTRAINT estudiante_apoderados_parentesco_check CHECK ((parentesco IN ('PADRE', 'MADRE', 'TUTOR_LEGAL', 'ABUELO_A', 'OTRO')))
);

CREATE SEQUENCE public.estudiante_apoderados_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.estudiante_apoderados_id_seq OWNED BY public.estudiante_apoderados.id;

CREATE TABLE public.estudiantes (
    id bigint NOT NULL,
    tipo_documento character varying(3) DEFAULT 'DNI'::character varying NOT NULL,
    numero_documento character varying(8) NOT NULL,
    nombres_estudiante character varying(20) NOT NULL,
    apellido_paterno_estudiante character varying(20) NOT NULL,
    apellido_materno_estudiante character varying(20) NOT NULL,
    fecha_nacimiento date NOT NULL,
    genero character(1) NOT NULL,
    codigo_estudiante_siagie character varying(14),
    validado_reniec boolean DEFAULT false NOT NULL,
    origen_registro character varying(20) DEFAULT 'RENIEC_API'::character varying NOT NULL,
    grupo_sanguineo character varying(5),
    alergias_condiciones text,
    activo boolean DEFAULT true NOT NULL,
    fecha_creacion timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_estudiantes_dni_8_digitos CHECK (((numero_documento)::text ~ '^[0-9]{8}$'::text)),
    CONSTRAINT chk_estudiantes_salud_longitud CHECK ((char_length(alergias_condiciones) <= 4000)),
    CONSTRAINT estudiantes_genero_check CHECK ((genero = ANY (ARRAY['M'::bpchar, 'F'::bpchar]))),
    CONSTRAINT estudiantes_origen_registro_check CHECK ((origen_registro IN ('RENIEC_API', 'MANUAL_CONTINGENCIA'))),
    CONSTRAINT estudiantes_tipo_documento_check CHECK ((tipo_documento = 'DNI'))
);

CREATE SEQUENCE public.estudiantes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.estudiantes_id_seq OWNED BY public.estudiantes.id;

CREATE TABLE public.grados (
    id smallint NOT NULL,
    nivel_id smallint NOT NULL,
    numero_grado smallint NOT NULL,
    nombre_grado character varying(30) NOT NULL,
    CONSTRAINT grados_numero_grado_check CHECK (((numero_grado >= 1) AND (numero_grado <= 6)))
);

CREATE SEQUENCE public.grados_id_seq
    AS smallint
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.grados_id_seq OWNED BY public.grados.id;

CREATE TABLE public.horarios_seccion (
    id bigint NOT NULL,
    anio_lectivo_id smallint NOT NULL,
    seccion_id integer NOT NULL,
    docente_usuario_id bigint NOT NULL,
    asignacion_docente_id bigint NOT NULL,
    dia_semana smallint NOT NULL,
    bloque_horario_id smallint NOT NULL,
    CONSTRAINT horarios_seccion_dia_semana_check CHECK (((dia_semana >= 1) AND (dia_semana <= 5)))
);

CREATE SEQUENCE public.horarios_seccion_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.horarios_seccion_id_seq OWNED BY public.horarios_seccion.id;

CREATE TABLE public.incidencias_conductuales (
    id bigint NOT NULL,
    matricula_id bigint NOT NULL,
    fecha_incidencia date NOT NULL,
    tipo_falta character varying(20) NOT NULL,
    descripcion text NOT NULL,
    reportado_por_usuario_id bigint NOT NULL,
    requiere_citacion boolean DEFAULT false NOT NULL,
    estado character varying(20) DEFAULT 'ABIERTA'::character varying NOT NULL,
    fecha_creacion timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_incidencias_descripcion_longitud CHECK ((char_length(descripcion) <= 10000)),
    CONSTRAINT incidencias_conductuales_estado_check CHECK ((estado IN ('ABIERTA', 'ATENDIDA', 'CERRADA'))),
    CONSTRAINT incidencias_conductuales_tipo_falta_check CHECK ((tipo_falta IN ('LEVE', 'GRAVE', 'MUY_GRAVE')))
);

CREATE SEQUENCE public.incidencias_conductuales_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.incidencias_conductuales_id_seq OWNED BY public.incidencias_conductuales.id;

CREATE TABLE public.inscripciones_refuerzo (
    id bigint NOT NULL,
    sesion_refuerzo_id bigint NOT NULL,
    estudiante_id bigint NOT NULL,
    calificacion_origen_id bigint,
    estado_asistencia character varying(20) DEFAULT 'PENDIENTE'::character varying NOT NULL,
    observaciones text,
    CONSTRAINT chk_refuerzo_observaciones_longitud CHECK ((char_length(observaciones) <= 500)),
    CONSTRAINT inscripciones_refuerzo_estado_asistencia_check CHECK ((estado_asistencia IN ('PENDIENTE', 'ASISTIO', 'FALTO', 'JUSTIFICADO')))
);

CREATE SEQUENCE public.inscripciones_refuerzo_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.inscripciones_refuerzo_id_seq OWNED BY public.inscripciones_refuerzo.id;

CREATE TABLE public.lotes_biometrico (
    id bigint NOT NULL,
    nombre_archivo character varying(150) NOT NULL,
    total_filas integer NOT NULL,
    marcas_validas integer NOT NULL,
    marcas_erroneas integer NOT NULL,
    importado_por_usuario_id bigint,
    fecha_creacion timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    huella_contenido character varying(64) NOT NULL,
    CONSTRAINT lotes_biometrico_marcas_erroneas_check CHECK ((marcas_erroneas >= 0)),
    CONSTRAINT lotes_biometrico_marcas_validas_check CHECK ((marcas_validas >= 0)),
    CONSTRAINT lotes_biometrico_total_filas_check CHECK ((total_filas >= 0))
);

CREATE SEQUENCE public.lotes_biometrico_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.lotes_biometrico_id_seq OWNED BY public.lotes_biometrico.id;

CREATE TABLE public.marcas_biometrico_porteria (
    id bigint NOT NULL,
    lote_id bigint,
    dni_leido character varying(8) NOT NULL,
    fecha_hora timestamp with time zone NOT NULL,
    dispositivo_codigo character varying(30) DEFAULT 'PORTERIA_01'::character varying NOT NULL,
    estudiante_id bigint,
    estado_procesamiento character varying(25) DEFAULT 'PENDIENTE'::character varying NOT NULL,
    fecha_creacion timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_marcas_porteria_dni_8_digitos CHECK (((dni_leido)::text ~ '^[0-9]{8}$'::text)),
    CONSTRAINT marcas_biometrico_porteria_estado_procesamiento_check CHECK ((estado_procesamiento IN ('PENDIENTE', 'CONCILIADO', 'DNI_NO_IDENTIFICADO', 'DUPLICADO')))
);

CREATE SEQUENCE public.marcas_biometrico_porteria_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.marcas_biometrico_porteria_id_seq OWNED BY public.marcas_biometrico_porteria.id;

CREATE TABLE public.matriculas (
    id bigint NOT NULL,
    anio_lectivo_id smallint NOT NULL,
    estudiante_id bigint NOT NULL,
    seccion_id integer NOT NULL,
    fecha_matricula timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    estado_matricula character varying(25) DEFAULT 'SOLICITADA'::character varying NOT NULL,
    fecha_expiracion_reserva timestamp with time zone,
    observaciones text,
    CONSTRAINT chk_matriculas_observaciones_longitud CHECK ((char_length(observaciones) <= 500)),
    CONSTRAINT matriculas_check CHECK ((((estado_matricula)::text <> 'RESERVADA_TEMPORAL'::text) OR (fecha_expiracion_reserva IS NOT NULL))),
    CONSTRAINT matriculas_estado_matricula_check CHECK ((estado_matricula IN ('SOLICITADA', 'RESERVADA_TEMPORAL', 'MATRICULADO', 'TRASLADADO', 'RETIRADO', 'CANCELADA')))
);

CREATE SEQUENCE public.matriculas_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.matriculas_id_seq OWNED BY public.matriculas.id;

CREATE TABLE public.niveles (
    id smallint NOT NULL,
    codigo_nivel character varying(20) NOT NULL,
    nombre_nivel character varying(30) NOT NULL,
    CONSTRAINT niveles_codigo_check CHECK ((codigo_nivel IN ('INICIAL', 'PRIMARIA', 'SECUNDARIA')))
);

CREATE SEQUENCE public.niveles_id_seq
    AS smallint
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.niveles_id_seq OWNED BY public.niveles.id;

CREATE TABLE public.obligaciones_pago (
    id bigint NOT NULL,
    matricula_id bigint NOT NULL,
    concepto_id smallint NOT NULL,
    tipo_concepto character varying(20) NOT NULL,
    numero_cuota smallint NOT NULL,
    descripcion character varying(150) NOT NULL,
    fecha_vencimiento date NOT NULL,
    monto_base numeric(10,2) NOT NULL,
    monto_mora numeric(10,2) DEFAULT 0.00 NOT NULL,
    monto_descuento numeric(10,2) DEFAULT 0.00 NOT NULL,
    total_pagado numeric(10,2) DEFAULT 0.00 NOT NULL,
    saldo_pendiente numeric(10,2) GENERATED ALWAYS AS ((((monto_base + monto_mora) - monto_descuento) - total_pagado)) STORED,
    estado character varying(25) DEFAULT 'PENDIENTE'::character varying NOT NULL,
    CONSTRAINT obligaciones_pago_check CHECK ((total_pagado <= ((monto_base + monto_mora) - monto_descuento))),
    CONSTRAINT obligaciones_pago_check1 CHECK (((((tipo_concepto)::text = 'MATRICULA'::text) AND (numero_cuota = 0)) OR (((tipo_concepto)::text = 'PENSION'::text) AND ((numero_cuota >= 1) AND (numero_cuota <= 10))) OR (tipo_concepto NOT IN ('MATRICULA', 'PENSION')))),
    CONSTRAINT obligaciones_pago_estado_check CHECK ((estado IN ('PENDIENTE', 'PAGADO_TOTAL', 'PAGADO_PARCIAL', 'VENCIDO', 'CANCELADO_POR_TRASLADO'))),
    CONSTRAINT obligaciones_pago_monto_base_check CHECK ((monto_base >= 0.00)),
    CONSTRAINT obligaciones_pago_monto_descuento_check CHECK ((monto_descuento >= 0.00)),
    CONSTRAINT obligaciones_pago_monto_mora_check CHECK ((monto_mora >= 0.00)),
    CONSTRAINT obligaciones_pago_numero_cuota_check CHECK (((numero_cuota >= 0) AND (numero_cuota <= 10))),
    CONSTRAINT obligaciones_pago_total_pagado_check CHECK ((total_pagado >= 0.00))
);

CREATE SEQUENCE public.obligaciones_pago_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.obligaciones_pago_id_seq OWNED BY public.obligaciones_pago.id;

CREATE TABLE public.pagos_transacciones (
    id bigint NOT NULL,
    obligacion_pago_id bigint NOT NULL,
    pasarela_proveedor character varying(30) NOT NULL,
    pasarela_transaccion_id character varying(100) NOT NULL,
    metodo_pago character varying(30) NOT NULL,
    monto_pagado numeric(10,2) NOT NULL,
    fecha_pago timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    estado_pago character varying(20) DEFAULT 'APROBADO'::character varying NOT NULL,
    payload_webhook jsonb,
    CONSTRAINT pagos_transacciones_estado_pago_check CHECK ((estado_pago IN ('APROBADO', 'RECHAZADO', 'REVERTIDO'))),
    CONSTRAINT pagos_transacciones_metodo_pago_check CHECK ((metodo_pago IN ('TARJETA_CREDITO', 'TARJETA_DEBITO', 'YAPE', 'PLIN', 'TRANSFERENCIA', 'EFECTIVO', 'PAGO_EFECTIVO'))),
    CONSTRAINT pagos_transacciones_monto_pagado_check CHECK ((monto_pagado > 0.00)),
    CONSTRAINT pagos_transacciones_pasarela_proveedor_check CHECK ((pasarela_proveedor IN ('CULQI', 'NIUBIZ', 'MERCADO_PAGO', 'CAJA_EFECTIVO', 'TRANSFERENCIA')))
);

CREATE SEQUENCE public.pagos_transacciones_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.pagos_transacciones_id_seq OWNED BY public.pagos_transacciones.id;

CREATE TABLE public.periodos_academicos (
    id smallint NOT NULL,
    anio_lectivo_id smallint NOT NULL,
    numero_periodo smallint NOT NULL,
    nombre_periodo character varying(30) NOT NULL,
    fecha_inicio date NOT NULL,
    fecha_fin date NOT NULL,
    cerrado boolean DEFAULT false NOT NULL,
    activo boolean DEFAULT true NOT NULL,
    CONSTRAINT periodos_academicos_check CHECK ((fecha_fin > fecha_inicio)),
    CONSTRAINT periodos_academicos_numero_periodo_check CHECK (((numero_periodo >= 1) AND (numero_periodo <= 4)))
);

CREATE SEQUENCE public.periodos_academicos_id_seq
    AS smallint
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.periodos_academicos_id_seq OWNED BY public.periodos_academicos.id;

CREATE TABLE public.roles (
    id smallint NOT NULL,
    codigo_rol character varying(20) NOT NULL,
    nombre_rol character varying(30) NOT NULL
);

CREATE SEQUENCE public.roles_id_seq
    AS smallint
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.roles_id_seq OWNED BY public.roles.id;

CREATE TABLE public.secciones (
    id integer NOT NULL,
    anio_lectivo_id smallint NOT NULL,
    grado_id smallint NOT NULL,
    nivel_id smallint NOT NULL,
    letra character(1) NOT NULL,
    cupo_maximo smallint NOT NULL,
    vacantes_ocupadas smallint DEFAULT 0 NOT NULL,
    aula_id integer NOT NULL,
    CONSTRAINT secciones_check CHECK (((vacantes_ocupadas >= 0) AND (vacantes_ocupadas <= cupo_maximo))),
    CONSTRAINT secciones_cupo_maximo_check CHECK ((cupo_maximo > 0)),
    CONSTRAINT secciones_letra_check CHECK ((letra ~ '^[A-Z]$'::text))
);

CREATE SEQUENCE public.secciones_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.secciones_id_seq OWNED BY public.secciones.id;

CREATE TABLE public.series_comprobante (
    serie character varying(4) NOT NULL,
    ultimo_correlativo integer DEFAULT 0 NOT NULL,
    fecha_actualizacion timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT series_comprobante_serie_check CHECK (((serie)::text ~ '^[BE][0-9]{3}$'::text)),
    CONSTRAINT series_comprobante_ultimo_correlativo_check CHECK ((ultimo_correlativo >= 0))
);

CREATE TABLE public.sesiones (
    id bigint NOT NULL,
    usuario_id bigint NOT NULL,
    huella_token character varying(64) NOT NULL,
    direccion_ip inet,
    agente_usuario text,
    fecha_expiracion timestamp with time zone NOT NULL,
    fecha_creacion timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE SEQUENCE public.sesiones_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.sesiones_id_seq OWNED BY public.sesiones.id;

CREATE TABLE public.sesiones_refuerzo (
    id bigint NOT NULL,
    anio_lectivo_id smallint NOT NULL,
    periodo_academico_id smallint NOT NULL,
    area_curricular_id smallint NOT NULL,
    docente_usuario_id bigint NOT NULL,
    tema_refuerzo character varying(40) NOT NULL,
    fecha_programada date NOT NULL,
    hora_inicio time without time zone NOT NULL,
    hora_fin time without time zone NOT NULL,
    codigo_aula character varying(30),
    CONSTRAINT sesiones_refuerzo_check CHECK ((hora_fin > hora_inicio))
);

CREATE SEQUENCE public.sesiones_refuerzo_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.sesiones_refuerzo_id_seq OWNED BY public.sesiones_refuerzo.id;

CREATE TABLE public.solicitudes_matricula_documentos (
    solicitud_id uuid NOT NULL,
    tipo character varying(30) NOT NULL,
    contenedor character varying(100) NOT NULL,
    ruta_objeto character varying(500) NOT NULL,
    tipo_contenido character varying(30) NOT NULL,
    tamano_bytes bigint NOT NULL,
    huella_sha256 character(64) NOT NULL,
    fecha_actualizacion timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    version_id uuid NOT NULL,
    CONSTRAINT solicitudes_matricula_documentos_mime_type_check CHECK (((tipo_contenido)::text = ANY (ARRAY[('application/pdf'::character varying)::text, ('image/jpeg'::character varying)::text, ('image/png'::character varying)::text, ('image/webp'::character varying)::text]))),
    CONSTRAINT solicitudes_matricula_documentos_tamano_bytes_check CHECK (((tamano_bytes >= 1) AND (tamano_bytes <= 5242880))),
    CONSTRAINT solicitudes_matricula_documentos_tipo_check CHECK ((tipo IN ('PARTIDA_NACIMIENTO', 'DNI_C4', 'RECIBO_SERVICIO')))
);

CREATE TABLE public.solicitudes_matricula_publica (
    id uuid NOT NULL,
    huella_token character varying(64) NOT NULL,
    estado character varying(30) NOT NULL,
    anio_lectivo_id smallint NOT NULL,
    seccion_id integer NOT NULL,
    numero_documento_estudiante character varying(8) NOT NULL,
    nombres_estudiante character varying(20) NOT NULL,
    apellido_paterno_estudiante character varying(20) NOT NULL,
    apellido_materno_estudiante character varying(20) NOT NULL,
    fecha_nacimiento_estudiante date NOT NULL,
    genero_estudiante character(1) NOT NULL,
    numero_documento_apoderado character varying(8) NOT NULL,
    nombres_apoderado character varying(20) NOT NULL,
    apellido_paterno_apoderado character varying(20) NOT NULL,
    apellido_materno_apoderado character varying(20) NOT NULL,
    celular_apoderado character varying(9) NOT NULL,
    correo_apoderado character varying(50),
    direccion_apoderado character varying(150) NOT NULL,
    ubigeo_apoderado character varying(6) NOT NULL,
    parentesco character varying(30) NOT NULL,
    consentimiento_gemini boolean NOT NULL,
    estado_partida character varying(15) DEFAULT 'PENDIENTE'::character varying NOT NULL,
    observacion_partida character varying(500),
    estado_dni_c4 character varying(15) DEFAULT 'PENDIENTE'::character varying NOT NULL,
    observacion_dni_c4 character varying(500),
    estado_recibo character varying(15) DEFAULT 'PENDIENTE'::character varying NOT NULL,
    observacion_recibo character varying(500),
    pago_preferencia_id character varying(100),
    pago_enlace character varying(1000),
    pago_id character varying(100),
    pago_monto numeric(10,2) DEFAULT 350.00 NOT NULL,
    fecha_expiracion_pago timestamp with time zone,
    vacante_reservada boolean DEFAULT false NOT NULL,
    estudiante_id bigint,
    apoderado_id bigint,
    matricula_id bigint,
    fecha_creacion timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    fecha_actualizacion timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    comprobante_pago_codigo character varying(40),
    fecha_emision_documentos timestamp with time zone,
    CONSTRAINT chk_solicitud_matricula_comprobante_confirmada CHECK ((((estado)::text <> 'MATRICULADA'::text) OR (comprobante_pago_codigo IS NOT NULL))),
    CONSTRAINT chk_solicitud_matricula_documentos_emitidos CHECK (((comprobante_pago_codigo IS NULL) = (fecha_emision_documentos IS NULL))),
    CONSTRAINT chk_solicitudes_matricula_pago_monto_positivo CHECK ((pago_monto > (0)::numeric)),
    CONSTRAINT solicitudes_matricula_publica_celular_apoderado_check CHECK (((celular_apoderado)::text ~ '^9[0-9]{8}$'::text)),
    CONSTRAINT solicitudes_matricula_publica_check CHECK (((NOT vacante_reservada) OR (fecha_expiracion_pago IS NOT NULL))),
    CONSTRAINT solicitudes_matricula_publica_check1 CHECK ((((estado)::text <> 'MATRICULADA'::text) OR ((estudiante_id IS NOT NULL) AND (apoderado_id IS NOT NULL) AND (matricula_id IS NOT NULL) AND (pago_id IS NOT NULL)))),
    CONSTRAINT solicitudes_matricula_publica_consentimiento_gemini_check CHECK ((consentimiento_gemini IS TRUE)),
    CONSTRAINT solicitudes_matricula_publica_estado_check CHECK ((estado IN ('DOCUMENTOS_PENDIENTES', 'DOCUMENTOS_EN_VALIDACION', 'DOCUMENTOS_OBSERVADOS', 'DOCUMENTOS_VALIDADOS', 'PAGO_PENDIENTE', 'MATRICULADA', 'RECHAZADA'))),
    CONSTRAINT solicitudes_matricula_publica_estado_dni_c4_check CHECK ((estado_dni_c4 IN ('PENDIENTE', 'EN_VALIDACION', 'VALIDADO', 'OBSERVADO'))),
    CONSTRAINT solicitudes_matricula_publica_estado_partida_check CHECK ((estado_partida IN ('PENDIENTE', 'EN_VALIDACION', 'VALIDADO', 'OBSERVADO'))),
    CONSTRAINT solicitudes_matricula_publica_estado_recibo_check CHECK ((estado_recibo IN ('PENDIENTE', 'EN_VALIDACION', 'VALIDADO', 'OBSERVADO'))),
    CONSTRAINT solicitudes_matricula_publica_genero_estudiante_check CHECK ((genero_estudiante = ANY (ARRAY['M'::bpchar, 'F'::bpchar]))),
    CONSTRAINT solicitudes_matricula_publica_numero_documento_apoderado_check CHECK (((numero_documento_apoderado)::text ~ '^[0-9]{8}$'::text)),
    CONSTRAINT solicitudes_matricula_publica_numero_documento_estudiante_check CHECK (((numero_documento_estudiante)::text ~ '^[0-9]{8}$'::text)),
    CONSTRAINT solicitudes_matricula_publica_parentesco_check CHECK ((parentesco IN ('PADRE', 'MADRE', 'TUTOR_LEGAL', 'ABUELO_A', 'OTRO'))),
    CONSTRAINT solicitudes_matricula_publica_ubigeo_apoderado_check CHECK (((ubigeo_apoderado)::text ~ '^[0-9]{6}$'::text))
);

CREATE TABLE public.usuario_roles (
    usuario_id bigint NOT NULL,
    rol_id smallint NOT NULL
);

CREATE TABLE public.usuarios (
    id bigint NOT NULL,
    nombre_usuario character varying(30) NOT NULL,
    correo_electronico character varying(50) NOT NULL,
    usuario_password character varying(60) NOT NULL,
    activo boolean DEFAULT true NOT NULL,
    fecha_creacion timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    fecha_actualizacion timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE SEQUENCE public.usuarios_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.usuarios_id_seq OWNED BY public.usuarios.id;

ALTER TABLE ONLY public.anios_lectivos ALTER COLUMN id SET DEFAULT nextval('public.anios_lectivos_id_seq'::regclass);

ALTER TABLE ONLY public.apoderados ALTER COLUMN id SET DEFAULT nextval('public.apoderados_id_seq'::regclass);

ALTER TABLE ONLY public.areas_curriculares ALTER COLUMN id SET DEFAULT nextval('public.areas_curriculares_id_seq'::regclass);

ALTER TABLE ONLY public.asignaciones_docentes ALTER COLUMN id SET DEFAULT nextval('public.asignaciones_docentes_id_seq'::regclass);

ALTER TABLE ONLY public.asistencias_aula ALTER COLUMN id SET DEFAULT nextval('public.asistencias_aula_id_seq'::regclass);

ALTER TABLE ONLY public.auditoria_cambios ALTER COLUMN id SET DEFAULT nextval('public.auditoria_cambios_id_seq'::regclass);

ALTER TABLE ONLY public.aulas ALTER COLUMN id SET DEFAULT nextval('public.aulas_id_seq'::regclass);

ALTER TABLE ONLY public.bloques_horarios ALTER COLUMN id SET DEFAULT nextval('public.bloques_horarios_id_seq'::regclass);

ALTER TABLE ONLY public.calificaciones_cneb ALTER COLUMN id SET DEFAULT nextval('public.calificaciones_cneb_id_seq'::regclass);

ALTER TABLE ONLY public.competencias ALTER COLUMN id SET DEFAULT nextval('public.competencias_id_seq'::regclass);

ALTER TABLE ONLY public.comprobantes_pago ALTER COLUMN id SET DEFAULT nextval('public.comprobantes_pago_id_seq'::regclass);

ALTER TABLE ONLY public.comunicado_destinatarios ALTER COLUMN id SET DEFAULT nextval('public.comunicado_destinatarios_id_seq'::regclass);

ALTER TABLE ONLY public.comunicados_oficiales ALTER COLUMN id SET DEFAULT nextval('public.comunicados_oficiales_id_seq'::regclass);

ALTER TABLE ONLY public.conceptos_cobro ALTER COLUMN id SET DEFAULT nextval('public.conceptos_cobro_id_seq'::regclass);

ALTER TABLE ONLY public.conciliaciones_asistencia ALTER COLUMN id SET DEFAULT nextval('public.conciliaciones_asistencia_id_seq'::regclass);

ALTER TABLE ONLY public.estudiante_apoderados ALTER COLUMN id SET DEFAULT nextval('public.estudiante_apoderados_id_seq'::regclass);

ALTER TABLE ONLY public.estudiantes ALTER COLUMN id SET DEFAULT nextval('public.estudiantes_id_seq'::regclass);

ALTER TABLE ONLY public.grados ALTER COLUMN id SET DEFAULT nextval('public.grados_id_seq'::regclass);

ALTER TABLE ONLY public.horarios_seccion ALTER COLUMN id SET DEFAULT nextval('public.horarios_seccion_id_seq'::regclass);

ALTER TABLE ONLY public.incidencias_conductuales ALTER COLUMN id SET DEFAULT nextval('public.incidencias_conductuales_id_seq'::regclass);

ALTER TABLE ONLY public.inscripciones_refuerzo ALTER COLUMN id SET DEFAULT nextval('public.inscripciones_refuerzo_id_seq'::regclass);

ALTER TABLE ONLY public.lotes_biometrico ALTER COLUMN id SET DEFAULT nextval('public.lotes_biometrico_id_seq'::regclass);

ALTER TABLE ONLY public.marcas_biometrico_porteria ALTER COLUMN id SET DEFAULT nextval('public.marcas_biometrico_porteria_id_seq'::regclass);

ALTER TABLE ONLY public.matriculas ALTER COLUMN id SET DEFAULT nextval('public.matriculas_id_seq'::regclass);

ALTER TABLE ONLY public.niveles ALTER COLUMN id SET DEFAULT nextval('public.niveles_id_seq'::regclass);

ALTER TABLE ONLY public.obligaciones_pago ALTER COLUMN id SET DEFAULT nextval('public.obligaciones_pago_id_seq'::regclass);

ALTER TABLE ONLY public.pagos_transacciones ALTER COLUMN id SET DEFAULT nextval('public.pagos_transacciones_id_seq'::regclass);

ALTER TABLE ONLY public.periodos_academicos ALTER COLUMN id SET DEFAULT nextval('public.periodos_academicos_id_seq'::regclass);

ALTER TABLE ONLY public.roles ALTER COLUMN id SET DEFAULT nextval('public.roles_id_seq'::regclass);

ALTER TABLE ONLY public.secciones ALTER COLUMN id SET DEFAULT nextval('public.secciones_id_seq'::regclass);

ALTER TABLE ONLY public.sesiones ALTER COLUMN id SET DEFAULT nextval('public.sesiones_id_seq'::regclass);

ALTER TABLE ONLY public.sesiones_refuerzo ALTER COLUMN id SET DEFAULT nextval('public.sesiones_refuerzo_id_seq'::regclass);

ALTER TABLE ONLY public.usuarios ALTER COLUMN id SET DEFAULT nextval('public.usuarios_id_seq'::regclass);

ALTER TABLE ONLY public.anios_lectivos
    ADD CONSTRAINT anios_lectivos_anio_key UNIQUE (anio);

ALTER TABLE ONLY public.anios_lectivos
    ADD CONSTRAINT anios_lectivos_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.apoderados
    ADD CONSTRAINT apoderados_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.apoderados
    ADD CONSTRAINT apoderados_tipo_documento_numero_documento_key UNIQUE (tipo_documento, numero_documento);

ALTER TABLE ONLY public.apoderados
    ADD CONSTRAINT apoderados_usuario_id_key UNIQUE (usuario_id);

ALTER TABLE ONLY public.areas_curriculares
    ADD CONSTRAINT areas_curriculares_id_nivel_id_key UNIQUE (id, nivel_id);

ALTER TABLE ONLY public.areas_curriculares
    ADD CONSTRAINT areas_curriculares_nivel_id_codigo_key UNIQUE (nivel_id, codigo_area);

ALTER TABLE ONLY public.areas_curriculares
    ADD CONSTRAINT areas_curriculares_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.asignaciones_docentes
    ADD CONSTRAINT asignaciones_docentes_id_anio_lectivo_id_seccion_id_docente_key UNIQUE (id, anio_lectivo_id, seccion_id, docente_usuario_id);

ALTER TABLE ONLY public.asignaciones_docentes
    ADD CONSTRAINT asignaciones_docentes_id_seccion_id_area_curricular_id_anio_key UNIQUE (id, seccion_id, area_curricular_id, anio_lectivo_id, docente_usuario_id);

ALTER TABLE ONLY public.asignaciones_docentes
    ADD CONSTRAINT asignaciones_docentes_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.asignaciones_docentes
    ADD CONSTRAINT asignaciones_docentes_seccion_id_area_curricular_id_anio_le_key UNIQUE (seccion_id, area_curricular_id, anio_lectivo_id);

ALTER TABLE ONLY public.asistencias_aula
    ADD CONSTRAINT asistencias_aula_matricula_id_fecha_sesion_key UNIQUE (matricula_id, fecha_sesion);

ALTER TABLE ONLY public.asistencias_aula
    ADD CONSTRAINT asistencias_aula_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.auditoria_cambios
    ADD CONSTRAINT auditoria_cambios_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.aulas
    ADD CONSTRAINT aulas_codigo_key UNIQUE (codigo_aula);

ALTER TABLE ONLY public.aulas
    ADD CONSTRAINT aulas_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.bloques_horarios
    ADD CONSTRAINT bloques_horarios_numero_bloque_key UNIQUE (numero_bloque);

ALTER TABLE ONLY public.bloques_horarios
    ADD CONSTRAINT bloques_horarios_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.calificaciones_cneb
    ADD CONSTRAINT calificaciones_cneb_matricula_id_competencia_id_periodo_aca_key UNIQUE (matricula_id, competencia_id, periodo_academico_id);

ALTER TABLE ONLY public.calificaciones_cneb
    ADD CONSTRAINT calificaciones_cneb_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.competencias
    ADD CONSTRAINT competencias_id_area_id_key UNIQUE (id, area_id);

ALTER TABLE ONLY public.competencias
    ADD CONSTRAINT competencias_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.comprobantes_pago
    ADD CONSTRAINT comprobantes_pago_pago_transaccion_id_key UNIQUE (pago_transaccion_id);

ALTER TABLE ONLY public.comprobantes_pago
    ADD CONSTRAINT comprobantes_pago_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.comprobantes_pago
    ADD CONSTRAINT comprobantes_pago_serie_correlativo_key UNIQUE (serie, correlativo);

ALTER TABLE ONLY public.comunicado_destinatarios
    ADD CONSTRAINT comunicado_destinatarios_comunicado_id_apoderado_id_key UNIQUE (comunicado_id, apoderado_id);

ALTER TABLE ONLY public.comunicado_destinatarios
    ADD CONSTRAINT comunicado_destinatarios_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.comunicados_oficiales
    ADD CONSTRAINT comunicados_oficiales_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.conceptos_cobro
    ADD CONSTRAINT conceptos_cobro_codigo_key UNIQUE (codigo_concepto);

ALTER TABLE ONLY public.conceptos_cobro
    ADD CONSTRAINT conceptos_cobro_id_tipo_concepto_key UNIQUE (id, tipo_concepto);

ALTER TABLE ONLY public.conceptos_cobro
    ADD CONSTRAINT conceptos_cobro_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.conciliaciones_asistencia
    ADD CONSTRAINT conciliaciones_asistencia_fecha_estudiante_id_key UNIQUE (fecha, estudiante_id);

ALTER TABLE ONLY public.conciliaciones_asistencia
    ADD CONSTRAINT conciliaciones_asistencia_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.estudiante_apoderados
    ADD CONSTRAINT estudiante_apoderados_estudiante_id_apoderado_id_key UNIQUE (estudiante_id, apoderado_id);

ALTER TABLE ONLY public.estudiante_apoderados
    ADD CONSTRAINT estudiante_apoderados_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.estudiantes
    ADD CONSTRAINT estudiantes_codigo_estudiante_siagie_key UNIQUE (codigo_estudiante_siagie);

ALTER TABLE ONLY public.estudiantes
    ADD CONSTRAINT estudiantes_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.estudiantes
    ADD CONSTRAINT estudiantes_tipo_documento_numero_documento_key UNIQUE (tipo_documento, numero_documento);

ALTER TABLE ONLY public.grados
    ADD CONSTRAINT grados_id_nivel_id_key UNIQUE (id, nivel_id);

ALTER TABLE ONLY public.grados
    ADD CONSTRAINT grados_nivel_id_numero_grado_key UNIQUE (nivel_id, numero_grado);

ALTER TABLE ONLY public.grados
    ADD CONSTRAINT grados_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.horarios_seccion
    ADD CONSTRAINT horarios_seccion_docente_usuario_id_dia_semana_bloque_horar_key UNIQUE (docente_usuario_id, dia_semana, bloque_horario_id, anio_lectivo_id);

ALTER TABLE ONLY public.horarios_seccion
    ADD CONSTRAINT horarios_seccion_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.horarios_seccion
    ADD CONSTRAINT horarios_seccion_seccion_id_dia_semana_bloque_horario_id_an_key UNIQUE (seccion_id, dia_semana, bloque_horario_id, anio_lectivo_id);

ALTER TABLE ONLY public.incidencias_conductuales
    ADD CONSTRAINT incidencias_conductuales_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.inscripciones_refuerzo
    ADD CONSTRAINT inscripciones_refuerzo_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.inscripciones_refuerzo
    ADD CONSTRAINT inscripciones_refuerzo_sesion_refuerzo_id_estudiante_id_key UNIQUE (sesion_refuerzo_id, estudiante_id);

ALTER TABLE ONLY public.lotes_biometrico
    ADD CONSTRAINT lotes_biometrico_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.marcas_biometrico_porteria
    ADD CONSTRAINT marcas_biometrico_porteria_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.matriculas
    ADD CONSTRAINT matriculas_anio_lectivo_id_estudiante_id_key UNIQUE (anio_lectivo_id, estudiante_id);

ALTER TABLE ONLY public.matriculas
    ADD CONSTRAINT matriculas_id_anio_lectivo_id_key UNIQUE (id, anio_lectivo_id);

ALTER TABLE ONLY public.matriculas
    ADD CONSTRAINT matriculas_id_anio_lectivo_id_seccion_id_key UNIQUE (id, anio_lectivo_id, seccion_id);

ALTER TABLE ONLY public.matriculas
    ADD CONSTRAINT matriculas_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.niveles
    ADD CONSTRAINT niveles_codigo_key UNIQUE (codigo_nivel);

ALTER TABLE ONLY public.niveles
    ADD CONSTRAINT niveles_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.obligaciones_pago
    ADD CONSTRAINT obligaciones_pago_matricula_id_concepto_id_numero_cuota_key UNIQUE (matricula_id, concepto_id, numero_cuota);

ALTER TABLE ONLY public.obligaciones_pago
    ADD CONSTRAINT obligaciones_pago_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.pagos_transacciones
    ADD CONSTRAINT pagos_transacciones_pasarela_transaccion_id_key UNIQUE (pasarela_transaccion_id);

ALTER TABLE ONLY public.pagos_transacciones
    ADD CONSTRAINT pagos_transacciones_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.periodos_academicos
    ADD CONSTRAINT periodos_academicos_anio_lectivo_id_numero_periodo_key UNIQUE (anio_lectivo_id, numero_periodo);

ALTER TABLE ONLY public.periodos_academicos
    ADD CONSTRAINT periodos_academicos_id_anio_lectivo_id_key UNIQUE (id, anio_lectivo_id);

ALTER TABLE ONLY public.periodos_academicos
    ADD CONSTRAINT periodos_academicos_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_codigo_key UNIQUE (codigo_rol);

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.secciones
    ADD CONSTRAINT secciones_anio_lectivo_id_grado_id_letra_key UNIQUE (anio_lectivo_id, grado_id, letra);

ALTER TABLE ONLY public.secciones
    ADD CONSTRAINT secciones_id_anio_lectivo_id_key UNIQUE (id, anio_lectivo_id);

ALTER TABLE ONLY public.secciones
    ADD CONSTRAINT secciones_id_anio_lectivo_id_nivel_id_key UNIQUE (id, anio_lectivo_id, nivel_id);

ALTER TABLE ONLY public.secciones
    ADD CONSTRAINT secciones_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.series_comprobante
    ADD CONSTRAINT series_comprobante_pkey PRIMARY KEY (serie);

ALTER TABLE ONLY public.sesiones
    ADD CONSTRAINT sesiones_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.sesiones_refuerzo
    ADD CONSTRAINT sesiones_refuerzo_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.solicitudes_matricula_documentos
    ADD CONSTRAINT solicitudes_matricula_documentos_bucket_object_key_key UNIQUE (contenedor, ruta_objeto);

ALTER TABLE ONLY public.solicitudes_matricula_documentos
    ADD CONSTRAINT solicitudes_matricula_documentos_pkey PRIMARY KEY (solicitud_id, tipo);

ALTER TABLE ONLY public.solicitudes_matricula_publica
    ADD CONSTRAINT solicitudes_matricula_publica_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.solicitudes_matricula_publica
    ADD CONSTRAINT solicitudes_matricula_publica_token_hash_key UNIQUE (huella_token);

ALTER TABLE ONLY public.lotes_biometrico
    ADD CONSTRAINT uq_lotes_biometrico_hash_contenido UNIQUE (huella_contenido);

ALTER TABLE ONLY public.usuario_roles
    ADD CONSTRAINT usuario_roles_pkey PRIMARY KEY (usuario_id, rol_id);

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT usuarios_email_key UNIQUE (correo_electronico);

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT usuarios_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT usuarios_username_key UNIQUE (nombre_usuario);

CREATE INDEX idx_asignaciones_area_nivel ON public.asignaciones_docentes USING btree (area_curricular_id, nivel_id);

CREATE INDEX idx_asignaciones_docente ON public.asignaciones_docentes USING btree (docente_usuario_id);

CREATE INDEX idx_asignaciones_seccion ON public.asignaciones_docentes USING btree (seccion_id);

CREATE INDEX idx_asistencias_aula_auxiliar_id ON public.asistencias_aula USING btree (auxiliar_usuario_id);

CREATE INDEX idx_auditoria_cambios_usuario_id ON public.auditoria_cambios USING btree (usuario_id);

CREATE INDEX idx_calificaciones_asignacion_periodo ON public.calificaciones_cneb USING btree (asignacion_docente_id, periodo_academico_id);

CREATE INDEX idx_calificaciones_competencia ON public.calificaciones_cneb USING btree (competencia_id);

CREATE INDEX idx_calificaciones_docente ON public.calificaciones_cneb USING btree (docente_usuario_id);

CREATE INDEX idx_calificaciones_periodo ON public.calificaciones_cneb USING btree (periodo_academico_id);

CREATE INDEX idx_calificaciones_refuerzo ON public.calificaciones_cneb USING btree (periodo_academico_id, requiere_refuerzo) WHERE (requiere_refuerzo = true);

CREATE INDEX idx_competencias_area ON public.competencias USING btree (area_id);

CREATE INDEX idx_comunicado_dest_apod ON public.comunicado_destinatarios USING btree (apoderado_id);

CREATE INDEX idx_comunicados_remitente ON public.comunicados_oficiales USING btree (remitente_usuario_id);

CREATE INDEX idx_conciliaciones_estudiante_id ON public.conciliaciones_asistencia USING btree (estudiante_id);

CREATE INDEX idx_estudiante_apoderados_apod ON public.estudiante_apoderados USING btree (apoderado_id);

CREATE INDEX idx_estudiantes_apellidos ON public.estudiantes USING btree (apellido_paterno_estudiante, apellido_materno_estudiante);

CREATE INDEX idx_horarios_anio_lectivo_id ON public.horarios_seccion USING btree (anio_lectivo_id);

CREATE INDEX idx_horarios_asignacion ON public.horarios_seccion USING btree (asignacion_docente_id);

CREATE INDEX idx_horarios_bloque ON public.horarios_seccion USING btree (bloque_horario_id);

CREATE INDEX idx_horarios_docente ON public.horarios_seccion USING btree (docente_usuario_id, dia_semana);

CREATE INDEX idx_incidencias_matricula ON public.incidencias_conductuales USING btree (matricula_id);

CREATE INDEX idx_incidencias_reportado ON public.incidencias_conductuales USING btree (reportado_por_usuario_id);

CREATE INDEX idx_inscripciones_calificacion_origen_id ON public.inscripciones_refuerzo USING btree (calificacion_origen_id);

CREATE INDEX idx_inscripciones_refuerzo_est ON public.inscripciones_refuerzo USING btree (estudiante_id);

CREATE INDEX idx_lotes_biometrico_importado_por ON public.lotes_biometrico USING btree (importado_por_usuario_id);

CREATE INDEX idx_marcas_lote ON public.marcas_biometrico_porteria USING btree (lote_id);

CREATE INDEX idx_marcas_porteria_dni_fecha ON public.marcas_biometrico_porteria USING btree (dni_leido, fecha_hora);

CREATE INDEX idx_marcas_porteria_dni_leido ON public.marcas_biometrico_porteria USING btree (dni_leido);

CREATE INDEX idx_marcas_porteria_estudiante_id ON public.marcas_biometrico_porteria USING btree (estudiante_id);

CREATE INDEX idx_marcas_porteria_fecha_hora ON public.marcas_biometrico_porteria USING btree (fecha_hora);

CREATE INDEX idx_matriculas_estudiante_id ON public.matriculas USING btree (estudiante_id);

CREATE INDEX idx_matriculas_seccion ON public.matriculas USING btree (seccion_id, estado_matricula);

CREATE INDEX idx_obligaciones_concepto ON public.obligaciones_pago USING btree (concepto_id);

CREATE INDEX idx_obligaciones_estado_venc ON public.obligaciones_pago USING btree (estado, fecha_vencimiento);

CREATE INDEX idx_obligaciones_matricula ON public.obligaciones_pago USING btree (matricula_id);

CREATE INDEX idx_pagos_obligacion_id ON public.pagos_transacciones USING btree (obligacion_pago_id);

CREATE INDEX idx_secciones_aula_id ON public.secciones USING btree (aula_id);

CREATE INDEX idx_secciones_grado_nivel ON public.secciones USING btree (grado_id, nivel_id);

CREATE INDEX idx_sesiones_refuerzo_area ON public.sesiones_refuerzo USING btree (area_curricular_id);

CREATE INDEX idx_sesiones_refuerzo_aula_asignada ON public.sesiones_refuerzo USING btree (codigo_aula);

CREATE INDEX idx_sesiones_refuerzo_docente_id ON public.sesiones_refuerzo USING btree (docente_usuario_id);

CREATE INDEX idx_sesiones_refuerzo_periodo ON public.sesiones_refuerzo USING btree (periodo_academico_id);

CREATE INDEX idx_sesiones_usuario ON public.sesiones USING btree (usuario_id);

CREATE INDEX idx_solicitudes_matricula_anio ON public.solicitudes_matricula_publica USING btree (anio_lectivo_id);

CREATE INDEX idx_solicitudes_matricula_apoderado ON public.solicitudes_matricula_publica USING btree (apoderado_id) WHERE (apoderado_id IS NOT NULL);

CREATE INDEX idx_solicitudes_matricula_estado_fecha ON public.solicitudes_matricula_publica USING btree (estado, fecha_creacion DESC);

CREATE INDEX idx_solicitudes_matricula_estudiante ON public.solicitudes_matricula_publica USING btree (estudiante_id) WHERE (estudiante_id IS NOT NULL);

CREATE INDEX idx_solicitudes_matricula_matricula ON public.solicitudes_matricula_publica USING btree (matricula_id) WHERE (matricula_id IS NOT NULL);

CREATE INDEX idx_solicitudes_matricula_reservas ON public.solicitudes_matricula_publica USING btree (fecha_expiracion_pago) WHERE (vacante_reservada IS TRUE);

CREATE INDEX idx_solicitudes_matricula_seccion_anio ON public.solicitudes_matricula_publica USING btree (seccion_id, anio_lectivo_id);

CREATE INDEX idx_usuario_roles_rol_id ON public.usuario_roles USING btree (rol_id);

CREATE UNIQUE INDEX uq_aulas_codigo_normalizado ON public.aulas USING btree (upper((codigo_aula)::text));

CREATE UNIQUE INDEX uq_marcas_porteria_evento_valido ON public.marcas_biometrico_porteria USING btree (dni_leido, fecha_hora, dispositivo_codigo) WHERE ((estado_procesamiento)::text <> 'DUPLICADO'::text);

CREATE UNIQUE INDEX uq_secciones_anio_aula_id ON public.secciones USING btree (anio_lectivo_id, aula_id);

CREATE UNIQUE INDEX uq_solicitud_matricula_comprobante ON public.solicitudes_matricula_publica USING btree (comprobante_pago_codigo) WHERE (comprobante_pago_codigo IS NOT NULL);

CREATE UNIQUE INDEX uq_solicitud_matricula_dni_activa ON public.solicitudes_matricula_publica USING btree (numero_documento_estudiante) WHERE (estado NOT IN ('MATRICULADA', 'RECHAZADA'));

CREATE TRIGGER trg_anular_comprobante_pago AFTER UPDATE OF estado_pago ON public.pagos_transacciones FOR EACH ROW EXECUTE FUNCTION public.fn_anular_comprobante_pago();

CREATE TRIGGER trg_aplicar_pago_obligacion AFTER INSERT OR UPDATE OF estado_pago, monto_pagado ON public.pagos_transacciones FOR EACH ROW EXECUTE FUNCTION public.fn_aplicar_pago_obligacion();

CREATE TRIGGER trg_auditar_calificaciones AFTER INSERT OR DELETE OR UPDATE ON public.calificaciones_cneb FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios();

CREATE TRIGGER trg_auditar_comprobantes AFTER INSERT OR DELETE OR UPDATE ON public.comprobantes_pago FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios();

CREATE TRIGGER trg_auditar_matriculas AFTER INSERT OR DELETE OR UPDATE ON public.matriculas FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios();

CREATE TRIGGER trg_auditar_obligaciones AFTER INSERT OR DELETE OR UPDATE ON public.obligaciones_pago FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios();

CREATE TRIGGER trg_auditar_pagos AFTER INSERT OR DELETE OR UPDATE ON public.pagos_transacciones FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios();

CREATE TRIGGER trg_calificaciones_fecha_actualizacion BEFORE UPDATE ON public.calificaciones_cneb FOR EACH ROW EXECUTE FUNCTION public.fn_actualizar_fecha();

CREATE TRIGGER trg_gestionar_vacante_matricula AFTER INSERT OR UPDATE OF estado_matricula, seccion_id ON public.matriculas FOR EACH ROW EXECUTE FUNCTION public.fn_gestionar_vacante_matricula();

CREATE TRIGGER trg_prevenir_cambio_obligacion_pago BEFORE UPDATE OF obligacion_pago_id ON public.pagos_transacciones FOR EACH ROW EXECUTE FUNCTION public.fn_prevenir_cambio_obligacion_pago();

CREATE TRIGGER trg_prevenir_cambio_pago_comprobante BEFORE UPDATE OF pago_transaccion_id ON public.comprobantes_pago FOR EACH ROW EXECUTE FUNCTION public.fn_prevenir_cambio_pago_comprobante();

CREATE TRIGGER trg_usuarios_fecha_actualizacion BEFORE UPDATE ON public.usuarios FOR EACH ROW EXECUTE FUNCTION public.fn_actualizar_fecha();

CREATE TRIGGER trg_validar_comprobante_pago BEFORE INSERT ON public.comprobantes_pago FOR EACH ROW EXECUTE FUNCTION public.fn_validar_comprobante_pago();

CREATE TRIGGER trg_validar_estado_comprobante_pago BEFORE UPDATE OF estado_comprobante, fecha_anulacion, motivo_anulacion ON public.comprobantes_pago FOR EACH ROW EXECUTE FUNCTION public.fn_validar_estado_comprobante_pago();

CREATE TRIGGER trg_validar_transicion_pago BEFORE UPDATE OF estado_pago, monto_pagado ON public.pagos_transacciones FOR EACH ROW EXECUTE FUNCTION public.fn_validar_transicion_pago();

ALTER TABLE ONLY public.apoderados
    ADD CONSTRAINT apoderados_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id) ON DELETE SET NULL;

ALTER TABLE ONLY public.areas_curriculares
    ADD CONSTRAINT areas_curriculares_nivel_id_fkey FOREIGN KEY (nivel_id) REFERENCES public.niveles(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.asignaciones_docentes
    ADD CONSTRAINT asignaciones_docentes_area_curricular_id_nivel_id_fkey FOREIGN KEY (area_curricular_id, nivel_id) REFERENCES public.areas_curriculares(id, nivel_id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.asignaciones_docentes
    ADD CONSTRAINT asignaciones_docentes_docente_usuario_id_fkey FOREIGN KEY (docente_usuario_id) REFERENCES public.usuarios(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.asignaciones_docentes
    ADD CONSTRAINT asignaciones_docentes_seccion_id_anio_lectivo_id_nivel_id_fkey FOREIGN KEY (seccion_id, anio_lectivo_id, nivel_id) REFERENCES public.secciones(id, anio_lectivo_id, nivel_id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.asistencias_aula
    ADD CONSTRAINT asistencias_aula_auxiliar_usuario_id_fkey FOREIGN KEY (auxiliar_usuario_id) REFERENCES public.usuarios(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.asistencias_aula
    ADD CONSTRAINT asistencias_aula_matricula_id_fkey FOREIGN KEY (matricula_id) REFERENCES public.matriculas(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.auditoria_cambios
    ADD CONSTRAINT auditoria_cambios_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id) ON DELETE SET NULL;

ALTER TABLE ONLY public.calificaciones_cneb
    ADD CONSTRAINT calificaciones_cneb_asignacion_docente_id_seccion_id_area__fkey FOREIGN KEY (asignacion_docente_id, seccion_id, area_curricular_id, anio_lectivo_id, docente_usuario_id) REFERENCES public.asignaciones_docentes(id, seccion_id, area_curricular_id, anio_lectivo_id, docente_usuario_id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.calificaciones_cneb
    ADD CONSTRAINT calificaciones_cneb_competencia_id_area_curricular_id_fkey FOREIGN KEY (competencia_id, area_curricular_id) REFERENCES public.competencias(id, area_id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.calificaciones_cneb
    ADD CONSTRAINT calificaciones_cneb_matricula_id_anio_lectivo_id_seccion_i_fkey FOREIGN KEY (matricula_id, anio_lectivo_id, seccion_id) REFERENCES public.matriculas(id, anio_lectivo_id, seccion_id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.calificaciones_cneb
    ADD CONSTRAINT calificaciones_cneb_periodo_academico_id_anio_lectivo_id_fkey FOREIGN KEY (periodo_academico_id, anio_lectivo_id) REFERENCES public.periodos_academicos(id, anio_lectivo_id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.comprobantes_pago
    ADD CONSTRAINT comprobantes_pago_pago_transaccion_id_fkey FOREIGN KEY (pago_transaccion_id) REFERENCES public.pagos_transacciones(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.comunicado_destinatarios
    ADD CONSTRAINT comunicado_destinatarios_apoderado_id_fkey FOREIGN KEY (apoderado_id) REFERENCES public.apoderados(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.comunicado_destinatarios
    ADD CONSTRAINT comunicado_destinatarios_comunicado_id_fkey FOREIGN KEY (comunicado_id) REFERENCES public.comunicados_oficiales(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.comunicados_oficiales
    ADD CONSTRAINT comunicados_oficiales_remitente_usuario_id_fkey FOREIGN KEY (remitente_usuario_id) REFERENCES public.usuarios(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.conciliaciones_asistencia
    ADD CONSTRAINT conciliaciones_asistencia_estudiante_id_fkey FOREIGN KEY (estudiante_id) REFERENCES public.estudiantes(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.estudiante_apoderados
    ADD CONSTRAINT estudiante_apoderados_apoderado_id_fkey FOREIGN KEY (apoderado_id) REFERENCES public.apoderados(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.estudiante_apoderados
    ADD CONSTRAINT estudiante_apoderados_estudiante_id_fkey FOREIGN KEY (estudiante_id) REFERENCES public.estudiantes(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.competencias
    ADD CONSTRAINT fk_competencias_area_id FOREIGN KEY (area_id) REFERENCES public.areas_curriculares(id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE ONLY public.comprobantes_pago
    ADD CONSTRAINT fk_comprobantes_pago_serie FOREIGN KEY (serie) REFERENCES public.series_comprobante(serie) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE ONLY public.secciones
    ADD CONSTRAINT fk_secciones_aula FOREIGN KEY (aula_id) REFERENCES public.aulas(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.sesiones_refuerzo
    ADD CONSTRAINT fk_sesiones_refuerzo_aula_codigo FOREIGN KEY (codigo_aula) REFERENCES public.aulas(codigo_aula) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE ONLY public.solicitudes_matricula_documentos
    ADD CONSTRAINT fk_solicitudes_documentos_solicitud FOREIGN KEY (solicitud_id) REFERENCES public.solicitudes_matricula_publica(id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE ONLY public.grados
    ADD CONSTRAINT grados_nivel_id_fkey FOREIGN KEY (nivel_id) REFERENCES public.niveles(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.horarios_seccion
    ADD CONSTRAINT horarios_seccion_anio_lectivo_id_fkey FOREIGN KEY (anio_lectivo_id) REFERENCES public.anios_lectivos(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.horarios_seccion
    ADD CONSTRAINT horarios_seccion_asignacion_docente_id_anio_lectivo_id_sec_fkey FOREIGN KEY (asignacion_docente_id, anio_lectivo_id, seccion_id, docente_usuario_id) REFERENCES public.asignaciones_docentes(id, anio_lectivo_id, seccion_id, docente_usuario_id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.horarios_seccion
    ADD CONSTRAINT horarios_seccion_bloque_horario_id_fkey FOREIGN KEY (bloque_horario_id) REFERENCES public.bloques_horarios(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.horarios_seccion
    ADD CONSTRAINT horarios_seccion_docente_usuario_id_fkey FOREIGN KEY (docente_usuario_id) REFERENCES public.usuarios(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.incidencias_conductuales
    ADD CONSTRAINT incidencias_conductuales_matricula_id_fkey FOREIGN KEY (matricula_id) REFERENCES public.matriculas(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.incidencias_conductuales
    ADD CONSTRAINT incidencias_conductuales_reportado_por_usuario_id_fkey FOREIGN KEY (reportado_por_usuario_id) REFERENCES public.usuarios(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.inscripciones_refuerzo
    ADD CONSTRAINT inscripciones_refuerzo_calificacion_origen_id_fkey FOREIGN KEY (calificacion_origen_id) REFERENCES public.calificaciones_cneb(id) ON DELETE SET NULL;

ALTER TABLE ONLY public.inscripciones_refuerzo
    ADD CONSTRAINT inscripciones_refuerzo_estudiante_id_fkey FOREIGN KEY (estudiante_id) REFERENCES public.estudiantes(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.inscripciones_refuerzo
    ADD CONSTRAINT inscripciones_refuerzo_sesion_refuerzo_id_fkey FOREIGN KEY (sesion_refuerzo_id) REFERENCES public.sesiones_refuerzo(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.lotes_biometrico
    ADD CONSTRAINT lotes_biometrico_importado_por_usuario_id_fkey FOREIGN KEY (importado_por_usuario_id) REFERENCES public.usuarios(id) ON DELETE SET NULL;

ALTER TABLE ONLY public.marcas_biometrico_porteria
    ADD CONSTRAINT marcas_biometrico_porteria_estudiante_id_fkey FOREIGN KEY (estudiante_id) REFERENCES public.estudiantes(id) ON DELETE SET NULL;

ALTER TABLE ONLY public.marcas_biometrico_porteria
    ADD CONSTRAINT marcas_biometrico_porteria_lote_id_fkey FOREIGN KEY (lote_id) REFERENCES public.lotes_biometrico(id) ON DELETE SET NULL;

ALTER TABLE ONLY public.matriculas
    ADD CONSTRAINT matriculas_estudiante_id_fkey FOREIGN KEY (estudiante_id) REFERENCES public.estudiantes(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.matriculas
    ADD CONSTRAINT matriculas_seccion_id_anio_lectivo_id_fkey FOREIGN KEY (seccion_id, anio_lectivo_id) REFERENCES public.secciones(id, anio_lectivo_id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.obligaciones_pago
    ADD CONSTRAINT obligaciones_pago_concepto_id_tipo_concepto_fkey FOREIGN KEY (concepto_id, tipo_concepto) REFERENCES public.conceptos_cobro(id, tipo_concepto) ON DELETE RESTRICT;

ALTER TABLE ONLY public.obligaciones_pago
    ADD CONSTRAINT obligaciones_pago_matricula_id_fkey FOREIGN KEY (matricula_id) REFERENCES public.matriculas(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.pagos_transacciones
    ADD CONSTRAINT pagos_transacciones_obligacion_pago_id_fkey FOREIGN KEY (obligacion_pago_id) REFERENCES public.obligaciones_pago(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.periodos_academicos
    ADD CONSTRAINT periodos_academicos_anio_lectivo_id_fkey FOREIGN KEY (anio_lectivo_id) REFERENCES public.anios_lectivos(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.secciones
    ADD CONSTRAINT secciones_anio_lectivo_id_fkey FOREIGN KEY (anio_lectivo_id) REFERENCES public.anios_lectivos(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.secciones
    ADD CONSTRAINT secciones_grado_id_nivel_id_fkey FOREIGN KEY (grado_id, nivel_id) REFERENCES public.grados(id, nivel_id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.sesiones_refuerzo
    ADD CONSTRAINT sesiones_refuerzo_area_curricular_id_fkey FOREIGN KEY (area_curricular_id) REFERENCES public.areas_curriculares(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.sesiones_refuerzo
    ADD CONSTRAINT sesiones_refuerzo_docente_usuario_id_fkey FOREIGN KEY (docente_usuario_id) REFERENCES public.usuarios(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.sesiones_refuerzo
    ADD CONSTRAINT sesiones_refuerzo_periodo_academico_id_anio_lectivo_id_fkey FOREIGN KEY (periodo_academico_id, anio_lectivo_id) REFERENCES public.periodos_academicos(id, anio_lectivo_id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.sesiones
    ADD CONSTRAINT sesiones_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.solicitudes_matricula_publica
    ADD CONSTRAINT solicitudes_matricula_publica_anio_lectivo_id_fkey FOREIGN KEY (anio_lectivo_id) REFERENCES public.anios_lectivos(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.solicitudes_matricula_publica
    ADD CONSTRAINT solicitudes_matricula_publica_apoderado_id_fkey FOREIGN KEY (apoderado_id) REFERENCES public.apoderados(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.solicitudes_matricula_publica
    ADD CONSTRAINT solicitudes_matricula_publica_estudiante_id_fkey FOREIGN KEY (estudiante_id) REFERENCES public.estudiantes(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.solicitudes_matricula_publica
    ADD CONSTRAINT solicitudes_matricula_publica_matricula_id_fkey FOREIGN KEY (matricula_id) REFERENCES public.matriculas(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.solicitudes_matricula_publica
    ADD CONSTRAINT solicitudes_matricula_publica_seccion_id_anio_lectivo_id_fkey FOREIGN KEY (seccion_id, anio_lectivo_id) REFERENCES public.secciones(id, anio_lectivo_id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.usuario_roles
    ADD CONSTRAINT usuario_roles_rol_id_fkey FOREIGN KEY (rol_id) REFERENCES public.roles(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.usuario_roles
    ADD CONSTRAINT usuario_roles_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id) ON DELETE CASCADE;

ALTER TABLE public.anios_lectivos ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.apoderados ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.areas_curriculares ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.asignaciones_docentes ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.asistencias_aula ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.auditoria_cambios ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.aulas ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.bloques_horarios ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.calificaciones_cneb ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.competencias ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.comprobantes_pago ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.comunicado_destinatarios ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.comunicados_oficiales ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.conceptos_cobro ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.conciliaciones_asistencia ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.estudiante_apoderados ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.estudiantes ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.grados ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.horarios_seccion ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.incidencias_conductuales ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.inscripciones_refuerzo ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.lotes_biometrico ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.marcas_biometrico_porteria ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.matriculas ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.niveles ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.obligaciones_pago ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.pagos_transacciones ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.periodos_academicos ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.roles ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.secciones ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.series_comprobante ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.sesiones ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.sesiones_refuerzo ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.solicitudes_matricula_documentos ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.solicitudes_matricula_publica ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.usuario_roles ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.usuarios ENABLE ROW LEVEL SECURITY;

-- 2. CATÁLOGOS: datos académicos e institucionales; sin cuentas ni expedientes.
--
-- Data for Name: anios_lectivos; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.anios_lectivos (id, anio, fecha_inicio, fecha_fin, abierto) VALUES (1, 2026, '2026-03-01', '2026-12-20', true);

--
-- Data for Name: niveles; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.niveles (id, codigo_nivel, nombre_nivel) VALUES (1, 'INICIAL', 'Educación Inicial');
INSERT INTO public.niveles (id, codigo_nivel, nombre_nivel) VALUES (2, 'PRIMARIA', 'Educación Primaria');
INSERT INTO public.niveles (id, codigo_nivel, nombre_nivel) VALUES (3, 'SECUNDARIA', 'Educación Secundaria');

--
-- Data for Name: areas_curriculares; Type: TABLE DATA; Schema: public; Owner: -
--

--
-- Data for Name: aulas; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.aulas (id, codigo_aula, nombre_aula, ubicacion, capacidad, activa) VALUES (1, 'AULA_I_01', 'Pabellón Inicial - Aula 1', 'Pabellón A - Piso 1', 30, true);
INSERT INTO public.aulas (id, codigo_aula, nombre_aula, ubicacion, capacidad, activa) VALUES (2, 'AULA_I_02', 'Pabellón Inicial - Aula 2', 'Pabellón A - Piso 1', 30, true);
INSERT INTO public.aulas (id, codigo_aula, nombre_aula, ubicacion, capacidad, activa) VALUES (3, 'AULA_I_03', 'Pabellón Inicial - Aula 3', 'Pabellón A - Piso 1', 30, true);
INSERT INTO public.aulas (id, codigo_aula, nombre_aula, ubicacion, capacidad, activa) VALUES (4, 'AULA_P_101', 'Pabellón Primaria - Aula 101', 'Pabellón B - Piso 1', 35, true);
INSERT INTO public.aulas (id, codigo_aula, nombre_aula, ubicacion, capacidad, activa) VALUES (5, 'AULA_P_102', 'Pabellón Primaria - Aula 102', 'Pabellón B - Piso 1', 35, true);
INSERT INTO public.aulas (id, codigo_aula, nombre_aula, ubicacion, capacidad, activa) VALUES (6, 'AULA_P_103', 'Pabellón Primaria - Aula 103', 'Pabellón B - Piso 2', 35, true);
INSERT INTO public.aulas (id, codigo_aula, nombre_aula, ubicacion, capacidad, activa) VALUES (7, 'AULA_P_104', 'Pabellón Primaria - Aula 104', 'Pabellón B - Piso 2', 35, true);
INSERT INTO public.aulas (id, codigo_aula, nombre_aula, ubicacion, capacidad, activa) VALUES (8, 'AULA_P_105', 'Pabellón Primaria - Aula 105', 'Pabellón B - Piso 3', 35, true);
INSERT INTO public.aulas (id, codigo_aula, nombre_aula, ubicacion, capacidad, activa) VALUES (9, 'AULA_P_106', 'Pabellón Primaria - Aula 106', 'Pabellón B - Piso 3', 35, true);
INSERT INTO public.aulas (id, codigo_aula, nombre_aula, ubicacion, capacidad, activa) VALUES (10, 'AULA_S_201', 'Pabellón Secundaria - Aula 201', 'Pabellón C - Piso 1', 35, true);
INSERT INTO public.aulas (id, codigo_aula, nombre_aula, ubicacion, capacidad, activa) VALUES (11, 'AULA_S_202', 'Pabellón Secundaria - Aula 202', 'Pabellón C - Piso 1', 35, true);
INSERT INTO public.aulas (id, codigo_aula, nombre_aula, ubicacion, capacidad, activa) VALUES (12, 'AULA_S_203', 'Pabellón Secundaria - Aula 203', 'Pabellón C - Piso 2', 35, true);
INSERT INTO public.aulas (id, codigo_aula, nombre_aula, ubicacion, capacidad, activa) VALUES (13, 'AULA_S_204', 'Pabellón Secundaria - Aula 204', 'Pabellón C - Piso 2', 35, true);
INSERT INTO public.aulas (id, codigo_aula, nombre_aula, ubicacion, capacidad, activa) VALUES (14, 'AULA_S_205', 'Pabellón Secundaria - Aula 205', 'Pabellón C - Piso 3', 35, true);

--
-- Data for Name: competencias; Type: TABLE DATA; Schema: public; Owner: -
--

--
-- Data for Name: conceptos_cobro; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.conceptos_cobro (id, codigo_concepto, nombre_concepto, tipo_concepto, monto_sugerido) VALUES (1, 'MATRICULA_ANUAL', 'Derecho de Matrícula y Admisión Anual', 'MATRICULA', 350.00);
INSERT INTO public.conceptos_cobro (id, codigo_concepto, nombre_concepto, tipo_concepto, monto_sugerido) VALUES (2, 'PENSION_MENSUAL', 'Pensión Escolar de Enseñanza (Cuotas 1 a 10)', 'PENSION', 380.00);
INSERT INTO public.conceptos_cobro (id, codigo_concepto, nombre_concepto, tipo_concepto, monto_sugerido) VALUES (3, 'CONSTANCIA_ESTUDIOS', 'Emisión de Constancia de Estudios', 'CERTIFICADO', 25.00);

--
-- Data for Name: grados; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.grados (id, nivel_id, numero_grado, nombre_grado) VALUES (1, 1, 1, 'Inicial 3 años');
INSERT INTO public.grados (id, nivel_id, numero_grado, nombre_grado) VALUES (2, 1, 2, 'Inicial 4 años');
INSERT INTO public.grados (id, nivel_id, numero_grado, nombre_grado) VALUES (3, 1, 3, 'Inicial 5 años');
INSERT INTO public.grados (id, nivel_id, numero_grado, nombre_grado) VALUES (4, 2, 1, '1° de Primaria');
INSERT INTO public.grados (id, nivel_id, numero_grado, nombre_grado) VALUES (5, 2, 2, '2° de Primaria');
INSERT INTO public.grados (id, nivel_id, numero_grado, nombre_grado) VALUES (6, 2, 3, '3° de Primaria');
INSERT INTO public.grados (id, nivel_id, numero_grado, nombre_grado) VALUES (7, 2, 4, '4° de Primaria');
INSERT INTO public.grados (id, nivel_id, numero_grado, nombre_grado) VALUES (8, 2, 5, '5° de Primaria');
INSERT INTO public.grados (id, nivel_id, numero_grado, nombre_grado) VALUES (9, 2, 6, '6° de Primaria');
INSERT INTO public.grados (id, nivel_id, numero_grado, nombre_grado) VALUES (10, 3, 1, '1° de Secundaria');
INSERT INTO public.grados (id, nivel_id, numero_grado, nombre_grado) VALUES (11, 3, 2, '2° de Secundaria');
INSERT INTO public.grados (id, nivel_id, numero_grado, nombre_grado) VALUES (12, 3, 3, '3° de Secundaria');
INSERT INTO public.grados (id, nivel_id, numero_grado, nombre_grado) VALUES (13, 3, 4, '4° de Secundaria');
INSERT INTO public.grados (id, nivel_id, numero_grado, nombre_grado) VALUES (14, 3, 5, '5° de Secundaria');

--
-- Data for Name: periodos_academicos; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.periodos_academicos (id, anio_lectivo_id, numero_periodo, nombre_periodo, fecha_inicio, fecha_fin, cerrado, activo) VALUES (1, 1, 1, 'I Bimestre', '2026-03-01', '2026-05-08', false, true);
INSERT INTO public.periodos_academicos (id, anio_lectivo_id, numero_periodo, nombre_periodo, fecha_inicio, fecha_fin, cerrado, activo) VALUES (2, 1, 2, 'II Bimestre', '2026-05-18', '2026-07-24', false, true);
INSERT INTO public.periodos_academicos (id, anio_lectivo_id, numero_periodo, nombre_periodo, fecha_inicio, fecha_fin, cerrado, activo) VALUES (3, 1, 3, 'III Bimestre', '2026-08-10', '2026-10-09', false, true);
INSERT INTO public.periodos_academicos (id, anio_lectivo_id, numero_periodo, nombre_periodo, fecha_inicio, fecha_fin, cerrado, activo) VALUES (4, 1, 4, 'IV Bimestre', '2026-10-19', '2026-12-18', false, true);

--
-- Data for Name: roles; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.roles (id, codigo_rol, nombre_rol) VALUES (1, 'DIRECCION', 'Dirección General');
INSERT INTO public.roles (id, codigo_rol, nombre_rol) VALUES (2, 'SECRETARIA', 'Secretaría Académica');
INSERT INTO public.roles (id, codigo_rol, nombre_rol) VALUES (3, 'DOCENTE', 'Plana Docente');
INSERT INTO public.roles (id, codigo_rol, nombre_rol) VALUES (4, 'AUXILIAR', 'Auxiliar de Educación');
INSERT INTO public.roles (id, codigo_rol, nombre_rol) VALUES (5, 'TUTOR', 'Tutor de Aula');
INSERT INTO public.roles (id, codigo_rol, nombre_rol) VALUES (6, 'APODERADO', 'Padre de Familia / Tutor');

--
-- Data for Name: secciones; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.secciones (id, anio_lectivo_id, grado_id, nivel_id, letra, cupo_maximo, vacantes_ocupadas, aula_id) VALUES (1, 1, 1, 1, 'A', 30, 0, 1);
INSERT INTO public.secciones (id, anio_lectivo_id, grado_id, nivel_id, letra, cupo_maximo, vacantes_ocupadas, aula_id) VALUES (2, 1, 2, 1, 'A', 30, 0, 2);
INSERT INTO public.secciones (id, anio_lectivo_id, grado_id, nivel_id, letra, cupo_maximo, vacantes_ocupadas, aula_id) VALUES (3, 1, 3, 1, 'A', 30, 0, 3);
INSERT INTO public.secciones (id, anio_lectivo_id, grado_id, nivel_id, letra, cupo_maximo, vacantes_ocupadas, aula_id) VALUES (4, 1, 4, 2, 'A', 35, 0, 4);
INSERT INTO public.secciones (id, anio_lectivo_id, grado_id, nivel_id, letra, cupo_maximo, vacantes_ocupadas, aula_id) VALUES (5, 1, 5, 2, 'A', 35, 0, 5);
INSERT INTO public.secciones (id, anio_lectivo_id, grado_id, nivel_id, letra, cupo_maximo, vacantes_ocupadas, aula_id) VALUES (6, 1, 6, 2, 'A', 35, 0, 6);
INSERT INTO public.secciones (id, anio_lectivo_id, grado_id, nivel_id, letra, cupo_maximo, vacantes_ocupadas, aula_id) VALUES (7, 1, 7, 2, 'A', 35, 0, 7);
INSERT INTO public.secciones (id, anio_lectivo_id, grado_id, nivel_id, letra, cupo_maximo, vacantes_ocupadas, aula_id) VALUES (8, 1, 8, 2, 'A', 35, 0, 8);
INSERT INTO public.secciones (id, anio_lectivo_id, grado_id, nivel_id, letra, cupo_maximo, vacantes_ocupadas, aula_id) VALUES (9, 1, 9, 2, 'A', 35, 0, 9);
INSERT INTO public.secciones (id, anio_lectivo_id, grado_id, nivel_id, letra, cupo_maximo, vacantes_ocupadas, aula_id) VALUES (10, 1, 10, 3, 'A', 35, 0, 10);
INSERT INTO public.secciones (id, anio_lectivo_id, grado_id, nivel_id, letra, cupo_maximo, vacantes_ocupadas, aula_id) VALUES (11, 1, 11, 3, 'A', 35, 0, 11);
INSERT INTO public.secciones (id, anio_lectivo_id, grado_id, nivel_id, letra, cupo_maximo, vacantes_ocupadas, aula_id) VALUES (12, 1, 12, 3, 'A', 35, 0, 12);
INSERT INTO public.secciones (id, anio_lectivo_id, grado_id, nivel_id, letra, cupo_maximo, vacantes_ocupadas, aula_id) VALUES (13, 1, 13, 3, 'A', 35, 0, 13);
INSERT INTO public.secciones (id, anio_lectivo_id, grado_id, nivel_id, letra, cupo_maximo, vacantes_ocupadas, aula_id) VALUES (14, 1, 14, 3, 'A', 35, 0, 14);

--
-- Data for Name: series_comprobante; Type: TABLE DATA; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.anios_lectivos_id_seq', 2, true);

SELECT pg_catalog.setval('public.areas_curriculares_id_seq', 1, false);

SELECT pg_catalog.setval('public.aulas_id_seq', 28, true);

SELECT pg_catalog.setval('public.competencias_id_seq', 1, false);

SELECT pg_catalog.setval('public.conceptos_cobro_id_seq', 3, true);

SELECT pg_catalog.setval('public.grados_id_seq', 28, true);

SELECT pg_catalog.setval('public.niveles_id_seq', 3, true);

SELECT pg_catalog.setval('public.periodos_academicos_id_seq', 8, true);

SELECT pg_catalog.setval('public.roles_id_seq', 6, true);

SELECT pg_catalog.setval('public.secciones_id_seq', 28, true);

-- 3. PRIVILEGIOS: acceso a través del backend; sin acceso directo de clientes Supabase.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
        REVOKE ALL ON ALL TABLES IN SCHEMA public FROM anon;
        REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM anon;
        REVOKE ALL ON ALL ROUTINES IN SCHEMA public FROM anon;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON TABLES FROM anon;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON SEQUENCES FROM anon;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON ROUTINES FROM anon;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
        REVOKE ALL ON ALL TABLES IN SCHEMA public FROM authenticated;
        REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM authenticated;
        REVOKE ALL ON ALL ROUTINES IN SCHEMA public FROM authenticated;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON TABLES FROM authenticated;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON SEQUENCES FROM authenticated;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON ROUTINES FROM authenticated;
    END IF;
END
$$;

-- Evidencia y comprobaciones automáticas de documentos V20
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

ALTER TABLE solicitudes_matricula_documentos ADD CONSTRAINT fk_documento_version_actual
  FOREIGN KEY (version_id, solicitud_id, tipo) REFERENCES versiones_documentos_matricula (id, solicitud_id, tipo) ON DELETE RESTRICT;

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

-- V21: series base; conserva correlativos existentes.
INSERT INTO series_comprobante (serie, ultimo_correlativo) VALUES ('B001',0),('E001',0) ON CONFLICT (serie) DO NOTHING;
