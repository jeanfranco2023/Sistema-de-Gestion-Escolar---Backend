ALTER TABLE usuarios
    ADD COLUMN IF NOT EXISTS nombre_completo VARCHAR(200);
