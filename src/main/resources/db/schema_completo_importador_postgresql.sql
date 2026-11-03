-- Schema-only snapshot from Docker shuji-postgres, database Sistema Escolar, schema public, Flyway V45.
-- Contains no table rows, owners, ACLs, or Flyway history.

--
-- PostgreSQL database dump
--

\restrict ip8cG6GQ98GFcFJ7PhYNPY9rTmpRdJSDAuczyafvtNc7lUDu2nYgqb7OjRhtbL1

-- Dumped from database version 16.15
-- Dumped by pg_dump version 16.15

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: public; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA public;


--
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON SCHEMA public IS 'standard public schema';


--
-- Name: fn_actualizar_fecha(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_actualizar_fecha() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.fecha_actualizacion = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;


--
-- Name: fn_anular_comprobante_pago(); Type: FUNCTION; Schema: public; Owner: -
--

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


--
-- Name: fn_aplicar_pago_obligacion(); Type: FUNCTION; Schema: public; Owner: -
--

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


--
-- Name: fn_auditar_cambios(); Type: FUNCTION; Schema: public; Owner: -
--

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


--
-- Name: fn_auditar_cambios_seguro(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_auditar_cambios_seguro() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    SET search_path TO 'public', 'pg_temp'
    AS $$
DECLARE
    fila_nueva JSONB;
    fila_anterior JSONB;
    id_registro BIGINT;
    clave_registro VARCHAR(500);
    actor_id BIGINT := NULLIF(current_setting('app.current_user_id', true), '')::BIGINT;
    actor_nombre VARCHAR(100) := NULLIF(current_setting('app.current_username', true), '');
    actor_roles VARCHAR(300) := NULLIF(current_setting('app.current_roles', true), '');
BEGIN
    actor_nombre := COALESCE(
        actor_nombre,
        (SELECT u.nombre_usuario FROM usuarios u WHERE u.id = actor_id),
        CASE WHEN actor_id IS NULL THEN 'SISTEMA' ELSE 'USUARIO_NO_RESUELTO' END
    );
    actor_roles := COALESCE(
        actor_roles,
        (SELECT string_agg(regexp_replace(r.codigo_rol, '^ROLE_', ''), ',' ORDER BY r.codigo_rol)
           FROM usuario_roles ur
           JOIN roles r ON r.id = ur.rol_id
          WHERE ur.usuario_id = actor_id),
        CASE WHEN actor_id IS NULL THEN 'SISTEMA' ELSE 'ROL_NO_RESUELTO' END
    );

    IF TG_OP = 'INSERT' THEN
        fila_nueva := to_jsonb(NEW);
        clave_registro := fn_clave_auditoria(TG_RELID, fila_nueva);
        IF EXISTS (
            SELECT 1 FROM pg_attribute
            WHERE attrelid = TG_RELID AND attname = 'id'
              AND atttypid IN ('smallint'::REGTYPE, 'integer'::REGTYPE, 'bigint'::REGTYPE)
              AND NOT attisdropped
        ) THEN
            id_registro := (fila_nueva ->> 'id')::BIGINT;
        ELSE
            id_registro := NULL;
        END IF;
        fila_nueva := fn_sanitizar_fila_auditoria(fila_nueva);
        INSERT INTO auditoria_cambios (
            tabla_afectada, registro_id, registro_clave, accion, datos_nuevos, usuario_id,
            usuario_nombre_snapshot, roles_usuario_snapshot
        ) VALUES (
            TG_TABLE_NAME, id_registro, clave_registro, TG_OP, fila_nueva, actor_id,
            actor_nombre, actor_roles
        );
        RETURN NEW;
    ELSIF TG_OP = 'UPDATE' THEN
        fila_anterior := to_jsonb(OLD);
        fila_nueva := to_jsonb(NEW);
        clave_registro := fn_clave_auditoria(TG_RELID, fila_nueva);
        IF EXISTS (
            SELECT 1 FROM pg_attribute
            WHERE attrelid = TG_RELID AND attname = 'id'
              AND atttypid IN ('smallint'::REGTYPE, 'integer'::REGTYPE, 'bigint'::REGTYPE)
              AND NOT attisdropped
        ) THEN
            id_registro := (fila_nueva ->> 'id')::BIGINT;
        ELSE
            id_registro := NULL;
        END IF;
        fila_anterior := fn_sanitizar_fila_auditoria(fila_anterior);
        fila_nueva := fn_sanitizar_fila_auditoria(fila_nueva);
        INSERT INTO auditoria_cambios (
            tabla_afectada, registro_id, registro_clave, accion, datos_anteriores, datos_nuevos,
            usuario_id, usuario_nombre_snapshot, roles_usuario_snapshot
        ) VALUES (
            TG_TABLE_NAME, id_registro, clave_registro, TG_OP, fila_anterior, fila_nueva,
            actor_id, actor_nombre, actor_roles
        );
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        fila_anterior := to_jsonb(OLD);
        clave_registro := fn_clave_auditoria(TG_RELID, fila_anterior);
        IF EXISTS (
            SELECT 1 FROM pg_attribute
            WHERE attrelid = TG_RELID AND attname = 'id'
              AND atttypid IN ('smallint'::REGTYPE, 'integer'::REGTYPE, 'bigint'::REGTYPE)
              AND NOT attisdropped
        ) THEN
            id_registro := (fila_anterior ->> 'id')::BIGINT;
        ELSE
            id_registro := NULL;
        END IF;
        fila_anterior := fn_sanitizar_fila_auditoria(fila_anterior);
        INSERT INTO auditoria_cambios (
            tabla_afectada, registro_id, registro_clave, accion, datos_anteriores, usuario_id,
            usuario_nombre_snapshot, roles_usuario_snapshot
        ) VALUES (
            TG_TABLE_NAME, id_registro, clave_registro, TG_OP, fila_anterior, actor_id,
            actor_nombre, actor_roles
        );
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$;


--
-- Name: fn_clave_auditoria(oid, jsonb); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_clave_auditoria(tabla_oid oid, fila jsonb) RETURNS text
    LANGUAGE plpgsql STABLE
    SET search_path TO 'public', 'pg_temp'
    AS $$
DECLARE
    clave JSONB;
BEGIN
    SELECT jsonb_object_agg(columna.attname, fila -> columna.attname)
      INTO clave
      FROM pg_index indice
      CROSS JOIN LATERAL unnest(indice.indkey) WITH ORDINALITY AS llave(attnum, orden)
      JOIN pg_attribute columna
        ON columna.attrelid = indice.indrelid
       AND columna.attnum = llave.attnum
     WHERE indice.indrelid = tabla_oid
       AND indice.indisprimary;

    IF clave IS NULL THEN
        RETURN 'hash:' || md5(fn_sanitizar_fila_auditoria(fila)::TEXT);
    END IF;

    RETURN left(fn_sanitizar_fila_auditoria(clave)::TEXT, 500);
END;
$$;


--
-- Name: fn_gestionar_vacante_matricula(); Type: FUNCTION; Schema: public; Owner: -
--

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


--
-- Name: fn_impedir_mutacion_auditoria(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_impedir_mutacion_auditoria() RETURNS trigger
    LANGUAGE plpgsql
    SET search_path TO 'public', 'pg_temp'
    AS $$
BEGIN
    RAISE EXCEPTION 'Los registros de auditoría son inmutables';
END;
$$;


--
-- Name: fn_liberar_reservas_expiradas(); Type: FUNCTION; Schema: public; Owner: -
--

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


--
-- Name: fn_prevenir_cambio_obligacion_pago(); Type: FUNCTION; Schema: public; Owner: -
--

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


--
-- Name: fn_prevenir_cambio_pago_comprobante(); Type: FUNCTION; Schema: public; Owner: -
--

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


--
-- Name: fn_proteger_historial_estados_incidencia(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_proteger_historial_estados_incidencia() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    RAISE EXCEPTION 'El historial de estados de incidencias es inmutable';
END;
$$;


--
-- Name: fn_proteger_version_documental(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_proteger_version_documental() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
  RAISE EXCEPTION 'La evidencia documental es inmutable; registre un nuevo intento de verificación.';
END;
$$;


--
-- Name: fn_sanitizar_fila_auditoria(jsonb); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_sanitizar_fila_auditoria(fila jsonb) RETURNS jsonb
    LANGUAGE sql IMMUTABLE
    SET search_path TO 'public', 'pg_temp'
    AS $$
    SELECT COALESCE(jsonb_object_agg(campo.key, campo.value), '{}'::JSONB)
    FROM jsonb_each(COALESCE(fila, '{}'::JSONB)) AS campo
    WHERE campo.key !~* '(password|contrase|token|secret|clave|dni|documento|archivo|contenido|email|correo|direccion|telefono|celular|url|foto|imagen|observaciones|motivo|justificacion)';
$$;


--
-- Name: fn_validar_comprobante_pago(); Type: FUNCTION; Schema: public; Owner: -
--

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


--
-- Name: fn_validar_estado_comprobante_pago(); Type: FUNCTION; Schema: public; Owner: -
--

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


--
-- Name: fn_validar_transicion_pago(); Type: FUNCTION; Schema: public; Owner: -
--

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


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: anios_lectivos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.anios_lectivos (
    id smallint NOT NULL,
    anio smallint NOT NULL,
    fecha_inicio date NOT NULL,
    fecha_fin date NOT NULL,
    abierto boolean DEFAULT true NOT NULL,
    CONSTRAINT anios_lectivos_anio_check CHECK ((anio >= 2024)),
    CONSTRAINT anios_lectivos_check CHECK ((fecha_fin > fecha_inicio))
);


--
-- Name: anios_lectivos_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.anios_lectivos_id_seq
    AS smallint
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: anios_lectivos_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.anios_lectivos_id_seq OWNED BY public.anios_lectivos.id;


--
-- Name: apoderados; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.apoderados (
    id bigint NOT NULL,
    usuario_id bigint,
    tipo_documento character varying(3) DEFAULT 'DNI'::character varying NOT NULL,
    numero_documento character varying(8) NOT NULL,
    nombres_apoderado character varying(20) NOT NULL,
    apellido_paterno_apoderado character varying(20) NOT NULL,
    apellido_materno_apoderado character varying(20) NOT NULL,
    celular character varying(9) NOT NULL,
    correo_electronico character varying(100),
    direccion character varying(150) NOT NULL,
    ubigeo_inei character varying(6),
    validado_reniec boolean DEFAULT false NOT NULL,
    origen_registro character varying(20) DEFAULT 'RENIEC_API'::character varying NOT NULL,
    fecha_creacion timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    activo boolean DEFAULT true NOT NULL,
    estado character varying(20) DEFAULT 'ACTIVO'::character varying NOT NULL,
    CONSTRAINT apoderados_celular_check CHECK (((celular)::text ~ '^9[0-9]{8}$'::text)),
    CONSTRAINT apoderados_origen_registro_check CHECK (((origen_registro)::text = ANY ((ARRAY['RENIEC_API'::character varying, 'MANUAL_CONTINGENCIA'::character varying])::text[]))),
    CONSTRAINT apoderados_tipo_documento_check CHECK (((tipo_documento)::text = 'DNI'::text)),
    CONSTRAINT apoderados_ubigeo_inei_check CHECK (((ubigeo_inei)::text ~ '^[0-9]{6}$'::text)),
    CONSTRAINT chk_apoderados_dni_8_digitos CHECK (((numero_documento)::text ~ '^[0-9]{8}$'::text)),
    CONSTRAINT chk_apoderados_estado CHECK (((estado)::text = ANY ((ARRAY['ACTIVO'::character varying, 'SUSPENDIDO'::character varying, 'INACTIVO'::character varying])::text[])))
);


--
-- Name: apoderados_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.apoderados_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: apoderados_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.apoderados_id_seq OWNED BY public.apoderados.id;


--
-- Name: apoderados_suplentes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.apoderados_suplentes (
    id bigint NOT NULL,
    estudiante_id bigint NOT NULL,
    apoderado_titular_id bigint NOT NULL,
    numero_documento_suplente character varying(8) NOT NULL,
    nombres_suplente character varying(20) NOT NULL,
    apellido_paterno_suplente character varying(20) NOT NULL,
    apellido_materno_suplente character varying(20) NOT NULL,
    parentesco_suplente character varying(30) NOT NULL,
    celular_suplente character varying(9) NOT NULL,
    fecha_inicio date NOT NULL,
    fecha_fin date NOT NULL,
    estado character varying(20) DEFAULT 'PENDIENTE_APROBACION'::character varying NOT NULL,
    motivo_resolucion character varying(500),
    autorizado_por_usuario_id bigint,
    fecha_solicitud timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    fecha_resolucion timestamp with time zone,
    CONSTRAINT apoderados_suplentes_apellido_materno_suplente_check CHECK ((btrim((apellido_materno_suplente)::text) <> ''::text)),
    CONSTRAINT apoderados_suplentes_apellido_paterno_suplente_check CHECK ((btrim((apellido_paterno_suplente)::text) <> ''::text)),
    CONSTRAINT apoderados_suplentes_celular_suplente_check CHECK (((celular_suplente)::text ~ '^9[0-9]{8}$'::text)),
    CONSTRAINT apoderados_suplentes_estado_check CHECK (((estado)::text = ANY ((ARRAY['PENDIENTE_APROBACION'::character varying, 'ACTIVO'::character varying, 'RECHAZADO'::character varying, 'REVOCADO'::character varying])::text[]))),
    CONSTRAINT apoderados_suplentes_nombres_suplente_check CHECK ((btrim((nombres_suplente)::text) <> ''::text)),
    CONSTRAINT apoderados_suplentes_numero_documento_suplente_check CHECK (((numero_documento_suplente)::text ~ '^[0-9]{8}$'::text)),
    CONSTRAINT apoderados_suplentes_parentesco_suplente_check CHECK ((btrim((parentesco_suplente)::text) <> ''::text)),
    CONSTRAINT ck_suplencia_fechas CHECK ((fecha_fin >= fecha_inicio)),
    CONSTRAINT ck_suplencia_motivo CHECK ((((estado)::text <> ALL ((ARRAY['RECHAZADO'::character varying, 'REVOCADO'::character varying])::text[])) OR ((motivo_resolucion IS NOT NULL) AND (btrim((motivo_resolucion)::text) <> ''::text)))),
    CONSTRAINT ck_suplencia_resolucion CHECK ((((estado)::text = 'PENDIENTE_APROBACION'::text) OR ((autorizado_por_usuario_id IS NOT NULL) AND (fecha_resolucion IS NOT NULL))))
);


--
-- Name: apoderados_suplentes_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.apoderados_suplentes ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.apoderados_suplentes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: areas_curriculares; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.areas_curriculares (
    id smallint NOT NULL,
    nivel_id smallint NOT NULL,
    codigo_area character varying(20) NOT NULL,
    nombre_area character varying(60) NOT NULL
);


--
-- Name: areas_curriculares_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.areas_curriculares_id_seq
    AS smallint
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: areas_curriculares_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.areas_curriculares_id_seq OWNED BY public.areas_curriculares.id;


--
-- Name: asignaciones_docentes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.asignaciones_docentes (
    id bigint NOT NULL,
    docente_usuario_id bigint NOT NULL,
    seccion_id integer NOT NULL,
    anio_lectivo_id smallint NOT NULL,
    nivel_id smallint NOT NULL,
    area_curricular_id smallint NOT NULL
);


--
-- Name: asignaciones_docentes_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.asignaciones_docentes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: asignaciones_docentes_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.asignaciones_docentes_id_seq OWNED BY public.asignaciones_docentes.id;


--
-- Name: asistencias_aula; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.asistencias_aula (
    id bigint NOT NULL,
    matricula_id bigint NOT NULL,
    fecha_sesion date NOT NULL,
    hora_registro time without time zone NOT NULL,
    estado character varying(25) NOT NULL,
    auxiliar_usuario_id bigint,
    justificada boolean DEFAULT false NOT NULL,
    motivo_justificacion text,
    documento_sustento_key character varying(255),
    CONSTRAINT asistencias_aula_estado_check CHECK (((estado)::text = ANY ((ARRAY['PRESENTE'::character varying, 'TARDANZA'::character varying, 'FALTA_INJUSTIFICADA'::character varying, 'FALTA_JUSTIFICADA'::character varying])::text[]))),
    CONSTRAINT chk_asistencias_justificacion_longitud CHECK ((char_length(motivo_justificacion) <= 4000))
);


--
-- Name: asistencias_aula_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.asistencias_aula_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: asistencias_aula_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.asistencias_aula_id_seq OWNED BY public.asistencias_aula.id;


--
-- Name: auditoria_accesos_api; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.auditoria_accesos_api (
    id bigint NOT NULL,
    solicitud_id character varying(36) NOT NULL,
    usuario_id bigint,
    usuario_nombre character varying(100) NOT NULL,
    roles character varying(300) NOT NULL,
    metodo character varying(10) NOT NULL,
    ruta character varying(500) NOT NULL,
    estado_http integer NOT NULL,
    direccion_ip character varying(45),
    agente_usuario character varying(300),
    duracion_ms bigint NOT NULL,
    fecha_evento timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: auditoria_accesos_api_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.auditoria_accesos_api ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.auditoria_accesos_api_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: auditoria_cambios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.auditoria_cambios (
    id bigint NOT NULL,
    tabla_afectada character varying(50) NOT NULL,
    registro_id bigint,
    accion character varying(10) NOT NULL,
    datos_anteriores jsonb,
    datos_nuevos jsonb,
    usuario_id bigint,
    fecha_creacion timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    usuario_nombre_snapshot character varying(100),
    roles_usuario_snapshot character varying(300),
    registro_clave character varying(500) NOT NULL,
    CONSTRAINT auditoria_cambios_accion_check CHECK (((accion)::text = ANY ((ARRAY['INSERT'::character varying, 'UPDATE'::character varying, 'DELETE'::character varying])::text[])))
);


--
-- Name: auditoria_cambios_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.auditoria_cambios_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: auditoria_cambios_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.auditoria_cambios_id_seq OWNED BY public.auditoria_cambios.id;


--
-- Name: aulas; Type: TABLE; Schema: public; Owner: -
--

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


--
-- Name: aulas_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.aulas_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: aulas_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.aulas_id_seq OWNED BY public.aulas.id;


--
-- Name: bloques_horarios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bloques_horarios (
    id smallint NOT NULL,
    numero_bloque smallint NOT NULL,
    hora_inicio time without time zone NOT NULL,
    hora_fin time without time zone NOT NULL,
    es_recreo boolean DEFAULT false NOT NULL,
    CONSTRAINT bloques_horarios_check CHECK ((hora_fin > hora_inicio))
);


--
-- Name: bloques_horarios_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.bloques_horarios_id_seq
    AS smallint
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: bloques_horarios_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.bloques_horarios_id_seq OWNED BY public.bloques_horarios.id;


--
-- Name: calificaciones_cneb; Type: TABLE; Schema: public; Owner: -
--

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
    CONSTRAINT calificaciones_cneb_calificacion_cualitativa_check CHECK (((calificacion_cualitativa)::text = ANY ((ARRAY['AD'::character varying, 'A'::character varying, 'B'::character varying, 'C'::character varying])::text[]))),
    CONSTRAINT chk_calificaciones_conclusion_longitud CHECK ((char_length(conclusion_descriptiva) <= 4000))
);


--
-- Name: calificaciones_cneb_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.calificaciones_cneb_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: calificaciones_cneb_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.calificaciones_cneb_id_seq OWNED BY public.calificaciones_cneb.id;


--
-- Name: competencias; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.competencias (
    id smallint NOT NULL,
    area_id smallint NOT NULL,
    numero_orden smallint NOT NULL,
    nombre_competencia character varying(50) NOT NULL,
    descripcion text,
    CONSTRAINT chk_competencias_descripcion_longitud CHECK ((char_length(descripcion) <= 4000)),
    CONSTRAINT competencias_numero_orden_check CHECK ((numero_orden > 0))
);


--
-- Name: competencias_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.competencias_id_seq
    AS smallint
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: competencias_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.competencias_id_seq OWNED BY public.competencias.id;


--
-- Name: comprobantes_pago; Type: TABLE; Schema: public; Owner: -
--

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
    CONSTRAINT comprobantes_pago_estado_comprobante_check CHECK (((estado_comprobante)::text = ANY ((ARRAY['EMITIDO'::character varying, 'ANULADO'::character varying])::text[]))),
    CONSTRAINT comprobantes_pago_monto_total_check CHECK ((monto_total > 0.00)),
    CONSTRAINT comprobantes_pago_serie_check CHECK (((serie)::text ~ '^[BE][0-9]{3}$'::text)),
    CONSTRAINT comprobantes_pago_tipo_comprobante_check CHECK (((tipo_comprobante)::text = ANY ((ARRAY['BOLETA'::character varying, 'RECIBO_INTERNO'::character varying])::text[])))
);


--
-- Name: comprobantes_pago_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.comprobantes_pago_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: comprobantes_pago_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.comprobantes_pago_id_seq OWNED BY public.comprobantes_pago.id;


--
-- Name: comunicado_destinatarios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.comunicado_destinatarios (
    id bigint NOT NULL,
    comunicado_id bigint NOT NULL,
    apoderado_id bigint NOT NULL,
    leido boolean DEFAULT false NOT NULL,
    fecha_lectura timestamp with time zone,
    acuse_confirmado boolean DEFAULT false NOT NULL,
    fecha_acuse timestamp with time zone
);


--
-- Name: comunicado_destinatarios_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.comunicado_destinatarios_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: comunicado_destinatarios_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.comunicado_destinatarios_id_seq OWNED BY public.comunicado_destinatarios.id;


--
-- Name: comunicados_oficiales; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.comunicados_oficiales (
    id bigint NOT NULL,
    titulo_comunicado character varying(40) NOT NULL,
    contenido text NOT NULL,
    remitente_usuario_id bigint NOT NULL,
    fecha_publicacion timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    requiere_acuse boolean DEFAULT false NOT NULL,
    CONSTRAINT chk_comunicados_contenido_longitud CHECK ((char_length(contenido) <= 20000))
);


--
-- Name: comunicados_oficiales_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.comunicados_oficiales_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: comunicados_oficiales_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.comunicados_oficiales_id_seq OWNED BY public.comunicados_oficiales.id;


--
-- Name: conceptos_cobro; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.conceptos_cobro (
    id smallint NOT NULL,
    codigo_concepto character varying(30) NOT NULL,
    nombre_concepto character varying(50) NOT NULL,
    tipo_concepto character varying(20) NOT NULL,
    monto_sugerido numeric(10,2) NOT NULL,
    CONSTRAINT conceptos_cobro_monto_sugerido_check CHECK ((monto_sugerido >= 0.00)),
    CONSTRAINT conceptos_cobro_tipo_concepto_check CHECK (((tipo_concepto)::text = ANY ((ARRAY['MATRICULA'::character varying, 'PENSION'::character varying, 'CERTIFICADO'::character varying, 'OTRO'::character varying])::text[])))
);


--
-- Name: conceptos_cobro_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.conceptos_cobro_id_seq
    AS smallint
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: conceptos_cobro_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.conceptos_cobro_id_seq OWNED BY public.conceptos_cobro.id;


--
-- Name: conciliaciones_asistencia; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.conciliaciones_asistencia (
    id bigint NOT NULL,
    fecha date NOT NULL,
    estudiante_id bigint NOT NULL,
    marco_porteria boolean NOT NULL,
    presente_aula boolean NOT NULL,
    tipo_discrepancia character varying(35) NOT NULL,
    alerta_notificada boolean DEFAULT false NOT NULL,
    CONSTRAINT conciliaciones_asistencia_tipo_discrepancia_check CHECK (((tipo_discrepancia)::text = ANY ((ARRAY['ASISTENCIA_CONCILIADA'::character varying, 'DISCREPANCIA_FUGA'::character varying, 'DISCREPANCIA_OMISION_PORTERIA'::character varying, 'PENDIENTE_CIERRE_TURNO'::character varying])::text[])))
);


--
-- Name: conciliaciones_asistencia_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.conciliaciones_asistencia_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: conciliaciones_asistencia_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.conciliaciones_asistencia_id_seq OWNED BY public.conciliaciones_asistencia.id;


--
-- Name: estudiante_apoderados; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.estudiante_apoderados (
    id bigint NOT NULL,
    estudiante_id bigint NOT NULL,
    apoderado_id bigint NOT NULL,
    parentesco character varying(30) NOT NULL,
    es_responsable_economico boolean DEFAULT false NOT NULL,
    tiene_custodia boolean DEFAULT true NOT NULL,
    permite_recojo boolean DEFAULT true NOT NULL,
    CONSTRAINT estudiante_apoderados_parentesco_check CHECK (((parentesco)::text = ANY ((ARRAY['PADRE'::character varying, 'MADRE'::character varying, 'TUTOR_LEGAL'::character varying, 'ABUELO_A'::character varying, 'OTRO'::character varying])::text[])))
);


--
-- Name: estudiante_apoderados_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.estudiante_apoderados_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: estudiante_apoderados_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.estudiante_apoderados_id_seq OWNED BY public.estudiante_apoderados.id;


--
-- Name: estudiantes; Type: TABLE; Schema: public; Owner: -
--

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
    estado character varying(20) DEFAULT 'ACTIVO'::character varying NOT NULL,
    CONSTRAINT chk_estudiantes_dni_8_digitos CHECK (((numero_documento)::text ~ '^[0-9]{8}$'::text)),
    CONSTRAINT chk_estudiantes_estado CHECK (((estado)::text = ANY ((ARRAY['ACTIVO'::character varying, 'SUSPENDIDO'::character varying, 'RETIRADO'::character varying, 'EGRESADO'::character varying, 'INACTIVO'::character varying])::text[]))),
    CONSTRAINT chk_estudiantes_salud_longitud CHECK ((char_length(alergias_condiciones) <= 4000)),
    CONSTRAINT estudiantes_genero_check CHECK ((genero = ANY (ARRAY['M'::bpchar, 'F'::bpchar]))),
    CONSTRAINT estudiantes_origen_registro_check CHECK (((origen_registro)::text = ANY ((ARRAY['RENIEC_API'::character varying, 'MANUAL_CONTINGENCIA'::character varying])::text[]))),
    CONSTRAINT estudiantes_tipo_documento_check CHECK (((tipo_documento)::text = 'DNI'::text))
);


--
-- Name: estudiantes_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.estudiantes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: estudiantes_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.estudiantes_id_seq OWNED BY public.estudiantes.id;


--
-- Name: expedientes_empleados_documentos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.expedientes_empleados_documentos (
    id bigint NOT NULL,
    usuario_id bigint NOT NULL,
    tipo_documento character varying(30) NOT NULL,
    nombre_archivo character varying(200) NOT NULL,
    ruta_objeto character varying(255) NOT NULL,
    tipo_contenido character varying(50) DEFAULT 'application/pdf'::character varying NOT NULL,
    tamano_bytes bigint NOT NULL,
    estado_ia character varying(20) DEFAULT 'PENDIENTE'::character varying NOT NULL,
    observaciones_ia text,
    metadatos_ia jsonb,
    fecha_creacion timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    fecha_validacion_ia timestamp with time zone,
    CONSTRAINT expedientes_empleados_documentos_estado_ia_check CHECK (((estado_ia)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'VALIDADO_IA'::character varying, 'OBSERVADO_IA'::character varying])::text[]))),
    CONSTRAINT expedientes_empleados_documentos_tamano_bytes_check CHECK ((tamano_bytes > 0)),
    CONSTRAINT expedientes_empleados_documentos_tipo_documento_check CHECK (((tipo_documento)::text = ANY ((ARRAY['CV'::character varying, 'TITULO_PROFESIONAL'::character varying, 'ANTECEDENTES_POLICIALES'::character varying, 'ANTECEDENTES_PENALES'::character varying, 'OTRO'::character varying])::text[])))
);


