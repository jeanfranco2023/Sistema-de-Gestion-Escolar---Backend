-- Registra en Flyway los cambios V18 y completa instalaciones que solo tengan V17.
-- La migracion es compatible con el esquema V18 aplicado manualmente en Supabase.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM usuarios WHERE char_length(correo_electronico) > 50)
       OR EXISTS (SELECT 1 FROM apoderados WHERE correo_electronico IS NOT NULL AND char_length(correo_electronico) > 50)
       OR EXISTS (SELECT 1 FROM solicitudes_matricula_publica WHERE correo_apoderado IS NOT NULL AND char_length(correo_apoderado) > 50) THEN
        RAISE EXCEPTION 'V18 cancelada: existen correos mayores de 50 caracteres; no se truncaron datos.';
    END IF;

    IF EXISTS (SELECT 1 FROM estudiantes WHERE tipo_documento = 'DNI' AND numero_documento !~ '^[0-9]{8}$')
       OR EXISTS (SELECT 1 FROM apoderados WHERE tipo_documento = 'DNI' AND numero_documento !~ '^[0-9]{8}$') THEN
        RAISE EXCEPTION 'V18 cancelada: existen DNI que no contienen exactamente ocho digitos.';
    END IF;
END
$$;

ALTER TABLE usuarios ALTER COLUMN correo_electronico TYPE VARCHAR(50);
ALTER TABLE apoderados ALTER COLUMN correo_electronico TYPE VARCHAR(50);
ALTER TABLE solicitudes_matricula_publica ALTER COLUMN correo_apoderado TYPE VARCHAR(50);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'estudiantes'::regclass AND conname = 'chk_estudiantes_dni_8_digitos') THEN
        ALTER TABLE estudiantes ADD CONSTRAINT chk_estudiantes_dni_8_digitos
            CHECK (tipo_documento <> 'DNI' OR numero_documento ~ '^[0-9]{8}$') NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'apoderados'::regclass AND conname = 'chk_apoderados_dni_8_digitos') THEN
        ALTER TABLE apoderados ADD CONSTRAINT chk_apoderados_dni_8_digitos
            CHECK (tipo_documento <> 'DNI' OR numero_documento ~ '^[0-9]{8}$') NOT VALID;
    END IF;
END
$$;

ALTER TABLE estudiantes VALIDATE CONSTRAINT chk_estudiantes_dni_8_digitos;
ALTER TABLE apoderados VALIDATE CONSTRAINT chk_apoderados_dni_8_digitos;

CREATE OR REPLACE FUNCTION fn_liberar_reservas_expiradas()
RETURNS INTEGER AS $$
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
$$ LANGUAGE plpgsql;
