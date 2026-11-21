-- Hibernate maps String columns as VARCHAR. V8 created this column as CHAR,
-- which makes schema validation fail on databases that already applied V8.
-- rtrim removes only CHAR padding and keeps the stored token hash unchanged.
ALTER TABLE solicitudes_matricula_publica
    ALTER COLUMN token_hash TYPE VARCHAR(64)
    USING rtrim(token_hash::text);