--
-- Name: expedientes_empleados_documentos_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.expedientes_empleados_documentos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: expedientes_empleados_documentos_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.expedientes_empleados_documentos_id_seq OWNED BY public.expedientes_empleados_documentos.id;


--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


--
-- Name: grados; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.grados (
    id smallint NOT NULL,
    nivel_id smallint NOT NULL,
    numero_grado smallint NOT NULL,
    nombre_grado character varying(30) NOT NULL,
    CONSTRAINT grados_numero_grado_check CHECK (((numero_grado >= 1) AND (numero_grado <= 6)))
);


--
-- Name: grados_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.grados_id_seq
    AS smallint
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: grados_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.grados_id_seq OWNED BY public.grados.id;


--
-- Name: historial_estados_incidencia; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.historial_estados_incidencia (
    id bigint NOT NULL,
    incidencia_id bigint NOT NULL,
    estado_anterior character varying(20),
    estado_nuevo character varying(20) NOT NULL,
    usuario_id bigint NOT NULL,
    nombre_usuario_snapshot character varying(30) NOT NULL,
    fecha_cambio timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    observacion_gestion character varying(1000) NOT NULL,
    resultado_verificado character varying(300),
    participantes character varying(120),
    comunicacion_apoderado character varying(130),
    CONSTRAINT chk_historial_cierre_comunicacion_no_vacia CHECK (((comunicacion_apoderado IS NULL) OR (length(btrim((comunicacion_apoderado)::text)) > 0))),
    CONSTRAINT chk_historial_cierre_participantes_no_vacios CHECK (((participantes IS NULL) OR (length(btrim((participantes)::text)) > 0))),
    CONSTRAINT chk_historial_cierre_resultado_no_vacio CHECK (((resultado_verificado IS NULL) OR (length(btrim((resultado_verificado)::text)) > 0))),
    CONSTRAINT chk_historial_cierre_resultado_verificado CHECK (((estado_anterior IS NULL) OR ((estado_nuevo)::text <> 'CERRADA'::text) OR (resultado_verificado IS NOT NULL))),
    CONSTRAINT chk_historial_incidencia_estado_anterior CHECK (((estado_anterior IS NULL) OR ((estado_anterior)::text = ANY ((ARRAY['ABIERTA'::character varying, 'ATENDIDA'::character varying, 'CERRADA'::character varying])::text[])))),
    CONSTRAINT chk_historial_incidencia_estado_nuevo CHECK (((estado_nuevo)::text = ANY ((ARRAY['ABIERTA'::character varying, 'ATENDIDA'::character varying, 'CERRADA'::character varying])::text[]))),
    CONSTRAINT chk_historial_incidencia_observacion CHECK ((length(btrim((observacion_gestion)::text)) > 0)),
    CONSTRAINT chk_historial_incidencia_transicion CHECK (((estado_anterior IS NULL) OR ((estado_anterior)::text <> (estado_nuevo)::text)))
);


--
-- Name: historial_estados_incidencia_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.historial_estados_incidencia ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.historial_estados_incidencia_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: horarios_seccion; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.horarios_seccion (
    id bigint NOT NULL,
    anio_lectivo_id smallint NOT NULL,
    seccion_id integer NOT NULL,
    docente_usuario_id bigint NOT NULL,
    asignacion_docente_id bigint NOT NULL,
    dia_semana smallint NOT NULL,
    bloque_horario_id smallint NOT NULL,
    activo boolean DEFAULT true NOT NULL,
    CONSTRAINT horarios_seccion_dia_semana_check CHECK (((dia_semana >= 1) AND (dia_semana <= 5)))
);


--
-- Name: horarios_seccion_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.horarios_seccion_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: horarios_seccion_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.horarios_seccion_id_seq OWNED BY public.horarios_seccion.id;


--
-- Name: incidencias_conductuales; Type: TABLE; Schema: public; Owner: -
--

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
    CONSTRAINT incidencias_conductuales_estado_check CHECK (((estado)::text = ANY ((ARRAY['ABIERTA'::character varying, 'ATENDIDA'::character varying, 'CERRADA'::character varying])::text[]))),
    CONSTRAINT incidencias_conductuales_tipo_falta_check CHECK (((tipo_falta)::text = ANY ((ARRAY['LEVE'::character varying, 'GRAVE'::character varying, 'MUY_GRAVE'::character varying])::text[])))
);


--
-- Name: incidencias_conductuales_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.incidencias_conductuales_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: incidencias_conductuales_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.incidencias_conductuales_id_seq OWNED BY public.incidencias_conductuales.id;


--
-- Name: inscripciones_refuerzo; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.inscripciones_refuerzo (
    id bigint NOT NULL,
    sesion_refuerzo_id bigint NOT NULL,
    estudiante_id bigint NOT NULL,
    calificacion_origen_id bigint,
    estado_asistencia character varying(20) DEFAULT 'PENDIENTE'::character varying NOT NULL,
    observaciones text,
    CONSTRAINT chk_refuerzo_observaciones_longitud CHECK ((char_length(observaciones) <= 500)),
    CONSTRAINT inscripciones_refuerzo_estado_asistencia_check CHECK (((estado_asistencia)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'ASISTIO'::character varying, 'FALTO'::character varying, 'JUSTIFICADO'::character varying])::text[])))
);


--
-- Name: inscripciones_refuerzo_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.inscripciones_refuerzo_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: inscripciones_refuerzo_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.inscripciones_refuerzo_id_seq OWNED BY public.inscripciones_refuerzo.id;


--
-- Name: lotes_biometrico; Type: TABLE; Schema: public; Owner: -
--

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


--
-- Name: lotes_biometrico_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.lotes_biometrico_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: lotes_biometrico_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.lotes_biometrico_id_seq OWNED BY public.lotes_biometrico.id;


--
-- Name: mallas_horarias_grado; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mallas_horarias_grado (
    id bigint NOT NULL,
    anio_lectivo_id smallint NOT NULL,
    grado_id smallint NOT NULL,
    area_curricular_id smallint NOT NULL,
    dia_semana smallint NOT NULL,
    bloque_horario_id smallint NOT NULL,
    activo boolean DEFAULT true NOT NULL,
    CONSTRAINT mallas_horarias_grado_dia_semana_check CHECK (((dia_semana >= 1) AND (dia_semana <= 5)))
);


--
-- Name: mallas_horarias_grado_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.mallas_horarias_grado ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.mallas_horarias_grado_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: marcas_biometrico_porteria; Type: TABLE; Schema: public; Owner: -
--

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
    CONSTRAINT marcas_biometrico_porteria_estado_procesamiento_check CHECK (((estado_procesamiento)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'CONCILIADO'::character varying, 'DNI_NO_IDENTIFICADO'::character varying, 'DUPLICADO'::character varying])::text[])))
);


