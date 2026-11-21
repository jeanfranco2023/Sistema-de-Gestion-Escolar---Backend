-- El sistema escolar peruano utiliza exclusivamente el DNI como identificador.
-- Nunca trunca ni transforma datos: aborta antes si encuentra valores incompatibles.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM estudiantes
        WHERE tipo_documento <> 'DNI' OR numero_documento !~ '^[0-9]{8}$'
    ) THEN
        RAISE EXCEPTION 'V19 cancelada: estudiantes contiene documentos distintos de DNI de 8 digitos.';
    END IF;

    IF EXISTS (
        SELECT 1 FROM apoderados
        WHERE tipo_documento <> 'DNI' OR numero_documento !~ '^[0-9]{8}$'
    ) THEN
        RAISE EXCEPTION 'V19 cancelada: apoderados contiene documentos distintos de DNI de 8 digitos.';
    END IF;

    IF EXISTS (
        SELECT 1 FROM marcas_biometrico_porteria
        WHERE dni_leido !~ '^[0-9]{8}$'
    ) THEN
        RAISE EXCEPTION 'V19 cancelada: marcas biométricas contiene DNI que no tiene exactamente 8 digitos.';
    END IF;
END
$$;

ALTER TABLE estudiantes DROP CONSTRAINT IF EXISTS estudiantes_tipo_documento_check;
ALTER TABLE apoderados DROP CONSTRAINT IF EXISTS apoderados_tipo_documento_check;
ALTER TABLE estudiantes DROP CONSTRAINT IF EXISTS chk_estudiantes_dni_8_digitos;
ALTER TABLE apoderados DROP CONSTRAINT IF EXISTS chk_apoderados_dni_8_digitos;

ALTER TABLE estudiantes ALTER COLUMN tipo_documento TYPE VARCHAR(3);
ALTER TABLE estudiantes ALTER COLUMN numero_documento TYPE VARCHAR(8);
ALTER TABLE apoderados ALTER COLUMN tipo_documento TYPE VARCHAR(3);
ALTER TABLE apoderados ALTER COLUMN numero_documento TYPE VARCHAR(8);
ALTER TABLE marcas_biometrico_porteria ALTER COLUMN dni_leido TYPE VARCHAR(8);

ALTER TABLE estudiantes
    ADD CONSTRAINT estudiantes_tipo_documento_check CHECK (tipo_documento = 'DNI'),
    ADD CONSTRAINT chk_estudiantes_dni_8_digitos CHECK (numero_documento ~ '^[0-9]{8}$');

ALTER TABLE apoderados
    ADD CONSTRAINT apoderados_tipo_documento_check CHECK (tipo_documento = 'DNI'),
    ADD CONSTRAINT chk_apoderados_dni_8_digitos CHECK (numero_documento ~ '^[0-9]{8}$');

ALTER TABLE marcas_biometrico_porteria
    ADD CONSTRAINT chk_marcas_porteria_dni_8_digitos CHECK (dni_leido ~ '^[0-9]{8}$');
