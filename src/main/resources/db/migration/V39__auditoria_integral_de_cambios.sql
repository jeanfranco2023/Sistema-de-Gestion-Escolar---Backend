ALTER TABLE auditoria_cambios
    ADD COLUMN IF NOT EXISTS usuario_nombre_snapshot VARCHAR(100),
    ADD COLUMN IF NOT EXISTS roles_usuario_snapshot VARCHAR(300),
    ADD COLUMN IF NOT EXISTS registro_clave VARCHAR(500);

ALTER TABLE auditoria_cambios ALTER COLUMN registro_id DROP NOT NULL;

UPDATE auditoria_cambios
SET registro_clave = COALESCE(registro_id::TEXT, 'legado:' || id::TEXT)
WHERE registro_clave IS NULL;

ALTER TABLE auditoria_cambios ALTER COLUMN registro_clave SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_auditoria_cambios_fecha_id
    ON auditoria_cambios (fecha_creacion DESC, id DESC);

CREATE OR REPLACE FUNCTION fn_sanitizar_fila_auditoria(fila JSONB)
RETURNS JSONB
LANGUAGE SQL
IMMUTABLE
SET search_path = public, pg_temp
AS $$
    SELECT COALESCE(jsonb_object_agg(campo.key, campo.value), '{}'::JSONB)
    FROM jsonb_each(COALESCE(fila, '{}'::JSONB)) AS campo
    WHERE campo.key !~* '(password|contrase|token|secret|clave|dni|documento|archivo|contenido|email|correo|direccion|telefono|celular|url|foto|imagen)';
$$;

CREATE OR REPLACE FUNCTION fn_clave_auditoria(tabla_oid OID, fila JSONB)
RETURNS TEXT
LANGUAGE plpgsql
STABLE
SET search_path = public, pg_temp
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

CREATE OR REPLACE FUNCTION fn_auditar_cambios_seguro()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
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

DO $$
DECLARE
    tabla RECORD;
    disparador RECORD;
    nombre_disparador TEXT;
BEGIN
    FOR tabla IN
        SELECT t.table_name
        FROM information_schema.tables t
        WHERE t.table_schema = 'public'
          AND t.table_type = 'BASE TABLE'
          AND t.table_name NOT IN ('auditoria_cambios', 'auditoria_accesos_api', 'flyway_schema_history')
    LOOP
        FOR disparador IN
            SELECT tr.tgname
            FROM pg_trigger tr
            JOIN pg_proc fn ON fn.oid = tr.tgfoid
            WHERE tr.tgrelid = format('%I.%I', 'public', tabla.table_name)::regclass
              AND NOT tr.tgisinternal
              AND fn.proname LIKE 'fn_auditar_cambios%'
        LOOP
            EXECUTE format('DROP TRIGGER %I ON public.%I', disparador.tgname, tabla.table_name);
        END LOOP;

        nombre_disparador := left('trg_auditar_' || tabla.table_name, 63);
        EXECUTE format('DROP TRIGGER IF EXISTS %I ON public.%I', nombre_disparador, tabla.table_name);
        EXECUTE format(
            'CREATE TRIGGER %I AFTER INSERT OR UPDATE OR DELETE ON public.%I FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambios_seguro()',
            nombre_disparador,
            tabla.table_name
        );
    END LOOP;
END;
$$;