--
-- Name: marcas_biometrico_porteria_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.marcas_biometrico_porteria_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: marcas_biometrico_porteria_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.marcas_biometrico_porteria_id_seq OWNED BY public.marcas_biometrico_porteria.id;


--
-- Name: matriculas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.matriculas (
    id bigint NOT NULL,
    anio_lectivo_id smallint NOT NULL,
    estudiante_id bigint NOT NULL,
    seccion_id integer,
    fecha_matricula timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    estado_matricula character varying(25) DEFAULT 'SOLICITADA'::character varying NOT NULL,
    fecha_expiracion_reserva timestamp with time zone,
    observaciones text,
    CONSTRAINT chk_matriculas_observaciones_longitud CHECK ((char_length(observaciones) <= 500)),
    CONSTRAINT ck_matricula_pendiente_sin_seccion CHECK ((((estado_matricula)::text <> 'PENDIENTE_ASIGNACION'::text) OR (seccion_id IS NULL))),
    CONSTRAINT ck_matriculado_con_seccion CHECK ((((estado_matricula)::text <> 'MATRICULADO'::text) OR (seccion_id IS NOT NULL))),
    CONSTRAINT matriculas_check CHECK ((((estado_matricula)::text <> 'RESERVADA_TEMPORAL'::text) OR (fecha_expiracion_reserva IS NOT NULL))),
    CONSTRAINT matriculas_estado_matricula_check CHECK (((estado_matricula)::text = ANY ((ARRAY['SOLICITADA'::character varying, 'RESERVADA_TEMPORAL'::character varying, 'PENDIENTE_ASIGNACION'::character varying, 'MATRICULADO'::character varying, 'TRASLADADO'::character varying, 'RETIRADO'::character varying, 'CANCELADA'::character varying])::text[])))
);


--
-- Name: matriculas_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.matriculas_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: matriculas_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.matriculas_id_seq OWNED BY public.matriculas.id;


--
-- Name: niveles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.niveles (
    id smallint NOT NULL,
    codigo_nivel character varying(20) NOT NULL,
    nombre_nivel character varying(30) NOT NULL,
    CONSTRAINT niveles_codigo_check CHECK (((codigo_nivel)::text = ANY ((ARRAY['INICIAL'::character varying, 'PRIMARIA'::character varying, 'SECUNDARIA'::character varying])::text[])))
);


--
-- Name: niveles_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.niveles_id_seq
    AS smallint
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: niveles_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.niveles_id_seq OWNED BY public.niveles.id;


--
-- Name: obligaciones_pago; Type: TABLE; Schema: public; Owner: -
--

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
    CONSTRAINT obligaciones_pago_check1 CHECK (((((tipo_concepto)::text = 'MATRICULA'::text) AND (numero_cuota = 0)) OR (((tipo_concepto)::text = 'PENSION'::text) AND ((numero_cuota >= 1) AND (numero_cuota <= 10))) OR ((tipo_concepto)::text <> ALL ((ARRAY['MATRICULA'::character varying, 'PENSION'::character varying])::text[])))),
    CONSTRAINT obligaciones_pago_estado_check CHECK (((estado)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'PAGADO_TOTAL'::character varying, 'PAGADO_PARCIAL'::character varying, 'VENCIDO'::character varying, 'CANCELADO_POR_TRASLADO'::character varying])::text[]))),
    CONSTRAINT obligaciones_pago_monto_base_check CHECK ((monto_base >= 0.00)),
    CONSTRAINT obligaciones_pago_monto_descuento_check CHECK ((monto_descuento >= 0.00)),
    CONSTRAINT obligaciones_pago_monto_mora_check CHECK ((monto_mora >= 0.00)),
    CONSTRAINT obligaciones_pago_numero_cuota_check CHECK (((numero_cuota >= 0) AND (numero_cuota <= 10))),
    CONSTRAINT obligaciones_pago_total_pagado_check CHECK ((total_pagado >= 0.00))
);


--
-- Name: obligaciones_pago_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.obligaciones_pago_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: obligaciones_pago_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.obligaciones_pago_id_seq OWNED BY public.obligaciones_pago.id;


--
-- Name: pagos_transacciones; Type: TABLE; Schema: public; Owner: -
--

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
    CONSTRAINT pagos_transacciones_estado_pago_check CHECK (((estado_pago)::text = ANY ((ARRAY['APROBADO'::character varying, 'RECHAZADO'::character varying, 'REVERTIDO'::character varying])::text[]))),
    CONSTRAINT pagos_transacciones_metodo_pago_check CHECK (((metodo_pago)::text = ANY ((ARRAY['TARJETA_CREDITO'::character varying, 'TARJETA_DEBITO'::character varying, 'YAPE'::character varying, 'PLIN'::character varying, 'TRANSFERENCIA'::character varying, 'EFECTIVO'::character varying, 'PAGO_EFECTIVO'::character varying])::text[]))),
    CONSTRAINT pagos_transacciones_monto_pagado_check CHECK ((monto_pagado > 0.00)),
    CONSTRAINT pagos_transacciones_pasarela_proveedor_check CHECK (((pasarela_proveedor)::text = ANY ((ARRAY['CULQI'::character varying, 'NIUBIZ'::character varying, 'MERCADO_PAGO'::character varying, 'CAJA_EFECTIVO'::character varying, 'TRANSFERENCIA'::character varying])::text[])))
);


--
-- Name: pagos_transacciones_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.pagos_transacciones_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: pagos_transacciones_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.pagos_transacciones_id_seq OWNED BY public.pagos_transacciones.id;


--
-- Name: periodos_academicos; Type: TABLE; Schema: public; Owner: -
--

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


--
-- Name: periodos_academicos_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.periodos_academicos_id_seq
    AS smallint
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: periodos_academicos_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.periodos_academicos_id_seq OWNED BY public.periodos_academicos.id;


--
-- Name: roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.roles (
    id smallint NOT NULL,
    codigo_rol character varying(20) NOT NULL,
    nombre_rol character varying(30) NOT NULL
);


--
-- Name: roles_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.roles_id_seq
    AS smallint
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: roles_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.roles_id_seq OWNED BY public.roles.id;


--
-- Name: secciones; Type: TABLE; Schema: public; Owner: -
--

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


--
-- Name: secciones_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.secciones_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: secciones_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.secciones_id_seq OWNED BY public.secciones.id;


--
-- Name: series_comprobante; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.series_comprobante (
    serie character varying(4) NOT NULL,
    ultimo_correlativo integer DEFAULT 0 NOT NULL,
    fecha_actualizacion timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT series_comprobante_serie_check CHECK (((serie)::text ~ '^[BE][0-9]{3}$'::text)),
    CONSTRAINT series_comprobante_ultimo_correlativo_check CHECK ((ultimo_correlativo >= 0))
);


--
-- Name: sesiones; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sesiones (
    id bigint NOT NULL,
    usuario_id bigint NOT NULL,
    huella_token character varying(64) NOT NULL,
    direccion_ip inet,
    agente_usuario text,
    fecha_expiracion timestamp with time zone NOT NULL,
    fecha_creacion timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: sesiones_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sesiones_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sesiones_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sesiones_id_seq OWNED BY public.sesiones.id;


--
-- Name: sesiones_refuerzo; Type: TABLE; Schema: public; Owner: -
--

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


--
-- Name: sesiones_refuerzo_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sesiones_refuerzo_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sesiones_refuerzo_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sesiones_refuerzo_id_seq OWNED BY public.sesiones_refuerzo.id;


--
-- Name: solicitudes_matricula_documentos; Type: TABLE; Schema: public; Owner: -
--

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
    CONSTRAINT ck_documento_actual_pdf CHECK (((tipo_contenido)::text = 'application/pdf'::text)),
    CONSTRAINT solicitudes_matricula_documentos_mime_type_check CHECK (((tipo_contenido)::text = ANY (ARRAY[('application/pdf'::character varying)::text, ('image/jpeg'::character varying)::text, ('image/png'::character varying)::text, ('image/webp'::character varying)::text]))),
    CONSTRAINT solicitudes_matricula_documentos_tamano_bytes_check CHECK (((tamano_bytes >= 1) AND (tamano_bytes <= 5242880))),
    CONSTRAINT solicitudes_matricula_documentos_tipo_check CHECK (((tipo)::text = ANY ((ARRAY['PARTIDA_NACIMIENTO'::character varying, 'DNI_C4'::character varying, 'RECIBO_SERVICIO'::character varying])::text[])))
);


--
-- Name: solicitudes_matricula_publica; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.solicitudes_matricula_publica (
    id uuid NOT NULL,
    huella_token character varying(64) NOT NULL,
    estado character varying(50) NOT NULL,
    anio_lectivo_id smallint NOT NULL,
    seccion_id integer,
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
    correo_apoderado character varying(100),
    direccion_apoderado character varying(150) NOT NULL,
    ubigeo_apoderado character varying(6),
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
    requiere_autorizacion_edad boolean DEFAULT false NOT NULL,
    motivo_autorizacion_edad character varying(500),
    autorizado_por_usuario_id bigint,
    fecha_autorizacion_edad timestamp with time zone,
    grado_id smallint NOT NULL,
    CONSTRAINT chk_solicitud_matricula_comprobante_confirmada CHECK ((((estado)::text <> 'MATRICULADA'::text) OR (comprobante_pago_codigo IS NOT NULL))),
    CONSTRAINT chk_solicitud_matricula_documentos_emitidos CHECK (((comprobante_pago_codigo IS NULL) = (fecha_emision_documentos IS NULL))),
    CONSTRAINT chk_solicitudes_matricula_pago_monto_positivo CHECK ((pago_monto > (0)::numeric)),
    CONSTRAINT ck_solicitud_matricula_requiere_estudiante CHECK (((matricula_id IS NULL) OR (estudiante_id IS NOT NULL))),
    CONSTRAINT solicitudes_matricula_publica_autorizacion_edad_check CHECK ((((NOT requiere_autorizacion_edad) AND (motivo_autorizacion_edad IS NULL) AND (autorizado_por_usuario_id IS NULL) AND (fecha_autorizacion_edad IS NULL)) OR (requiere_autorizacion_edad AND (((motivo_autorizacion_edad IS NULL) AND (autorizado_por_usuario_id IS NULL) AND (fecha_autorizacion_edad IS NULL)) OR (((length(btrim((motivo_autorizacion_edad)::text)) >= 10) AND (length(btrim((motivo_autorizacion_edad)::text)) <= 500)) AND (autorizado_por_usuario_id IS NOT NULL) AND (fecha_autorizacion_edad IS NOT NULL)))))),
    CONSTRAINT solicitudes_matricula_publica_celular_apoderado_check CHECK (((celular_apoderado)::text ~ '^9[0-9]{8}$'::text)),
    CONSTRAINT solicitudes_matricula_publica_check CHECK (((NOT vacante_reservada) OR (fecha_expiracion_pago IS NOT NULL))),
    CONSTRAINT solicitudes_matricula_publica_check1 CHECK ((((estado)::text <> 'MATRICULADA'::text) OR ((estudiante_id IS NOT NULL) AND (apoderado_id IS NOT NULL) AND (matricula_id IS NOT NULL) AND (pago_id IS NOT NULL)))),
    CONSTRAINT solicitudes_matricula_publica_consentimiento_gemini_check CHECK ((consentimiento_gemini IS TRUE)),
    CONSTRAINT solicitudes_matricula_publica_estado_check CHECK (((estado)::text = ANY ((ARRAY['DOCUMENTOS_PENDIENTES'::character varying, 'DOCUMENTOS_EN_VALIDACION'::character varying, 'DOCUMENTOS_OBSERVADOS'::character varying, 'DOCUMENTOS_VALIDADOS'::character varying, 'AUTORIZACION_EDAD_PENDIENTE'::character varying, 'PAGO_PENDIENTE'::character varying, 'PAGO_CONFIRMADO_PENDIENTE_ASIGNACION'::character varying, 'MATRICULADA'::character varying, 'RECHAZADA'::character varying])::text[]))),
    CONSTRAINT solicitudes_matricula_publica_estado_dni_c4_check CHECK (((estado_dni_c4)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'EN_VALIDACION'::character varying, 'VALIDADO'::character varying, 'OBSERVADO'::character varying])::text[]))),
    CONSTRAINT solicitudes_matricula_publica_estado_partida_check CHECK (((estado_partida)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'EN_VALIDACION'::character varying, 'VALIDADO'::character varying, 'OBSERVADO'::character varying])::text[]))),
    CONSTRAINT solicitudes_matricula_publica_estado_recibo_check CHECK (((estado_recibo)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'EN_VALIDACION'::character varying, 'VALIDADO'::character varying, 'OBSERVADO'::character varying])::text[]))),
    CONSTRAINT solicitudes_matricula_publica_genero_estudiante_check CHECK ((genero_estudiante = ANY (ARRAY['M'::bpchar, 'F'::bpchar]))),
    CONSTRAINT solicitudes_matricula_publica_numero_documento_apoderado_check CHECK (((numero_documento_apoderado)::text ~ '^[0-9]{8}$'::text)),
    CONSTRAINT solicitudes_matricula_publica_numero_documento_estudiante_check CHECK (((numero_documento_estudiante)::text ~ '^[0-9]{8}$'::text)),
    CONSTRAINT solicitudes_matricula_publica_parentesco_check CHECK (((parentesco)::text = ANY ((ARRAY['PADRE'::character varying, 'MADRE'::character varying, 'TUTOR_LEGAL'::character varying, 'ABUELO_A'::character varying, 'OTRO'::character varying])::text[]))),
    CONSTRAINT solicitudes_matricula_publica_ubigeo_apoderado_check CHECK (((ubigeo_apoderado)::text ~ '^[0-9]{6}$'::text))
);


--
-- Name: usuario_roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.usuario_roles (
    usuario_id bigint NOT NULL,
    rol_id smallint NOT NULL
);


--
-- Name: usuarios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.usuarios (
    id bigint NOT NULL,
    nombre_usuario character varying(30) NOT NULL,
    correo_electronico character varying(100) NOT NULL,
    usuario_password character varying(60) NOT NULL,
    activo boolean DEFAULT true NOT NULL,
    fecha_creacion timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    fecha_actualizacion timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    debe_cambiar_password boolean DEFAULT false NOT NULL,
    dni character varying(8),
    telefono character varying(15),
    direccion character varying(150),
    especialidad character varying(100),
    codigo_modular character varying(20),
    numero_colegiatura character varying(20),
    fecha_ingreso date,
    tipo_contrato character varying(30),
    estado_laboral character varying(20) DEFAULT 'ACTIVO'::character varying NOT NULL,
    CONSTRAINT chk_usuarios_estado_laboral CHECK (((estado_laboral)::text = ANY ((ARRAY['ACTIVO'::character varying, 'SUSPENDIDO'::character varying, 'CESADO'::character varying, 'INACTIVO'::character varying])::text[])))
);


--
-- Name: usuarios_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.usuarios_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: usuarios_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.usuarios_id_seq OWNED BY public.usuarios.id;


