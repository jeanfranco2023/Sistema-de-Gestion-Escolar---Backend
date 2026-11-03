-- 1. Suprimir obligatoriedad de ubigeo en apoderados y solicitudes de matricula publica
ALTER TABLE solicitudes_matricula_publica
    ALTER COLUMN ubigeo_apoderado DROP NOT NULL;

ALTER TABLE apoderados
    ALTER COLUMN ubigeo_inei DROP NOT NULL;

-- 2. Expediente laboral y credenciales del personal en la tabla usuarios
ALTER TABLE usuarios
    ADD COLUMN IF NOT EXISTS debe_cambiar_password BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS dni VARCHAR(8),
    ADD COLUMN IF NOT EXISTS telefono VARCHAR(15),
    ADD COLUMN IF NOT EXISTS direccion VARCHAR(150),
    ADD COLUMN IF NOT EXISTS especialidad VARCHAR(100),
    ADD COLUMN IF NOT EXISTS codigo_modular VARCHAR(20),
    ADD COLUMN IF NOT EXISTS numero_colegiatura VARCHAR(20),
    ADD COLUMN IF NOT EXISTS fecha_ingreso DATE,
    ADD COLUMN IF NOT EXISTS tipo_contrato VARCHAR(30),
    ADD COLUMN IF NOT EXISTS estado_laboral VARCHAR(20) NOT NULL DEFAULT 'ACTIVO';

-- 3. Restriccion de estados laborales para borrado logico formal
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_usuarios_estado_laboral'
    ) THEN
        ALTER TABLE usuarios
            ADD CONSTRAINT chk_usuarios_estado_laboral
            CHECK (estado_laboral IN ('ACTIVO', 'SUSPENDIDO', 'CESADO', 'INACTIVO'));
    END IF;
END $$;

-- 4. Indices para busqueda y desempeno operativo
CREATE INDEX IF NOT EXISTS idx_usuarios_dni ON usuarios (dni) WHERE dni IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_usuarios_estado_laboral ON usuarios (estado_laboral);
