-- Las justificaciones guardan una clave de objeto privado, nunca una URL pública.
ALTER TABLE asistencias_aula
    RENAME COLUMN documento_sustento_url TO documento_sustento_key;