--
-- Name: v_ranking_merito_cneb; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.v_ranking_merito_cneb AS
 WITH calif AS (
         SELECT calificaciones_cneb.matricula_id,
            (count(*))::integer AS total_calificaciones,
            round(avg(
                CASE calificaciones_cneb.calificacion_cualitativa
                    WHEN 'AD'::text THEN 4.0
                    WHEN 'A'::text THEN 3.0
                    WHEN 'B'::text THEN 2.0
                    WHEN 'C'::text THEN 1.0
                    ELSE 0.0
                END), 2) AS promedio_cneb,
            (count(*) FILTER (WHERE ((calificaciones_cneb.calificacion_cualitativa)::text = 'AD'::text)))::integer AS cant_ad,
            (count(*) FILTER (WHERE ((calificaciones_cneb.calificacion_cualitativa)::text = 'A'::text)))::integer AS cant_a,
            (count(*) FILTER (WHERE ((calificaciones_cneb.calificacion_cualitativa)::text = 'B'::text)))::integer AS cant_b,
            (count(*) FILTER (WHERE ((calificaciones_cneb.calificacion_cualitativa)::text = 'C'::text)))::integer AS cant_c
           FROM public.calificaciones_cneb
          GROUP BY calificaciones_cneb.matricula_id
        ), asist AS (
         SELECT asistencias_aula.matricula_id,
            (count(*))::integer AS total_asistencias,
            (count(*) FILTER (WHERE ((asistencias_aula.estado)::text = 'PRESENTE'::text)))::integer AS total_presente,
            (count(*) FILTER (WHERE ((asistencias_aula.estado)::text = 'TARDANZA'::text)))::integer AS total_tardanza,
            (count(*) FILTER (WHERE ((asistencias_aula.estado)::text = 'FALTA_INJUSTIFICADA'::text)))::integer AS total_falta_injustificada,
            (count(*) FILTER (WHERE ((asistencias_aula.estado)::text = 'FALTA_JUSTIFICADA'::text)))::integer AS total_falta_justificada,
            round((((count(*) FILTER (WHERE ((asistencias_aula.estado)::text = ANY ((ARRAY['PRESENTE'::character varying, 'TARDANZA'::character varying, 'FALTA_JUSTIFICADA'::character varying])::text[]))))::numeric / (NULLIF(count(*), 0))::numeric) * 100.0), 2) AS porcentaje_asistencia
           FROM public.asistencias_aula
          GROUP BY asistencias_aula.matricula_id
        ), incid AS (
         SELECT incidencias_conductuales.matricula_id,
            (count(*))::integer AS total_incidencias,
            (count(*) FILTER (WHERE ((incidencias_conductuales.tipo_falta)::text = 'LEVE'::text)))::integer AS incidencias_leves,
            (count(*) FILTER (WHERE ((incidencias_conductuales.tipo_falta)::text = 'GRAVE'::text)))::integer AS incidencias_graves,
            (count(*) FILTER (WHERE ((incidencias_conductuales.tipo_falta)::text = 'MUY_GRAVE'::text)))::integer AS incidencias_muy_graves,
            (count(*) FILTER (WHERE ((incidencias_conductuales.estado)::text = 'ABIERTA'::text)))::integer AS incidencias_abiertas
           FROM public.incidencias_conductuales
          GROUP BY incidencias_conductuales.matricula_id
        )
 SELECT m.id AS matricula_id,
    m.anio_lectivo_id,
    sec.grado_id,
    g.numero_grado,
    niv.codigo_nivel,
    m.seccion_id,
    sec.letra AS letra_seccion,
    m.estudiante_id,
    e.tipo_documento,
    e.numero_documento,
    e.nombres_estudiante,
    e.apellido_paterno_estudiante,
    e.apellido_materno_estudiante,
    m.estado_matricula,
    COALESCE(c.total_calificaciones, 0) AS total_calificaciones,
    COALESCE(c.promedio_cneb, 0.0) AS promedio_cneb,
    COALESCE(c.cant_ad, 0) AS cant_ad,
    COALESCE(c.cant_a, 0) AS cant_a,
    COALESCE(c.cant_b, 0) AS cant_b,
    COALESCE(c.cant_c, 0) AS cant_c,
    COALESCE(a.total_asistencias, 0) AS total_asistencias,
    COALESCE(a.total_presente, 0) AS total_presente,
    COALESCE(a.total_tardanza, 0) AS total_tardanza,
    COALESCE(a.total_falta_injustificada, 0) AS total_falta_injustificada,
    COALESCE(a.total_falta_justificada, 0) AS total_falta_justificada,
    COALESCE(a.porcentaje_asistencia, 100.0) AS porcentaje_asistencia,
    COALESCE(i.total_incidencias, 0) AS total_incidencias,
    COALESCE(i.incidencias_leves, 0) AS incidencias_leves,
    COALESCE(i.incidencias_graves, 0) AS incidencias_graves,
    COALESCE(i.incidencias_muy_graves, 0) AS incidencias_muy_graves,
    COALESCE(i.incidencias_abiertas, 0) AS incidencias_abiertas,
    dense_rank() OVER (PARTITION BY m.anio_lectivo_id, sec.grado_id ORDER BY COALESCE(c.promedio_cneb, 0.0) DESC, COALESCE(c.cant_ad, 0) DESC, COALESCE(c.cant_a, 0) DESC) AS puesto_grado,
    dense_rank() OVER (PARTITION BY m.anio_lectivo_id, m.seccion_id ORDER BY COALESCE(c.promedio_cneb, 0.0) DESC, COALESCE(c.cant_ad, 0) DESC, COALESCE(c.cant_a, 0) DESC) AS puesto_seccion
   FROM (((((((public.matriculas m
     JOIN public.estudiantes e ON ((e.id = m.estudiante_id)))
     LEFT JOIN public.secciones sec ON ((sec.id = m.seccion_id)))
     LEFT JOIN public.grados g ON ((g.id = sec.grado_id)))
     LEFT JOIN public.niveles niv ON ((niv.id = g.nivel_id)))
     LEFT JOIN calif c ON ((c.matricula_id = m.id)))
     LEFT JOIN asist a ON ((a.matricula_id = m.id)))
     LEFT JOIN incid i ON ((i.matricula_id = m.id)));


--
-- Name: versiones_documentos_matricula; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.versiones_documentos_matricula (
    id uuid NOT NULL,
    solicitud_id uuid NOT NULL,
    tipo character varying(30) NOT NULL,
    contenedor character varying(100) NOT NULL,
    ruta_objeto character varying(500) NOT NULL,
    tipo_contenido character varying(30) NOT NULL,
    tamano_bytes bigint NOT NULL,
    huella_sha256 character(64) NOT NULL,
    ia_valida boolean,
    dni_api_coincide boolean,
    observacion_ia character varying(500),
    estado_firma character varying(20) NOT NULL,
    detalle_firma character varying(500) NOT NULL,
    certificado_sha256 character(64),
    fecha_carga timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    vinculo_parental_coincide boolean,
    CONSTRAINT ck_version_documental_pdf CHECK (((tipo_contenido)::text = 'application/pdf'::text)),
    CONSTRAINT ck_version_vinculo_parental CHECK (((vinculo_parental_coincide IS NULL) OR ((tipo)::text = 'PARTIDA_NACIMIENTO'::text))),
    CONSTRAINT versiones_documentos_matricula_certificado_sha256_check CHECK (((certificado_sha256 IS NULL) OR (certificado_sha256 ~ '^[0-9a-f]{64}$'::text))),
    CONSTRAINT versiones_documentos_matricula_estado_firma_check CHECK (((estado_firma)::text = ANY ((ARRAY['VALIDA'::character varying, 'INVALIDA'::character varying, 'NO_APLICA'::character varying, 'NO_VERIFICABLE'::character varying, 'ERROR_SERVICIO'::character varying])::text[]))),
    CONSTRAINT versiones_documentos_matricula_huella_sha256_check CHECK ((huella_sha256 ~ '^[0-9a-f]{64}$'::text)),
    CONSTRAINT versiones_documentos_matricula_tamano_bytes_check CHECK (((tamano_bytes > 0) AND (tamano_bytes <= 5242880))),
    CONSTRAINT versiones_documentos_matricula_tipo_check CHECK (((tipo)::text = ANY ((ARRAY['PARTIDA_NACIMIENTO'::character varying, 'DNI_C4'::character varying, 'RECIBO_SERVICIO'::character varying])::text[]))),
    CONSTRAINT versiones_documentos_matricula_tipo_contenido_check CHECK (((tipo_contenido)::text = ANY ((ARRAY['application/pdf'::character varying, 'image/jpeg'::character varying, 'image/png'::character varying, 'image/webp'::character varying])::text[])))
);


--
-- Name: anios_lectivos id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.anios_lectivos ALTER COLUMN id SET DEFAULT nextval('public.anios_lectivos_id_seq'::regclass);


--
-- Name: apoderados id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.apoderados ALTER COLUMN id SET DEFAULT nextval('public.apoderados_id_seq'::regclass);


--
-- Name: areas_curriculares id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.areas_curriculares ALTER COLUMN id SET DEFAULT nextval('public.areas_curriculares_id_seq'::regclass);


--
-- Name: asignaciones_docentes id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asignaciones_docentes ALTER COLUMN id SET DEFAULT nextval('public.asignaciones_docentes_id_seq'::regclass);


--
-- Name: asistencias_aula id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asistencias_aula ALTER COLUMN id SET DEFAULT nextval('public.asistencias_aula_id_seq'::regclass);


--
-- Name: auditoria_cambios id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auditoria_cambios ALTER COLUMN id SET DEFAULT nextval('public.auditoria_cambios_id_seq'::regclass);


--
-- Name: aulas id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aulas ALTER COLUMN id SET DEFAULT nextval('public.aulas_id_seq'::regclass);


--
-- Name: bloques_horarios id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bloques_horarios ALTER COLUMN id SET DEFAULT nextval('public.bloques_horarios_id_seq'::regclass);


--
-- Name: calificaciones_cneb id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.calificaciones_cneb ALTER COLUMN id SET DEFAULT nextval('public.calificaciones_cneb_id_seq'::regclass);


--
-- Name: competencias id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.competencias ALTER COLUMN id SET DEFAULT nextval('public.competencias_id_seq'::regclass);


--
-- Name: comprobantes_pago id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comprobantes_pago ALTER COLUMN id SET DEFAULT nextval('public.comprobantes_pago_id_seq'::regclass);


--
-- Name: comunicado_destinatarios id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comunicado_destinatarios ALTER COLUMN id SET DEFAULT nextval('public.comunicado_destinatarios_id_seq'::regclass);


--
-- Name: comunicados_oficiales id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comunicados_oficiales ALTER COLUMN id SET DEFAULT nextval('public.comunicados_oficiales_id_seq'::regclass);


--
-- Name: conceptos_cobro id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.conceptos_cobro ALTER COLUMN id SET DEFAULT nextval('public.conceptos_cobro_id_seq'::regclass);


--
-- Name: conciliaciones_asistencia id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.conciliaciones_asistencia ALTER COLUMN id SET DEFAULT nextval('public.conciliaciones_asistencia_id_seq'::regclass);


--
-- Name: estudiante_apoderados id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estudiante_apoderados ALTER COLUMN id SET DEFAULT nextval('public.estudiante_apoderados_id_seq'::regclass);


--
-- Name: estudiantes id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estudiantes ALTER COLUMN id SET DEFAULT nextval('public.estudiantes_id_seq'::regclass);


--
-- Name: expedientes_empleados_documentos id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expedientes_empleados_documentos ALTER COLUMN id SET DEFAULT nextval('public.expedientes_empleados_documentos_id_seq'::regclass);


--
-- Name: grados id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grados ALTER COLUMN id SET DEFAULT nextval('public.grados_id_seq'::regclass);


--
-- Name: horarios_seccion id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.horarios_seccion ALTER COLUMN id SET DEFAULT nextval('public.horarios_seccion_id_seq'::regclass);


--
-- Name: incidencias_conductuales id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.incidencias_conductuales ALTER COLUMN id SET DEFAULT nextval('public.incidencias_conductuales_id_seq'::regclass);


--
-- Name: inscripciones_refuerzo id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inscripciones_refuerzo ALTER COLUMN id SET DEFAULT nextval('public.inscripciones_refuerzo_id_seq'::regclass);


--
-- Name: lotes_biometrico id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lotes_biometrico ALTER COLUMN id SET DEFAULT nextval('public.lotes_biometrico_id_seq'::regclass);


--
-- Name: marcas_biometrico_porteria id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.marcas_biometrico_porteria ALTER COLUMN id SET DEFAULT nextval('public.marcas_biometrico_porteria_id_seq'::regclass);


--
-- Name: matriculas id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matriculas ALTER COLUMN id SET DEFAULT nextval('public.matriculas_id_seq'::regclass);


--
-- Name: niveles id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.niveles ALTER COLUMN id SET DEFAULT nextval('public.niveles_id_seq'::regclass);


--
-- Name: obligaciones_pago id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.obligaciones_pago ALTER COLUMN id SET DEFAULT nextval('public.obligaciones_pago_id_seq'::regclass);


--
-- Name: pagos_transacciones id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pagos_transacciones ALTER COLUMN id SET DEFAULT nextval('public.pagos_transacciones_id_seq'::regclass);


--
-- Name: periodos_academicos id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.periodos_academicos ALTER COLUMN id SET DEFAULT nextval('public.periodos_academicos_id_seq'::regclass);


--
-- Name: roles id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles ALTER COLUMN id SET DEFAULT nextval('public.roles_id_seq'::regclass);


--
-- Name: secciones id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.secciones ALTER COLUMN id SET DEFAULT nextval('public.secciones_id_seq'::regclass);


--
-- Name: sesiones id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sesiones ALTER COLUMN id SET DEFAULT nextval('public.sesiones_id_seq'::regclass);


--
-- Name: sesiones_refuerzo id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sesiones_refuerzo ALTER COLUMN id SET DEFAULT nextval('public.sesiones_refuerzo_id_seq'::regclass);


--
-- Name: usuarios id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios ALTER COLUMN id SET DEFAULT nextval('public.usuarios_id_seq'::regclass);


--
-- Name: anios_lectivos anios_lectivos_anio_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.anios_lectivos
    ADD CONSTRAINT anios_lectivos_anio_key UNIQUE (anio);


--
-- Name: anios_lectivos anios_lectivos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.anios_lectivos
    ADD CONSTRAINT anios_lectivos_pkey PRIMARY KEY (id);


--
-- Name: apoderados apoderados_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.apoderados
    ADD CONSTRAINT apoderados_pkey PRIMARY KEY (id);


--
-- Name: apoderados_suplentes apoderados_suplentes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.apoderados_suplentes
    ADD CONSTRAINT apoderados_suplentes_pkey PRIMARY KEY (id);


--
-- Name: apoderados apoderados_tipo_documento_numero_documento_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.apoderados
    ADD CONSTRAINT apoderados_tipo_documento_numero_documento_key UNIQUE (tipo_documento, numero_documento);


--
-- Name: apoderados apoderados_usuario_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.apoderados
    ADD CONSTRAINT apoderados_usuario_id_key UNIQUE (usuario_id);


--
-- Name: areas_curriculares areas_curriculares_id_nivel_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.areas_curriculares
    ADD CONSTRAINT areas_curriculares_id_nivel_id_key UNIQUE (id, nivel_id);


--
-- Name: areas_curriculares areas_curriculares_nivel_id_codigo_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.areas_curriculares
    ADD CONSTRAINT areas_curriculares_nivel_id_codigo_key UNIQUE (nivel_id, codigo_area);


--
-- Name: areas_curriculares areas_curriculares_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.areas_curriculares
    ADD CONSTRAINT areas_curriculares_pkey PRIMARY KEY (id);


--
-- Name: asignaciones_docentes asignaciones_docentes_id_anio_lectivo_id_seccion_id_docente_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asignaciones_docentes
    ADD CONSTRAINT asignaciones_docentes_id_anio_lectivo_id_seccion_id_docente_key UNIQUE (id, anio_lectivo_id, seccion_id, docente_usuario_id);


--
-- Name: asignaciones_docentes asignaciones_docentes_id_seccion_id_area_curricular_id_anio_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asignaciones_docentes
    ADD CONSTRAINT asignaciones_docentes_id_seccion_id_area_curricular_id_anio_key UNIQUE (id, seccion_id, area_curricular_id, anio_lectivo_id, docente_usuario_id);


--
-- Name: asignaciones_docentes asignaciones_docentes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asignaciones_docentes
    ADD CONSTRAINT asignaciones_docentes_pkey PRIMARY KEY (id);


--
-- Name: asignaciones_docentes asignaciones_docentes_seccion_id_area_curricular_id_anio_le_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asignaciones_docentes
    ADD CONSTRAINT asignaciones_docentes_seccion_id_area_curricular_id_anio_le_key UNIQUE (seccion_id, area_curricular_id, anio_lectivo_id);


--
-- Name: asistencias_aula asistencias_aula_matricula_id_fecha_sesion_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asistencias_aula
    ADD CONSTRAINT asistencias_aula_matricula_id_fecha_sesion_key UNIQUE (matricula_id, fecha_sesion);


--
-- Name: asistencias_aula asistencias_aula_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asistencias_aula
    ADD CONSTRAINT asistencias_aula_pkey PRIMARY KEY (id);


--
-- Name: auditoria_accesos_api auditoria_accesos_api_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auditoria_accesos_api
    ADD CONSTRAINT auditoria_accesos_api_pkey PRIMARY KEY (id);


--
-- Name: auditoria_cambios auditoria_cambios_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auditoria_cambios
    ADD CONSTRAINT auditoria_cambios_pkey PRIMARY KEY (id);


--
-- Name: aulas aulas_codigo_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aulas
    ADD CONSTRAINT aulas_codigo_key UNIQUE (codigo_aula);


--
-- Name: aulas aulas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aulas
    ADD CONSTRAINT aulas_pkey PRIMARY KEY (id);


--
-- Name: bloques_horarios bloques_horarios_numero_bloque_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bloques_horarios
    ADD CONSTRAINT bloques_horarios_numero_bloque_key UNIQUE (numero_bloque);


--
-- Name: bloques_horarios bloques_horarios_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bloques_horarios
    ADD CONSTRAINT bloques_horarios_pkey PRIMARY KEY (id);


--
-- Name: calificaciones_cneb calificaciones_cneb_matricula_id_competencia_id_periodo_aca_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.calificaciones_cneb
    ADD CONSTRAINT calificaciones_cneb_matricula_id_competencia_id_periodo_aca_key UNIQUE (matricula_id, competencia_id, periodo_academico_id);


--
-- Name: calificaciones_cneb calificaciones_cneb_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.calificaciones_cneb
    ADD CONSTRAINT calificaciones_cneb_pkey PRIMARY KEY (id);


--
-- Name: competencias competencias_id_area_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.competencias
    ADD CONSTRAINT competencias_id_area_id_key UNIQUE (id, area_id);


--
-- Name: competencias competencias_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.competencias
    ADD CONSTRAINT competencias_pkey PRIMARY KEY (id);


--
-- Name: comprobantes_pago comprobantes_pago_pago_transaccion_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comprobantes_pago
    ADD CONSTRAINT comprobantes_pago_pago_transaccion_id_key UNIQUE (pago_transaccion_id);


--
-- Name: comprobantes_pago comprobantes_pago_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comprobantes_pago
    ADD CONSTRAINT comprobantes_pago_pkey PRIMARY KEY (id);


--
-- Name: comprobantes_pago comprobantes_pago_serie_correlativo_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comprobantes_pago
    ADD CONSTRAINT comprobantes_pago_serie_correlativo_key UNIQUE (serie, correlativo);


--
-- Name: comunicado_destinatarios comunicado_destinatarios_comunicado_id_apoderado_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comunicado_destinatarios
    ADD CONSTRAINT comunicado_destinatarios_comunicado_id_apoderado_id_key UNIQUE (comunicado_id, apoderado_id);


--
-- Name: comunicado_destinatarios comunicado_destinatarios_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comunicado_destinatarios
    ADD CONSTRAINT comunicado_destinatarios_pkey PRIMARY KEY (id);


--
-- Name: comunicados_oficiales comunicados_oficiales_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comunicados_oficiales
    ADD CONSTRAINT comunicados_oficiales_pkey PRIMARY KEY (id);


--
-- Name: conceptos_cobro conceptos_cobro_codigo_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.conceptos_cobro
    ADD CONSTRAINT conceptos_cobro_codigo_key UNIQUE (codigo_concepto);


--
-- Name: conceptos_cobro conceptos_cobro_id_tipo_concepto_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.conceptos_cobro
    ADD CONSTRAINT conceptos_cobro_id_tipo_concepto_key UNIQUE (id, tipo_concepto);


--
-- Name: conceptos_cobro conceptos_cobro_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.conceptos_cobro
    ADD CONSTRAINT conceptos_cobro_pkey PRIMARY KEY (id);


--
-- Name: conciliaciones_asistencia conciliaciones_asistencia_fecha_estudiante_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.conciliaciones_asistencia
    ADD CONSTRAINT conciliaciones_asistencia_fecha_estudiante_id_key UNIQUE (fecha, estudiante_id);


--
-- Name: conciliaciones_asistencia conciliaciones_asistencia_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.conciliaciones_asistencia
    ADD CONSTRAINT conciliaciones_asistencia_pkey PRIMARY KEY (id);


--
-- Name: estudiante_apoderados estudiante_apoderados_estudiante_id_apoderado_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estudiante_apoderados
    ADD CONSTRAINT estudiante_apoderados_estudiante_id_apoderado_id_key UNIQUE (estudiante_id, apoderado_id);


--
-- Name: estudiante_apoderados estudiante_apoderados_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estudiante_apoderados
    ADD CONSTRAINT estudiante_apoderados_pkey PRIMARY KEY (id);


--
-- Name: estudiantes estudiantes_codigo_estudiante_siagie_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estudiantes
    ADD CONSTRAINT estudiantes_codigo_estudiante_siagie_key UNIQUE (codigo_estudiante_siagie);


--
-- Name: estudiantes estudiantes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estudiantes
    ADD CONSTRAINT estudiantes_pkey PRIMARY KEY (id);


--
-- Name: estudiantes estudiantes_tipo_documento_numero_documento_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estudiantes
    ADD CONSTRAINT estudiantes_tipo_documento_numero_documento_key UNIQUE (tipo_documento, numero_documento);


--
-- Name: expedientes_empleados_documentos expedientes_empleados_documentos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expedientes_empleados_documentos
    ADD CONSTRAINT expedientes_empleados_documentos_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: grados grados_id_nivel_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grados
    ADD CONSTRAINT grados_id_nivel_id_key UNIQUE (id, nivel_id);


--
-- Name: grados grados_nivel_id_numero_grado_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grados
    ADD CONSTRAINT grados_nivel_id_numero_grado_key UNIQUE (nivel_id, numero_grado);


--
-- Name: grados grados_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grados
    ADD CONSTRAINT grados_pkey PRIMARY KEY (id);


--
-- Name: historial_estados_incidencia historial_estados_incidencia_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.historial_estados_incidencia
    ADD CONSTRAINT historial_estados_incidencia_pkey PRIMARY KEY (id);


--
-- Name: horarios_seccion horarios_seccion_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.horarios_seccion
    ADD CONSTRAINT horarios_seccion_pkey PRIMARY KEY (id);


--
-- Name: incidencias_conductuales incidencias_conductuales_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.incidencias_conductuales
    ADD CONSTRAINT incidencias_conductuales_pkey PRIMARY KEY (id);


--
-- Name: inscripciones_refuerzo inscripciones_refuerzo_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inscripciones_refuerzo
    ADD CONSTRAINT inscripciones_refuerzo_pkey PRIMARY KEY (id);


--
-- Name: inscripciones_refuerzo inscripciones_refuerzo_sesion_refuerzo_id_estudiante_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inscripciones_refuerzo
    ADD CONSTRAINT inscripciones_refuerzo_sesion_refuerzo_id_estudiante_id_key UNIQUE (sesion_refuerzo_id, estudiante_id);


--
-- Name: lotes_biometrico lotes_biometrico_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lotes_biometrico
    ADD CONSTRAINT lotes_biometrico_pkey PRIMARY KEY (id);


--
-- Name: mallas_horarias_grado mallas_horarias_grado_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mallas_horarias_grado
    ADD CONSTRAINT mallas_horarias_grado_pkey PRIMARY KEY (id);


--
-- Name: marcas_biometrico_porteria marcas_biometrico_porteria_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.marcas_biometrico_porteria
    ADD CONSTRAINT marcas_biometrico_porteria_pkey PRIMARY KEY (id);


--
-- Name: matriculas matriculas_anio_lectivo_id_estudiante_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matriculas
    ADD CONSTRAINT matriculas_anio_lectivo_id_estudiante_id_key UNIQUE (anio_lectivo_id, estudiante_id);


--
-- Name: matriculas matriculas_id_anio_lectivo_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matriculas
    ADD CONSTRAINT matriculas_id_anio_lectivo_id_key UNIQUE (id, anio_lectivo_id);


--
-- Name: matriculas matriculas_id_anio_lectivo_id_seccion_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matriculas
    ADD CONSTRAINT matriculas_id_anio_lectivo_id_seccion_id_key UNIQUE (id, anio_lectivo_id, seccion_id);


--
-- Name: matriculas matriculas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matriculas
    ADD CONSTRAINT matriculas_pkey PRIMARY KEY (id);


--
-- Name: niveles niveles_codigo_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.niveles
    ADD CONSTRAINT niveles_codigo_key UNIQUE (codigo_nivel);


--
-- Name: niveles niveles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.niveles
    ADD CONSTRAINT niveles_pkey PRIMARY KEY (id);


--
-- Name: obligaciones_pago obligaciones_pago_matricula_id_concepto_id_numero_cuota_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.obligaciones_pago
    ADD CONSTRAINT obligaciones_pago_matricula_id_concepto_id_numero_cuota_key UNIQUE (matricula_id, concepto_id, numero_cuota);


--
-- Name: obligaciones_pago obligaciones_pago_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.obligaciones_pago
    ADD CONSTRAINT obligaciones_pago_pkey PRIMARY KEY (id);


--
-- Name: pagos_transacciones pagos_transacciones_pasarela_transaccion_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pagos_transacciones
    ADD CONSTRAINT pagos_transacciones_pasarela_transaccion_id_key UNIQUE (pasarela_transaccion_id);


--
-- Name: pagos_transacciones pagos_transacciones_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pagos_transacciones
    ADD CONSTRAINT pagos_transacciones_pkey PRIMARY KEY (id);


--
-- Name: periodos_academicos periodos_academicos_anio_lectivo_id_numero_periodo_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.periodos_academicos
    ADD CONSTRAINT periodos_academicos_anio_lectivo_id_numero_periodo_key UNIQUE (anio_lectivo_id, numero_periodo);


--
-- Name: periodos_academicos periodos_academicos_id_anio_lectivo_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.periodos_academicos
    ADD CONSTRAINT periodos_academicos_id_anio_lectivo_id_key UNIQUE (id, anio_lectivo_id);


--
-- Name: periodos_academicos periodos_academicos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.periodos_academicos
    ADD CONSTRAINT periodos_academicos_pkey PRIMARY KEY (id);


--
-- Name: roles roles_codigo_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_codigo_key UNIQUE (codigo_rol);


--
-- Name: roles roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id);


--
-- Name: secciones secciones_anio_lectivo_id_grado_id_letra_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.secciones
    ADD CONSTRAINT secciones_anio_lectivo_id_grado_id_letra_key UNIQUE (anio_lectivo_id, grado_id, letra);


--
-- Name: secciones secciones_id_anio_lectivo_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.secciones
    ADD CONSTRAINT secciones_id_anio_lectivo_id_key UNIQUE (id, anio_lectivo_id);


--
-- Name: secciones secciones_id_anio_lectivo_id_nivel_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.secciones
    ADD CONSTRAINT secciones_id_anio_lectivo_id_nivel_id_key UNIQUE (id, anio_lectivo_id, nivel_id);


--
-- Name: secciones secciones_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.secciones
    ADD CONSTRAINT secciones_pkey PRIMARY KEY (id);


--
-- Name: series_comprobante series_comprobante_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.series_comprobante
    ADD CONSTRAINT series_comprobante_pkey PRIMARY KEY (serie);


--
-- Name: sesiones sesiones_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sesiones
    ADD CONSTRAINT sesiones_pkey PRIMARY KEY (id);


--
-- Name: sesiones_refuerzo sesiones_refuerzo_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sesiones_refuerzo
    ADD CONSTRAINT sesiones_refuerzo_pkey PRIMARY KEY (id);


--
-- Name: solicitudes_matricula_documentos solicitudes_matricula_documentos_bucket_object_key_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.solicitudes_matricula_documentos
    ADD CONSTRAINT solicitudes_matricula_documentos_bucket_object_key_key UNIQUE (contenedor, ruta_objeto);


--
-- Name: solicitudes_matricula_documentos solicitudes_matricula_documentos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.solicitudes_matricula_documentos
    ADD CONSTRAINT solicitudes_matricula_documentos_pkey PRIMARY KEY (solicitud_id, tipo);


--
-- Name: solicitudes_matricula_publica solicitudes_matricula_publica_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.solicitudes_matricula_publica
    ADD CONSTRAINT solicitudes_matricula_publica_pkey PRIMARY KEY (id);


--
-- Name: solicitudes_matricula_publica solicitudes_matricula_publica_token_hash_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.solicitudes_matricula_publica
    ADD CONSTRAINT solicitudes_matricula_publica_token_hash_key UNIQUE (huella_token);


--
-- Name: auditoria_accesos_api uq_auditoria_accesos_solicitud; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auditoria_accesos_api
    ADD CONSTRAINT uq_auditoria_accesos_solicitud UNIQUE (solicitud_id);


--
-- Name: lotes_biometrico uq_lotes_biometrico_hash_contenido; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lotes_biometrico
    ADD CONSTRAINT uq_lotes_biometrico_hash_contenido UNIQUE (huella_contenido);


--
-- Name: matriculas uq_matriculas_id_estudiante_anio; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matriculas
    ADD CONSTRAINT uq_matriculas_id_estudiante_anio UNIQUE (id, estudiante_id, anio_lectivo_id);


--
-- Name: secciones uq_secciones_id_anio_grado; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.secciones
    ADD CONSTRAINT uq_secciones_id_anio_grado UNIQUE (id, anio_lectivo_id, grado_id);


--
-- Name: usuario_roles usuario_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuario_roles
    ADD CONSTRAINT usuario_roles_pkey PRIMARY KEY (usuario_id, rol_id);


--
-- Name: usuarios usuarios_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT usuarios_email_key UNIQUE (correo_electronico);


--
-- Name: usuarios usuarios_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT usuarios_pkey PRIMARY KEY (id);


--
-- Name: usuarios usuarios_username_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT usuarios_username_key UNIQUE (nombre_usuario);


--
-- Name: versiones_documentos_matricula versiones_documentos_matricula_id_solicitud_id_tipo_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.versiones_documentos_matricula
    ADD CONSTRAINT versiones_documentos_matricula_id_solicitud_id_tipo_key UNIQUE (id, solicitud_id, tipo);


--
-- Name: versiones_documentos_matricula versiones_documentos_matricula_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.versiones_documentos_matricula
    ADD CONSTRAINT versiones_documentos_matricula_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_apoderados_activo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_apoderados_activo ON public.apoderados USING btree (activo);


--
-- Name: idx_apoderados_estado; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_apoderados_estado ON public.apoderados USING btree (estado);


--
-- Name: idx_asignaciones_area_nivel; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_asignaciones_area_nivel ON public.asignaciones_docentes USING btree (area_curricular_id, nivel_id);


--
-- Name: idx_asignaciones_docente; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_asignaciones_docente ON public.asignaciones_docentes USING btree (docente_usuario_id);


--
-- Name: idx_asignaciones_seccion; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_asignaciones_seccion ON public.asignaciones_docentes USING btree (seccion_id);


--
-- Name: idx_asignaciones_seccion_anio_nivel_fk; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_asignaciones_seccion_anio_nivel_fk ON public.asignaciones_docentes USING btree (seccion_id, anio_lectivo_id, nivel_id);


--
-- Name: idx_asistencias_aula_auxiliar_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_asistencias_aula_auxiliar_id ON public.asistencias_aula USING btree (auxiliar_usuario_id);


--
-- Name: idx_asistencias_aula_matricula_estado; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_asistencias_aula_matricula_estado ON public.asistencias_aula USING btree (matricula_id, estado);


--
-- Name: idx_auditoria_accesos_estado_fecha; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_auditoria_accesos_estado_fecha ON public.auditoria_accesos_api USING btree (estado_http, fecha_evento DESC);


--
-- Name: idx_auditoria_accesos_fecha; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_auditoria_accesos_fecha ON public.auditoria_accesos_api USING btree (fecha_evento DESC);


--
-- Name: idx_auditoria_accesos_usuario_fecha; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_auditoria_accesos_usuario_fecha ON public.auditoria_accesos_api USING btree (usuario_id, fecha_evento DESC);


--
-- Name: idx_auditoria_cambios_fecha_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_auditoria_cambios_fecha_id ON public.auditoria_cambios USING btree (fecha_creacion DESC, id DESC);


--
-- Name: idx_auditoria_cambios_usuario_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_auditoria_cambios_usuario_id ON public.auditoria_cambios USING btree (usuario_id);


--
-- Name: idx_calificaciones_asignacion_contexto_fk; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_calificaciones_asignacion_contexto_fk ON public.calificaciones_cneb USING btree (asignacion_docente_id, seccion_id, area_curricular_id, anio_lectivo_id, docente_usuario_id);


--
-- Name: idx_calificaciones_asignacion_periodo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_calificaciones_asignacion_periodo ON public.calificaciones_cneb USING btree (asignacion_docente_id, periodo_academico_id);


--
-- Name: idx_calificaciones_cneb_matricula_cualitativa; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_calificaciones_cneb_matricula_cualitativa ON public.calificaciones_cneb USING btree (matricula_id, calificacion_cualitativa);


--
-- Name: idx_calificaciones_competencia; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_calificaciones_competencia ON public.calificaciones_cneb USING btree (competencia_id);


--
-- Name: idx_calificaciones_competencia_area_fk; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_calificaciones_competencia_area_fk ON public.calificaciones_cneb USING btree (competencia_id, area_curricular_id);


--
-- Name: idx_calificaciones_docente; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_calificaciones_docente ON public.calificaciones_cneb USING btree (docente_usuario_id);


--
-- Name: idx_calificaciones_matricula_contexto_fk; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_calificaciones_matricula_contexto_fk ON public.calificaciones_cneb USING btree (matricula_id, anio_lectivo_id, seccion_id);


--
-- Name: idx_calificaciones_periodo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_calificaciones_periodo ON public.calificaciones_cneb USING btree (periodo_academico_id);


--
-- Name: idx_calificaciones_periodo_anio_fk; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_calificaciones_periodo_anio_fk ON public.calificaciones_cneb USING btree (periodo_academico_id, anio_lectivo_id);


--
-- Name: idx_calificaciones_refuerzo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_calificaciones_refuerzo ON public.calificaciones_cneb USING btree (periodo_academico_id, requiere_refuerzo) WHERE (requiere_refuerzo = true);


--
-- Name: idx_competencias_area; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_competencias_area ON public.competencias USING btree (area_id);


--
-- Name: idx_comunicado_dest_apod; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_comunicado_dest_apod ON public.comunicado_destinatarios USING btree (apoderado_id);


--
-- Name: idx_comunicados_remitente; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_comunicados_remitente ON public.comunicados_oficiales USING btree (remitente_usuario_id);


--
-- Name: idx_conciliaciones_estudiante_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_conciliaciones_estudiante_id ON public.conciliaciones_asistencia USING btree (estudiante_id);


--
-- Name: idx_documentos_version_actual_fk; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_documentos_version_actual_fk ON public.solicitudes_matricula_documentos USING btree (version_id, solicitud_id, tipo);


--
-- Name: idx_estudiante_apoderados_apod; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_estudiante_apoderados_apod ON public.estudiante_apoderados USING btree (apoderado_id);


--
-- Name: idx_estudiantes_apellidos; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_estudiantes_apellidos ON public.estudiantes USING btree (apellido_paterno_estudiante, apellido_materno_estudiante);


--
-- Name: idx_estudiantes_estado; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_estudiantes_estado ON public.estudiantes USING btree (estado);


--
-- Name: idx_expedientes_emp_doc_estado; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_expedientes_emp_doc_estado ON public.expedientes_empleados_documentos USING btree (estado_ia);


--
-- Name: idx_expedientes_emp_doc_usuario; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_expedientes_emp_doc_usuario ON public.expedientes_empleados_documentos USING btree (usuario_id);


--
-- Name: idx_historial_incidencia_fecha; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_historial_incidencia_fecha ON public.historial_estados_incidencia USING btree (incidencia_id, fecha_cambio, id);


--
-- Name: idx_historial_incidencia_usuario_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_historial_incidencia_usuario_id ON public.historial_estados_incidencia USING btree (usuario_id);


--
-- Name: idx_horarios_anio_lectivo_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_horarios_anio_lectivo_id ON public.horarios_seccion USING btree (anio_lectivo_id);


--
-- Name: idx_horarios_asignacion; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_horarios_asignacion ON public.horarios_seccion USING btree (asignacion_docente_id);


--
-- Name: idx_horarios_asignacion_contexto_fk; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_horarios_asignacion_contexto_fk ON public.horarios_seccion USING btree (asignacion_docente_id, anio_lectivo_id, seccion_id, docente_usuario_id);


--
-- Name: idx_horarios_bloque; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_horarios_bloque ON public.horarios_seccion USING btree (bloque_horario_id);


--
-- Name: idx_horarios_docente; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_horarios_docente ON public.horarios_seccion USING btree (docente_usuario_id, dia_semana);


--
-- Name: idx_incidencias_matricula; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_incidencias_matricula ON public.incidencias_conductuales USING btree (matricula_id);


--
-- Name: idx_incidencias_matricula_falta_estado; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_incidencias_matricula_falta_estado ON public.incidencias_conductuales USING btree (matricula_id, tipo_falta, estado);


--
-- Name: idx_incidencias_reportado; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_incidencias_reportado ON public.incidencias_conductuales USING btree (reportado_por_usuario_id);


--
-- Name: idx_inscripciones_calificacion_origen_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_inscripciones_calificacion_origen_id ON public.inscripciones_refuerzo USING btree (calificacion_origen_id);


--
-- Name: idx_inscripciones_refuerzo_est; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_inscripciones_refuerzo_est ON public.inscripciones_refuerzo USING btree (estudiante_id);


--
-- Name: idx_lotes_biometrico_importado_por; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_lotes_biometrico_importado_por ON public.lotes_biometrico USING btree (importado_por_usuario_id);


--
-- Name: idx_mallas_area; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_mallas_area ON public.mallas_horarias_grado USING btree (area_curricular_id);


--
-- Name: idx_mallas_bloque; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_mallas_bloque ON public.mallas_horarias_grado USING btree (bloque_horario_id);


--
-- Name: idx_mallas_grado_anio; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_mallas_grado_anio ON public.mallas_horarias_grado USING btree (grado_id, anio_lectivo_id);


--
-- Name: idx_mallas_horarias_grado_anio_lectivo_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_mallas_horarias_grado_anio_lectivo_id ON public.mallas_horarias_grado USING btree (anio_lectivo_id);


--
-- Name: idx_marcas_lote; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_marcas_lote ON public.marcas_biometrico_porteria USING btree (lote_id);


--
-- Name: idx_marcas_porteria_dni_fecha; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_marcas_porteria_dni_fecha ON public.marcas_biometrico_porteria USING btree (dni_leido, fecha_hora);


--
-- Name: idx_marcas_porteria_dni_leido; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_marcas_porteria_dni_leido ON public.marcas_biometrico_porteria USING btree (dni_leido);


--
-- Name: idx_marcas_porteria_estudiante_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_marcas_porteria_estudiante_id ON public.marcas_biometrico_porteria USING btree (estudiante_id);


--
-- Name: idx_marcas_porteria_fecha_hora; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_marcas_porteria_fecha_hora ON public.marcas_biometrico_porteria USING btree (fecha_hora);


--
-- Name: idx_matriculas_estudiante_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matriculas_estudiante_id ON public.matriculas USING btree (estudiante_id);


--
-- Name: idx_matriculas_seccion; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matriculas_seccion ON public.matriculas USING btree (seccion_id, estado_matricula);


--
-- Name: idx_matriculas_seccion_anio_fk; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matriculas_seccion_anio_fk ON public.matriculas USING btree (seccion_id, anio_lectivo_id);


--
-- Name: idx_obligaciones_concepto; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_obligaciones_concepto ON public.obligaciones_pago USING btree (concepto_id);


--
-- Name: idx_obligaciones_estado_venc; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_obligaciones_estado_venc ON public.obligaciones_pago USING btree (estado, fecha_vencimiento);


--
-- Name: idx_obligaciones_matricula; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_obligaciones_matricula ON public.obligaciones_pago USING btree (matricula_id);


--
-- Name: idx_pagos_obligacion_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pagos_obligacion_id ON public.pagos_transacciones USING btree (obligacion_pago_id);


--
-- Name: idx_refuerzos_periodo_anio_fk; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_refuerzos_periodo_anio_fk ON public.sesiones_refuerzo USING btree (periodo_academico_id, anio_lectivo_id);


--
-- Name: idx_secciones_aula_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_secciones_aula_id ON public.secciones USING btree (aula_id);


--
-- Name: idx_secciones_grado_nivel; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_secciones_grado_nivel ON public.secciones USING btree (grado_id, nivel_id);


--
-- Name: idx_sesiones_refuerzo_area; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sesiones_refuerzo_area ON public.sesiones_refuerzo USING btree (area_curricular_id);


--
-- Name: idx_sesiones_refuerzo_aula_asignada; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sesiones_refuerzo_aula_asignada ON public.sesiones_refuerzo USING btree (codigo_aula);


--
-- Name: idx_sesiones_refuerzo_docente_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sesiones_refuerzo_docente_id ON public.sesiones_refuerzo USING btree (docente_usuario_id);


--
-- Name: idx_sesiones_refuerzo_periodo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sesiones_refuerzo_periodo ON public.sesiones_refuerzo USING btree (periodo_academico_id);


--
-- Name: idx_sesiones_usuario; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sesiones_usuario ON public.sesiones USING btree (usuario_id);


--
-- Name: idx_solicitudes_matricula_anio; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_solicitudes_matricula_anio ON public.solicitudes_matricula_publica USING btree (anio_lectivo_id);


--
-- Name: idx_solicitudes_matricula_apoderado; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_solicitudes_matricula_apoderado ON public.solicitudes_matricula_publica USING btree (apoderado_id) WHERE (apoderado_id IS NOT NULL);


--
-- Name: idx_solicitudes_matricula_autorizacion_edad; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_solicitudes_matricula_autorizacion_edad ON public.solicitudes_matricula_publica USING btree (fecha_creacion DESC) WHERE ((estado)::text = 'AUTORIZACION_EDAD_PENDIENTE'::text);


--
-- Name: idx_solicitudes_matricula_estado_fecha; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_solicitudes_matricula_estado_fecha ON public.solicitudes_matricula_publica USING btree (estado, fecha_creacion DESC);


--
-- Name: idx_solicitudes_matricula_estudiante; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_solicitudes_matricula_estudiante ON public.solicitudes_matricula_publica USING btree (estudiante_id) WHERE (estudiante_id IS NOT NULL);


--
-- Name: idx_solicitudes_matricula_huella_token; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_solicitudes_matricula_huella_token ON public.solicitudes_matricula_publica USING btree (huella_token);


--
-- Name: idx_solicitudes_matricula_matricula; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_solicitudes_matricula_matricula ON public.solicitudes_matricula_publica USING btree (matricula_id) WHERE (matricula_id IS NOT NULL);


--
-- Name: idx_solicitudes_matricula_publica_autorizado_por_usuario_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_solicitudes_matricula_publica_autorizado_por_usuario_id ON public.solicitudes_matricula_publica USING btree (autorizado_por_usuario_id);


--
-- Name: idx_solicitudes_matricula_publica_grado_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_solicitudes_matricula_publica_grado_id ON public.solicitudes_matricula_publica USING btree (grado_id);


--
-- Name: idx_solicitudes_matricula_reservas; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_solicitudes_matricula_reservas ON public.solicitudes_matricula_publica USING btree (fecha_expiracion_pago) WHERE (vacante_reservada IS TRUE);


--
-- Name: idx_solicitudes_matricula_seccion_anio; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_solicitudes_matricula_seccion_anio ON public.solicitudes_matricula_publica USING btree (seccion_id, anio_lectivo_id);


--
-- Name: idx_suplentes_autorizador; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_suplentes_autorizador ON public.apoderados_suplentes USING btree (autorizado_por_usuario_id);


--
-- Name: idx_suplentes_estudiante_estado; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_suplentes_estudiante_estado ON public.apoderados_suplentes USING btree (estudiante_id, estado);


--
-- Name: idx_suplentes_titular; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_suplentes_titular ON public.apoderados_suplentes USING btree (apoderado_titular_id);


--
-- Name: idx_usuario_roles_rol_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_usuario_roles_rol_id ON public.usuario_roles USING btree (rol_id);


--
-- Name: idx_usuarios_dni; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_usuarios_dni ON public.usuarios USING btree (dni) WHERE (dni IS NOT NULL);


--
-- Name: idx_usuarios_estado_laboral; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_usuarios_estado_laboral ON public.usuarios USING btree (estado_laboral);


--
-- Name: idx_versiones_documentos_solicitud_tipo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_versiones_documentos_solicitud_tipo ON public.versiones_documentos_matricula USING btree (solicitud_id, tipo, fecha_carga DESC);


--
-- Name: uq_aulas_codigo_normalizado; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_aulas_codigo_normalizado ON public.aulas USING btree (upper((codigo_aula)::text));


--
-- Name: uq_mallas_grado_bloque_activo; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_mallas_grado_bloque_activo ON public.mallas_horarias_grado USING btree (anio_lectivo_id, grado_id, dia_semana, bloque_horario_id) WHERE (activo IS TRUE);


--
-- Name: uq_marcas_porteria_evento_valido; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_marcas_porteria_evento_valido ON public.marcas_biometrico_porteria USING btree (dni_leido, fecha_hora, dispositivo_codigo) WHERE ((estado_procesamiento)::text <> 'DUPLICADO'::text);


--
-- Name: uq_matriculas_estudiante_anio_activa; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_matriculas_estudiante_anio_activa ON public.matriculas USING btree (estudiante_id, anio_lectivo_id) WHERE ((estado_matricula)::text = ANY ((ARRAY['MATRICULADO'::character varying, 'RESERVADA_TEMPORAL'::character varying, 'PENDIENTE_ASIGNACION'::character varying])::text[]));


--
-- Name: uq_secciones_anio_aula_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_secciones_anio_aula_id ON public.secciones USING btree (anio_lectivo_id, aula_id);


--
-- Name: uq_solicitud_matricula_comprobante; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_solicitud_matricula_comprobante ON public.solicitudes_matricula_publica USING btree (comprobante_pago_codigo) WHERE (comprobante_pago_codigo IS NOT NULL);


--
-- Name: uq_solicitud_matricula_dni_activa; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_solicitud_matricula_dni_activa ON public.solicitudes_matricula_publica USING btree (numero_documento_estudiante) WHERE ((estado)::text <> ALL (ARRAY[('MATRICULADA'::character varying)::text, ('RECHAZADA'::character varying)::text]));


--
-- Name: uq_suplencia_solicitud_vigente; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_suplencia_solicitud_vigente ON public.apoderados_suplentes USING btree (estudiante_id, apoderado_titular_id, numero_documento_suplente, fecha_inicio, fecha_fin) WHERE ((estado)::text = ANY ((ARRAY['PENDIENTE_APROBACION'::character varying, 'ACTIVO'::character varying])::text[]));


--
-- Name: ux_estudiante_apoderados_un_responsable_economico; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX ux_estudiante_apoderados_un_responsable_economico ON public.estudiante_apoderados USING btree (estudiante_id) WHERE (es_responsable_economico IS TRUE);


--
-- Name: ux_horarios_docente_activo; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX ux_horarios_docente_activo ON public.horarios_seccion USING btree (docente_usuario_id, dia_semana, bloque_horario_id, anio_lectivo_id) WHERE (activo IS TRUE);


--
-- Name: ux_horarios_seccion_activo; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX ux_horarios_seccion_activo ON public.horarios_seccion USING btree (seccion_id, dia_semana, bloque_horario_id, anio_lectivo_id) WHERE (activo IS TRUE);


--
-- Name: pagos_transacciones trg_anular_comprobante_pago; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_anular_comprobante_pago AFTER UPDATE OF estado_pago ON public.pagos_transacciones FOR EACH ROW EXECUTE FUNCTION public.fn_anular_comprobante_pago();


--
-- Name: pagos_transacciones trg_aplicar_pago_obligacion; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_aplicar_pago_obligacion AFTER INSERT OR UPDATE OF estado_pago, monto_pagado ON public.pagos_transacciones FOR EACH ROW EXECUTE FUNCTION public.fn_aplicar_pago_obligacion();


--
-- Name: anios_lectivos trg_auditar_anios_lectivos; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditar_anios_lectivos AFTER INSERT OR DELETE OR UPDATE ON public.anios_lectivos FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios_seguro();


--
-- Name: apoderados trg_auditar_apoderados; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditar_apoderados AFTER INSERT OR DELETE OR UPDATE ON public.apoderados FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios_seguro();


--
-- Name: apoderados_suplentes trg_auditar_apoderados_suplentes; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditar_apoderados_suplentes AFTER INSERT OR DELETE OR UPDATE ON public.apoderados_suplentes FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios_seguro();


--
-- Name: areas_curriculares trg_auditar_areas_curriculares; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditar_areas_curriculares AFTER INSERT OR DELETE OR UPDATE ON public.areas_curriculares FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios_seguro();


--
-- Name: asignaciones_docentes trg_auditar_asignaciones_docentes; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditar_asignaciones_docentes AFTER INSERT OR DELETE OR UPDATE ON public.asignaciones_docentes FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios_seguro();


--
-- Name: asistencias_aula trg_auditar_asistencias_aula; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditar_asistencias_aula AFTER INSERT OR DELETE OR UPDATE ON public.asistencias_aula FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios_seguro();


--
-- Name: aulas trg_auditar_aulas; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditar_aulas AFTER INSERT OR DELETE OR UPDATE ON public.aulas FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios_seguro();


--
-- Name: bloques_horarios trg_auditar_bloques_horarios; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditar_bloques_horarios AFTER INSERT OR DELETE OR UPDATE ON public.bloques_horarios FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios_seguro();


--
-- Name: calificaciones_cneb trg_auditar_calificaciones_cneb; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditar_calificaciones_cneb AFTER INSERT OR DELETE OR UPDATE ON public.calificaciones_cneb FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios_seguro();


--
-- Name: competencias trg_auditar_competencias; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditar_competencias AFTER INSERT OR DELETE OR UPDATE ON public.competencias FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios_seguro();


--
-- Name: comprobantes_pago trg_auditar_comprobantes_pago; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditar_comprobantes_pago AFTER INSERT OR DELETE OR UPDATE ON public.comprobantes_pago FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios_seguro();


--
-- Name: comunicado_destinatarios trg_auditar_comunicado_destinatarios; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditar_comunicado_destinatarios AFTER INSERT OR DELETE OR UPDATE ON public.comunicado_destinatarios FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios_seguro();


--
-- Name: comunicados_oficiales trg_auditar_comunicados_oficiales; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditar_comunicados_oficiales AFTER INSERT OR DELETE OR UPDATE ON public.comunicados_oficiales FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios_seguro();


--
-- Name: conceptos_cobro trg_auditar_conceptos_cobro; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditar_conceptos_cobro AFTER INSERT OR DELETE OR UPDATE ON public.conceptos_cobro FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios_seguro();


--
-- Name: conciliaciones_asistencia trg_auditar_conciliaciones_asistencia; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditar_conciliaciones_asistencia AFTER INSERT OR DELETE OR UPDATE ON public.conciliaciones_asistencia FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios_seguro();


--
-- Name: estudiante_apoderados trg_auditar_estudiante_apoderados; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditar_estudiante_apoderados AFTER INSERT OR DELETE OR UPDATE ON public.estudiante_apoderados FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios_seguro();


--
-- Name: estudiantes trg_auditar_estudiantes; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditar_estudiantes AFTER INSERT OR DELETE OR UPDATE ON public.estudiantes FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios_seguro();


--
-- Name: expedientes_empleados_documentos trg_auditar_expedientes_empleados_documentos; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditar_expedientes_empleados_documentos AFTER INSERT OR DELETE OR UPDATE ON public.expedientes_empleados_documentos FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios_seguro();


--
-- Name: grados trg_auditar_grados; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditar_grados AFTER INSERT OR DELETE OR UPDATE ON public.grados FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios_seguro();


--
-- Name: historial_estados_incidencia trg_auditar_historial_estados_incidencia; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditar_historial_estados_incidencia AFTER INSERT OR DELETE OR UPDATE ON public.historial_estados_incidencia FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios_seguro();


--
-- Name: horarios_seccion trg_auditar_horarios_seccion; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditar_horarios_seccion AFTER INSERT OR DELETE OR UPDATE ON public.horarios_seccion FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios_seguro();


--
-- Name: incidencias_conductuales trg_auditar_incidencias_conductuales; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditar_incidencias_conductuales AFTER INSERT OR DELETE OR UPDATE ON public.incidencias_conductuales FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios_seguro();


--
-- Name: inscripciones_refuerzo trg_auditar_inscripciones_refuerzo; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditar_inscripciones_refuerzo AFTER INSERT OR DELETE OR UPDATE ON public.inscripciones_refuerzo FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios_seguro();


--
-- Name: lotes_biometrico trg_auditar_lotes_biometrico; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditar_lotes_biometrico AFTER INSERT OR DELETE OR UPDATE ON public.lotes_biometrico FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios_seguro();


--
-- Name: mallas_horarias_grado trg_auditar_mallas_horarias_grado; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditar_mallas_horarias_grado AFTER INSERT OR DELETE OR UPDATE ON public.mallas_horarias_grado FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios_seguro();


--
-- Name: marcas_biometrico_porteria trg_auditar_marcas_biometrico_porteria; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditar_marcas_biometrico_porteria AFTER INSERT OR DELETE OR UPDATE ON public.marcas_biometrico_porteria FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios_seguro();


--
-- Name: matriculas trg_auditar_matriculas; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditar_matriculas AFTER INSERT OR DELETE OR UPDATE ON public.matriculas FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios_seguro();


--
-- Name: niveles trg_auditar_niveles; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditar_niveles AFTER INSERT OR DELETE OR UPDATE ON public.niveles FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios_seguro();


--
-- Name: obligaciones_pago trg_auditar_obligaciones_pago; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditar_obligaciones_pago AFTER INSERT OR DELETE OR UPDATE ON public.obligaciones_pago FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios_seguro();


--
-- Name: pagos_transacciones trg_auditar_pagos_transacciones; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditar_pagos_transacciones AFTER INSERT OR DELETE OR UPDATE ON public.pagos_transacciones FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios_seguro();


--
-- Name: periodos_academicos trg_auditar_periodos_academicos; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditar_periodos_academicos AFTER INSERT OR DELETE OR UPDATE ON public.periodos_academicos FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios_seguro();


--
-- Name: roles trg_auditar_roles; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditar_roles AFTER INSERT OR DELETE OR UPDATE ON public.roles FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios_seguro();


--
-- Name: secciones trg_auditar_secciones; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditar_secciones AFTER INSERT OR DELETE OR UPDATE ON public.secciones FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios_seguro();


--
-- Name: series_comprobante trg_auditar_series_comprobante; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditar_series_comprobante AFTER INSERT OR DELETE OR UPDATE ON public.series_comprobante FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios_seguro();


--
-- Name: sesiones trg_auditar_sesiones; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditar_sesiones AFTER INSERT OR DELETE OR UPDATE ON public.sesiones FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios_seguro();


--
-- Name: sesiones_refuerzo trg_auditar_sesiones_refuerzo; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditar_sesiones_refuerzo AFTER INSERT OR DELETE OR UPDATE ON public.sesiones_refuerzo FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios_seguro();


--
-- Name: solicitudes_matricula_documentos trg_auditar_solicitudes_matricula_documentos; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditar_solicitudes_matricula_documentos AFTER INSERT OR DELETE OR UPDATE ON public.solicitudes_matricula_documentos FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios_seguro();


--
-- Name: solicitudes_matricula_publica trg_auditar_solicitudes_matricula_publica; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditar_solicitudes_matricula_publica AFTER INSERT OR DELETE OR UPDATE ON public.solicitudes_matricula_publica FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios_seguro();


--
-- Name: usuario_roles trg_auditar_usuario_roles; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditar_usuario_roles AFTER INSERT OR DELETE OR UPDATE ON public.usuario_roles FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios_seguro();


--
-- Name: usuarios trg_auditar_usuarios; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditar_usuarios AFTER INSERT OR DELETE OR UPDATE ON public.usuarios FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios_seguro();


--
-- Name: versiones_documentos_matricula trg_auditar_versiones_documentos_matricula; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditar_versiones_documentos_matricula AFTER INSERT OR DELETE OR UPDATE ON public.versiones_documentos_matricula FOR EACH ROW EXECUTE FUNCTION public.fn_auditar_cambios_seguro();


--
-- Name: auditoria_accesos_api trg_auditoria_accesos_api_inmutable; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditoria_accesos_api_inmutable BEFORE DELETE OR UPDATE ON public.auditoria_accesos_api FOR EACH ROW EXECUTE FUNCTION public.fn_impedir_mutacion_auditoria();


--
-- Name: auditoria_cambios trg_auditoria_cambios_inmutable; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditoria_cambios_inmutable BEFORE DELETE OR UPDATE ON public.auditoria_cambios FOR EACH ROW EXECUTE FUNCTION public.fn_impedir_mutacion_auditoria();


--
-- Name: calificaciones_cneb trg_calificaciones_fecha_actualizacion; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_calificaciones_fecha_actualizacion BEFORE UPDATE ON public.calificaciones_cneb FOR EACH ROW EXECUTE FUNCTION public.fn_actualizar_fecha();


--
-- Name: matriculas trg_gestionar_vacante_matricula; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_gestionar_vacante_matricula AFTER INSERT OR UPDATE OF estado_matricula, seccion_id ON public.matriculas FOR EACH ROW EXECUTE FUNCTION public.fn_gestionar_vacante_matricula();


--
-- Name: historial_estados_incidencia trg_historial_estados_incidencia_inmutable; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_historial_estados_incidencia_inmutable BEFORE DELETE OR UPDATE ON public.historial_estados_incidencia FOR EACH ROW EXECUTE FUNCTION public.fn_proteger_historial_estados_incidencia();


--
-- Name: pagos_transacciones trg_prevenir_cambio_obligacion_pago; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_prevenir_cambio_obligacion_pago BEFORE UPDATE OF obligacion_pago_id ON public.pagos_transacciones FOR EACH ROW EXECUTE FUNCTION public.fn_prevenir_cambio_obligacion_pago();


--
-- Name: comprobantes_pago trg_prevenir_cambio_pago_comprobante; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_prevenir_cambio_pago_comprobante BEFORE UPDATE OF pago_transaccion_id ON public.comprobantes_pago FOR EACH ROW EXECUTE FUNCTION public.fn_prevenir_cambio_pago_comprobante();


--
-- Name: usuarios trg_usuarios_fecha_actualizacion; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_usuarios_fecha_actualizacion BEFORE UPDATE ON public.usuarios FOR EACH ROW EXECUTE FUNCTION public.fn_actualizar_fecha();


--
-- Name: comprobantes_pago trg_validar_comprobante_pago; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_validar_comprobante_pago BEFORE INSERT ON public.comprobantes_pago FOR EACH ROW EXECUTE FUNCTION public.fn_validar_comprobante_pago();


--
-- Name: comprobantes_pago trg_validar_estado_comprobante_pago; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_validar_estado_comprobante_pago BEFORE UPDATE OF estado_comprobante, fecha_anulacion, motivo_anulacion ON public.comprobantes_pago FOR EACH ROW EXECUTE FUNCTION public.fn_validar_estado_comprobante_pago();


--
-- Name: pagos_transacciones trg_validar_transicion_pago; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_validar_transicion_pago BEFORE UPDATE OF estado_pago, monto_pagado ON public.pagos_transacciones FOR EACH ROW EXECUTE FUNCTION public.fn_validar_transicion_pago();


--
-- Name: versiones_documentos_matricula trg_version_documental_inmutable; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_version_documental_inmutable BEFORE DELETE OR UPDATE ON public.versiones_documentos_matricula FOR EACH ROW EXECUTE FUNCTION public.fn_proteger_version_documental();


--
-- Name: apoderados_suplentes apoderados_suplentes_apoderado_titular_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.apoderados_suplentes
    ADD CONSTRAINT apoderados_suplentes_apoderado_titular_id_fkey FOREIGN KEY (apoderado_titular_id) REFERENCES public.apoderados(id) ON DELETE RESTRICT;


--
-- Name: apoderados_suplentes apoderados_suplentes_autorizado_por_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.apoderados_suplentes
    ADD CONSTRAINT apoderados_suplentes_autorizado_por_usuario_id_fkey FOREIGN KEY (autorizado_por_usuario_id) REFERENCES public.usuarios(id) ON DELETE RESTRICT;


--
-- Name: apoderados_suplentes apoderados_suplentes_estudiante_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.apoderados_suplentes
    ADD CONSTRAINT apoderados_suplentes_estudiante_id_fkey FOREIGN KEY (estudiante_id) REFERENCES public.estudiantes(id) ON DELETE RESTRICT;


--
-- Name: apoderados apoderados_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.apoderados
    ADD CONSTRAINT apoderados_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id) ON DELETE SET NULL;


--
-- Name: areas_curriculares areas_curriculares_nivel_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.areas_curriculares
    ADD CONSTRAINT areas_curriculares_nivel_id_fkey FOREIGN KEY (nivel_id) REFERENCES public.niveles(id) ON DELETE RESTRICT;


--
-- Name: asignaciones_docentes asignaciones_docentes_area_curricular_id_nivel_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asignaciones_docentes
    ADD CONSTRAINT asignaciones_docentes_area_curricular_id_nivel_id_fkey FOREIGN KEY (area_curricular_id, nivel_id) REFERENCES public.areas_curriculares(id, nivel_id) ON DELETE RESTRICT;


--
-- Name: asignaciones_docentes asignaciones_docentes_docente_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asignaciones_docentes
    ADD CONSTRAINT asignaciones_docentes_docente_usuario_id_fkey FOREIGN KEY (docente_usuario_id) REFERENCES public.usuarios(id) ON DELETE RESTRICT;


--
-- Name: asignaciones_docentes asignaciones_docentes_seccion_id_anio_lectivo_id_nivel_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asignaciones_docentes
    ADD CONSTRAINT asignaciones_docentes_seccion_id_anio_lectivo_id_nivel_id_fkey FOREIGN KEY (seccion_id, anio_lectivo_id, nivel_id) REFERENCES public.secciones(id, anio_lectivo_id, nivel_id) ON DELETE RESTRICT;


--
-- Name: asistencias_aula asistencias_aula_auxiliar_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asistencias_aula
    ADD CONSTRAINT asistencias_aula_auxiliar_usuario_id_fkey FOREIGN KEY (auxiliar_usuario_id) REFERENCES public.usuarios(id) ON DELETE RESTRICT;


--
-- Name: asistencias_aula asistencias_aula_matricula_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asistencias_aula
    ADD CONSTRAINT asistencias_aula_matricula_id_fkey FOREIGN KEY (matricula_id) REFERENCES public.matriculas(id) ON DELETE RESTRICT;


--
-- Name: auditoria_cambios auditoria_cambios_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auditoria_cambios
    ADD CONSTRAINT auditoria_cambios_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id) ON DELETE SET NULL;


--
-- Name: calificaciones_cneb calificaciones_cneb_asignacion_docente_id_seccion_id_area__fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.calificaciones_cneb
    ADD CONSTRAINT calificaciones_cneb_asignacion_docente_id_seccion_id_area__fkey FOREIGN KEY (asignacion_docente_id, seccion_id, area_curricular_id, anio_lectivo_id, docente_usuario_id) REFERENCES public.asignaciones_docentes(id, seccion_id, area_curricular_id, anio_lectivo_id, docente_usuario_id) ON DELETE RESTRICT;


--
-- Name: calificaciones_cneb calificaciones_cneb_competencia_id_area_curricular_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.calificaciones_cneb
    ADD CONSTRAINT calificaciones_cneb_competencia_id_area_curricular_id_fkey FOREIGN KEY (competencia_id, area_curricular_id) REFERENCES public.competencias(id, area_id) ON DELETE RESTRICT;


--
-- Name: calificaciones_cneb calificaciones_cneb_matricula_id_anio_lectivo_id_seccion_i_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.calificaciones_cneb
    ADD CONSTRAINT calificaciones_cneb_matricula_id_anio_lectivo_id_seccion_i_fkey FOREIGN KEY (matricula_id, anio_lectivo_id, seccion_id) REFERENCES public.matriculas(id, anio_lectivo_id, seccion_id) ON DELETE RESTRICT;


--
-- Name: calificaciones_cneb calificaciones_cneb_periodo_academico_id_anio_lectivo_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.calificaciones_cneb
    ADD CONSTRAINT calificaciones_cneb_periodo_academico_id_anio_lectivo_id_fkey FOREIGN KEY (periodo_academico_id, anio_lectivo_id) REFERENCES public.periodos_academicos(id, anio_lectivo_id) ON DELETE RESTRICT;


--
-- Name: comprobantes_pago comprobantes_pago_pago_transaccion_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comprobantes_pago
    ADD CONSTRAINT comprobantes_pago_pago_transaccion_id_fkey FOREIGN KEY (pago_transaccion_id) REFERENCES public.pagos_transacciones(id) ON DELETE RESTRICT;


--
-- Name: comunicado_destinatarios comunicado_destinatarios_apoderado_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comunicado_destinatarios
    ADD CONSTRAINT comunicado_destinatarios_apoderado_id_fkey FOREIGN KEY (apoderado_id) REFERENCES public.apoderados(id) ON DELETE RESTRICT;


--
-- Name: comunicado_destinatarios comunicado_destinatarios_comunicado_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comunicado_destinatarios
    ADD CONSTRAINT comunicado_destinatarios_comunicado_id_fkey FOREIGN KEY (comunicado_id) REFERENCES public.comunicados_oficiales(id) ON DELETE RESTRICT;


--
-- Name: comunicados_oficiales comunicados_oficiales_remitente_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comunicados_oficiales
    ADD CONSTRAINT comunicados_oficiales_remitente_usuario_id_fkey FOREIGN KEY (remitente_usuario_id) REFERENCES public.usuarios(id) ON DELETE RESTRICT;


--
-- Name: conciliaciones_asistencia conciliaciones_asistencia_estudiante_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.conciliaciones_asistencia
    ADD CONSTRAINT conciliaciones_asistencia_estudiante_id_fkey FOREIGN KEY (estudiante_id) REFERENCES public.estudiantes(id) ON DELETE RESTRICT;


--
-- Name: estudiante_apoderados estudiante_apoderados_apoderado_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estudiante_apoderados
    ADD CONSTRAINT estudiante_apoderados_apoderado_id_fkey FOREIGN KEY (apoderado_id) REFERENCES public.apoderados(id) ON DELETE RESTRICT;


--
-- Name: estudiante_apoderados estudiante_apoderados_estudiante_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estudiante_apoderados
    ADD CONSTRAINT estudiante_apoderados_estudiante_id_fkey FOREIGN KEY (estudiante_id) REFERENCES public.estudiantes(id) ON DELETE RESTRICT;


--
-- Name: expedientes_empleados_documentos expedientes_empleados_documentos_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expedientes_empleados_documentos
    ADD CONSTRAINT expedientes_empleados_documentos_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id) ON DELETE RESTRICT;


--
-- Name: competencias fk_competencias_area_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.competencias
    ADD CONSTRAINT fk_competencias_area_id FOREIGN KEY (area_id) REFERENCES public.areas_curriculares(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: comprobantes_pago fk_comprobantes_pago_serie; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comprobantes_pago
    ADD CONSTRAINT fk_comprobantes_pago_serie FOREIGN KEY (serie) REFERENCES public.series_comprobante(serie) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: solicitudes_matricula_documentos fk_documento_version_actual; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.solicitudes_matricula_documentos
    ADD CONSTRAINT fk_documento_version_actual FOREIGN KEY (version_id, solicitud_id, tipo) REFERENCES public.versiones_documentos_matricula(id, solicitud_id, tipo) ON DELETE RESTRICT;


--
-- Name: historial_estados_incidencia fk_historial_incidencia; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.historial_estados_incidencia
    ADD CONSTRAINT fk_historial_incidencia FOREIGN KEY (incidencia_id) REFERENCES public.incidencias_conductuales(id) ON DELETE RESTRICT;


--
-- Name: historial_estados_incidencia fk_historial_incidencia_usuario; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.historial_estados_incidencia
    ADD CONSTRAINT fk_historial_incidencia_usuario FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id) ON DELETE RESTRICT;


--
-- Name: secciones fk_secciones_aula; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.secciones
    ADD CONSTRAINT fk_secciones_aula FOREIGN KEY (aula_id) REFERENCES public.aulas(id) ON DELETE RESTRICT;


--
-- Name: sesiones_refuerzo fk_sesiones_refuerzo_aula_codigo; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sesiones_refuerzo
    ADD CONSTRAINT fk_sesiones_refuerzo_aula_codigo FOREIGN KEY (codigo_aula) REFERENCES public.aulas(codigo_aula) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: solicitudes_matricula_publica fk_solicitud_matricula_estudiante_anio; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.solicitudes_matricula_publica
    ADD CONSTRAINT fk_solicitud_matricula_estudiante_anio FOREIGN KEY (matricula_id, estudiante_id, anio_lectivo_id) REFERENCES public.matriculas(id, estudiante_id, anio_lectivo_id) ON DELETE RESTRICT;


--
-- Name: solicitudes_matricula_publica fk_solicitud_matricula_grado; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.solicitudes_matricula_publica
    ADD CONSTRAINT fk_solicitud_matricula_grado FOREIGN KEY (grado_id) REFERENCES public.grados(id);


--
-- Name: solicitudes_matricula_publica fk_solicitud_seccion_anio_grado; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.solicitudes_matricula_publica
    ADD CONSTRAINT fk_solicitud_seccion_anio_grado FOREIGN KEY (seccion_id, anio_lectivo_id, grado_id) REFERENCES public.secciones(id, anio_lectivo_id, grado_id) ON DELETE RESTRICT;


--
-- Name: solicitudes_matricula_documentos fk_solicitudes_documentos_solicitud; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.solicitudes_matricula_documentos
    ADD CONSTRAINT fk_solicitudes_documentos_solicitud FOREIGN KEY (solicitud_id) REFERENCES public.solicitudes_matricula_publica(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: grados grados_nivel_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grados
    ADD CONSTRAINT grados_nivel_id_fkey FOREIGN KEY (nivel_id) REFERENCES public.niveles(id) ON DELETE RESTRICT;


--
-- Name: horarios_seccion horarios_seccion_anio_lectivo_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.horarios_seccion
    ADD CONSTRAINT horarios_seccion_anio_lectivo_id_fkey FOREIGN KEY (anio_lectivo_id) REFERENCES public.anios_lectivos(id) ON DELETE RESTRICT;


--
-- Name: horarios_seccion horarios_seccion_asignacion_docente_id_anio_lectivo_id_sec_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.horarios_seccion
    ADD CONSTRAINT horarios_seccion_asignacion_docente_id_anio_lectivo_id_sec_fkey FOREIGN KEY (asignacion_docente_id, anio_lectivo_id, seccion_id, docente_usuario_id) REFERENCES public.asignaciones_docentes(id, anio_lectivo_id, seccion_id, docente_usuario_id) ON DELETE RESTRICT;


--
-- Name: horarios_seccion horarios_seccion_bloque_horario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.horarios_seccion
    ADD CONSTRAINT horarios_seccion_bloque_horario_id_fkey FOREIGN KEY (bloque_horario_id) REFERENCES public.bloques_horarios(id) ON DELETE RESTRICT;


--
-- Name: horarios_seccion horarios_seccion_docente_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.horarios_seccion
    ADD CONSTRAINT horarios_seccion_docente_usuario_id_fkey FOREIGN KEY (docente_usuario_id) REFERENCES public.usuarios(id) ON DELETE RESTRICT;


--
-- Name: incidencias_conductuales incidencias_conductuales_matricula_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.incidencias_conductuales
    ADD CONSTRAINT incidencias_conductuales_matricula_id_fkey FOREIGN KEY (matricula_id) REFERENCES public.matriculas(id) ON DELETE RESTRICT;


--
-- Name: incidencias_conductuales incidencias_conductuales_reportado_por_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.incidencias_conductuales
    ADD CONSTRAINT incidencias_conductuales_reportado_por_usuario_id_fkey FOREIGN KEY (reportado_por_usuario_id) REFERENCES public.usuarios(id) ON DELETE RESTRICT;


--
-- Name: inscripciones_refuerzo inscripciones_refuerzo_calificacion_origen_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inscripciones_refuerzo
    ADD CONSTRAINT inscripciones_refuerzo_calificacion_origen_id_fkey FOREIGN KEY (calificacion_origen_id) REFERENCES public.calificaciones_cneb(id) ON DELETE SET NULL;


--
-- Name: inscripciones_refuerzo inscripciones_refuerzo_estudiante_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inscripciones_refuerzo
    ADD CONSTRAINT inscripciones_refuerzo_estudiante_id_fkey FOREIGN KEY (estudiante_id) REFERENCES public.estudiantes(id) ON DELETE RESTRICT;


--
-- Name: inscripciones_refuerzo inscripciones_refuerzo_sesion_refuerzo_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inscripciones_refuerzo
    ADD CONSTRAINT inscripciones_refuerzo_sesion_refuerzo_id_fkey FOREIGN KEY (sesion_refuerzo_id) REFERENCES public.sesiones_refuerzo(id) ON DELETE RESTRICT;


--
-- Name: lotes_biometrico lotes_biometrico_importado_por_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lotes_biometrico
    ADD CONSTRAINT lotes_biometrico_importado_por_usuario_id_fkey FOREIGN KEY (importado_por_usuario_id) REFERENCES public.usuarios(id) ON DELETE SET NULL;


--
-- Name: mallas_horarias_grado mallas_horarias_grado_anio_lectivo_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mallas_horarias_grado
    ADD CONSTRAINT mallas_horarias_grado_anio_lectivo_id_fkey FOREIGN KEY (anio_lectivo_id) REFERENCES public.anios_lectivos(id) ON DELETE RESTRICT;


--
-- Name: mallas_horarias_grado mallas_horarias_grado_area_curricular_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mallas_horarias_grado
    ADD CONSTRAINT mallas_horarias_grado_area_curricular_id_fkey FOREIGN KEY (area_curricular_id) REFERENCES public.areas_curriculares(id) ON DELETE RESTRICT;


--
-- Name: mallas_horarias_grado mallas_horarias_grado_bloque_horario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mallas_horarias_grado
    ADD CONSTRAINT mallas_horarias_grado_bloque_horario_id_fkey FOREIGN KEY (bloque_horario_id) REFERENCES public.bloques_horarios(id) ON DELETE RESTRICT;


--
-- Name: mallas_horarias_grado mallas_horarias_grado_grado_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mallas_horarias_grado
    ADD CONSTRAINT mallas_horarias_grado_grado_id_fkey FOREIGN KEY (grado_id) REFERENCES public.grados(id) ON DELETE RESTRICT;


--
-- Name: marcas_biometrico_porteria marcas_biometrico_porteria_estudiante_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.marcas_biometrico_porteria
    ADD CONSTRAINT marcas_biometrico_porteria_estudiante_id_fkey FOREIGN KEY (estudiante_id) REFERENCES public.estudiantes(id) ON DELETE SET NULL;


--
-- Name: marcas_biometrico_porteria marcas_biometrico_porteria_lote_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.marcas_biometrico_porteria
    ADD CONSTRAINT marcas_biometrico_porteria_lote_id_fkey FOREIGN KEY (lote_id) REFERENCES public.lotes_biometrico(id) ON DELETE SET NULL;


--
-- Name: matriculas matriculas_estudiante_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matriculas
    ADD CONSTRAINT matriculas_estudiante_id_fkey FOREIGN KEY (estudiante_id) REFERENCES public.estudiantes(id) ON DELETE RESTRICT;


--
-- Name: matriculas matriculas_seccion_id_anio_lectivo_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matriculas
    ADD CONSTRAINT matriculas_seccion_id_anio_lectivo_id_fkey FOREIGN KEY (seccion_id, anio_lectivo_id) REFERENCES public.secciones(id, anio_lectivo_id) ON DELETE RESTRICT;


--
-- Name: obligaciones_pago obligaciones_pago_concepto_id_tipo_concepto_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.obligaciones_pago
    ADD CONSTRAINT obligaciones_pago_concepto_id_tipo_concepto_fkey FOREIGN KEY (concepto_id, tipo_concepto) REFERENCES public.conceptos_cobro(id, tipo_concepto) ON DELETE RESTRICT;


--
-- Name: obligaciones_pago obligaciones_pago_matricula_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.obligaciones_pago
    ADD CONSTRAINT obligaciones_pago_matricula_id_fkey FOREIGN KEY (matricula_id) REFERENCES public.matriculas(id) ON DELETE RESTRICT;


--
-- Name: pagos_transacciones pagos_transacciones_obligacion_pago_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pagos_transacciones
    ADD CONSTRAINT pagos_transacciones_obligacion_pago_id_fkey FOREIGN KEY (obligacion_pago_id) REFERENCES public.obligaciones_pago(id) ON DELETE RESTRICT;


--
-- Name: periodos_academicos periodos_academicos_anio_lectivo_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.periodos_academicos
    ADD CONSTRAINT periodos_academicos_anio_lectivo_id_fkey FOREIGN KEY (anio_lectivo_id) REFERENCES public.anios_lectivos(id) ON DELETE RESTRICT;


--
-- Name: secciones secciones_anio_lectivo_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.secciones
    ADD CONSTRAINT secciones_anio_lectivo_id_fkey FOREIGN KEY (anio_lectivo_id) REFERENCES public.anios_lectivos(id) ON DELETE RESTRICT;


--
-- Name: secciones secciones_grado_id_nivel_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.secciones
    ADD CONSTRAINT secciones_grado_id_nivel_id_fkey FOREIGN KEY (grado_id, nivel_id) REFERENCES public.grados(id, nivel_id) ON DELETE RESTRICT;


--
-- Name: sesiones_refuerzo sesiones_refuerzo_area_curricular_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sesiones_refuerzo
    ADD CONSTRAINT sesiones_refuerzo_area_curricular_id_fkey FOREIGN KEY (area_curricular_id) REFERENCES public.areas_curriculares(id) ON DELETE RESTRICT;


--
-- Name: sesiones_refuerzo sesiones_refuerzo_docente_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sesiones_refuerzo
    ADD CONSTRAINT sesiones_refuerzo_docente_usuario_id_fkey FOREIGN KEY (docente_usuario_id) REFERENCES public.usuarios(id) ON DELETE RESTRICT;


--
-- Name: sesiones_refuerzo sesiones_refuerzo_periodo_academico_id_anio_lectivo_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sesiones_refuerzo
    ADD CONSTRAINT sesiones_refuerzo_periodo_academico_id_anio_lectivo_id_fkey FOREIGN KEY (periodo_academico_id, anio_lectivo_id) REFERENCES public.periodos_academicos(id, anio_lectivo_id) ON DELETE RESTRICT;


--
-- Name: sesiones sesiones_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sesiones
    ADD CONSTRAINT sesiones_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id) ON DELETE CASCADE;


--
-- Name: solicitudes_matricula_publica solicitudes_matricula_publica_anio_lectivo_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.solicitudes_matricula_publica
    ADD CONSTRAINT solicitudes_matricula_publica_anio_lectivo_id_fkey FOREIGN KEY (anio_lectivo_id) REFERENCES public.anios_lectivos(id) ON DELETE RESTRICT;


--
-- Name: solicitudes_matricula_publica solicitudes_matricula_publica_apoderado_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.solicitudes_matricula_publica
    ADD CONSTRAINT solicitudes_matricula_publica_apoderado_id_fkey FOREIGN KEY (apoderado_id) REFERENCES public.apoderados(id) ON DELETE RESTRICT;


--
-- Name: solicitudes_matricula_publica solicitudes_matricula_publica_autorizado_por_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.solicitudes_matricula_publica
    ADD CONSTRAINT solicitudes_matricula_publica_autorizado_por_usuario_id_fkey FOREIGN KEY (autorizado_por_usuario_id) REFERENCES public.usuarios(id) ON DELETE RESTRICT;


--
-- Name: solicitudes_matricula_publica solicitudes_matricula_publica_estudiante_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.solicitudes_matricula_publica
    ADD CONSTRAINT solicitudes_matricula_publica_estudiante_id_fkey FOREIGN KEY (estudiante_id) REFERENCES public.estudiantes(id) ON DELETE RESTRICT;


--
-- Name: solicitudes_matricula_publica solicitudes_matricula_publica_matricula_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.solicitudes_matricula_publica
    ADD CONSTRAINT solicitudes_matricula_publica_matricula_id_fkey FOREIGN KEY (matricula_id) REFERENCES public.matriculas(id) ON DELETE RESTRICT;


--
-- Name: usuario_roles usuario_roles_rol_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuario_roles
    ADD CONSTRAINT usuario_roles_rol_id_fkey FOREIGN KEY (rol_id) REFERENCES public.roles(id) ON DELETE RESTRICT;


--
-- Name: usuario_roles usuario_roles_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuario_roles
    ADD CONSTRAINT usuario_roles_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id) ON DELETE CASCADE;


--
-- Name: versiones_documentos_matricula versiones_documentos_matricula_solicitud_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.versiones_documentos_matricula
    ADD CONSTRAINT versiones_documentos_matricula_solicitud_id_fkey FOREIGN KEY (solicitud_id) REFERENCES public.solicitudes_matricula_publica(id) ON DELETE RESTRICT;


--
-- Name: anios_lectivos; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.anios_lectivos ENABLE ROW LEVEL SECURITY;

--
-- Name: apoderados; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.apoderados ENABLE ROW LEVEL SECURITY;

--
-- Name: apoderados_suplentes; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.apoderados_suplentes ENABLE ROW LEVEL SECURITY;

--
-- Name: areas_curriculares; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.areas_curriculares ENABLE ROW LEVEL SECURITY;

--
-- Name: asignaciones_docentes; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.asignaciones_docentes ENABLE ROW LEVEL SECURITY;

--
-- Name: asistencias_aula; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.asistencias_aula ENABLE ROW LEVEL SECURITY;

--
-- Name: auditoria_cambios; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.auditoria_cambios ENABLE ROW LEVEL SECURITY;

--
-- Name: aulas; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.aulas ENABLE ROW LEVEL SECURITY;

--
-- Name: bloques_horarios; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.bloques_horarios ENABLE ROW LEVEL SECURITY;

--
-- Name: calificaciones_cneb; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.calificaciones_cneb ENABLE ROW LEVEL SECURITY;

--
-- Name: competencias; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.competencias ENABLE ROW LEVEL SECURITY;

--
-- Name: comprobantes_pago; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.comprobantes_pago ENABLE ROW LEVEL SECURITY;

--
-- Name: comunicado_destinatarios; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.comunicado_destinatarios ENABLE ROW LEVEL SECURITY;

--
-- Name: comunicados_oficiales; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.comunicados_oficiales ENABLE ROW LEVEL SECURITY;

--
-- Name: conceptos_cobro; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.conceptos_cobro ENABLE ROW LEVEL SECURITY;

--
-- Name: conciliaciones_asistencia; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.conciliaciones_asistencia ENABLE ROW LEVEL SECURITY;

--
-- Name: estudiante_apoderados; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.estudiante_apoderados ENABLE ROW LEVEL SECURITY;

--
-- Name: estudiantes; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.estudiantes ENABLE ROW LEVEL SECURITY;

--
-- Name: grados; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.grados ENABLE ROW LEVEL SECURITY;

--
-- Name: horarios_seccion; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.horarios_seccion ENABLE ROW LEVEL SECURITY;

--
-- Name: incidencias_conductuales; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.incidencias_conductuales ENABLE ROW LEVEL SECURITY;

--
-- Name: inscripciones_refuerzo; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.inscripciones_refuerzo ENABLE ROW LEVEL SECURITY;

--
-- Name: lotes_biometrico; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.lotes_biometrico ENABLE ROW LEVEL SECURITY;

--
-- Name: mallas_horarias_grado; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.mallas_horarias_grado ENABLE ROW LEVEL SECURITY;

--
-- Name: marcas_biometrico_porteria; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.marcas_biometrico_porteria ENABLE ROW LEVEL SECURITY;

--
-- Name: matriculas; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.matriculas ENABLE ROW LEVEL SECURITY;

--
-- Name: niveles; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.niveles ENABLE ROW LEVEL SECURITY;

--
-- Name: obligaciones_pago; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.obligaciones_pago ENABLE ROW LEVEL SECURITY;

--
-- Name: pagos_transacciones; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.pagos_transacciones ENABLE ROW LEVEL SECURITY;

--
-- Name: periodos_academicos; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.periodos_academicos ENABLE ROW LEVEL SECURITY;

--
-- Name: roles; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.roles ENABLE ROW LEVEL SECURITY;

--
-- Name: secciones; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.secciones ENABLE ROW LEVEL SECURITY;

--
-- Name: series_comprobante; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.series_comprobante ENABLE ROW LEVEL SECURITY;

--
-- Name: sesiones; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.sesiones ENABLE ROW LEVEL SECURITY;

--
-- Name: sesiones_refuerzo; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.sesiones_refuerzo ENABLE ROW LEVEL SECURITY;

--
-- Name: solicitudes_matricula_documentos; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.solicitudes_matricula_documentos ENABLE ROW LEVEL SECURITY;

--
-- Name: solicitudes_matricula_publica; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.solicitudes_matricula_publica ENABLE ROW LEVEL SECURITY;

--
-- Name: usuario_roles; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.usuario_roles ENABLE ROW LEVEL SECURITY;

--
-- Name: usuarios; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.usuarios ENABLE ROW LEVEL SECURITY;

--
-- Name: versiones_documentos_matricula; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.versiones_documentos_matricula ENABLE ROW LEVEL SECURITY;

--
-- PostgreSQL database dump complete
--

\unrestrict ip8cG6GQ98GFcFJ7PhYNPY9rTmpRdJSDAuczyafvtNc7lUDu2nYgqb7OjRhtbL1

