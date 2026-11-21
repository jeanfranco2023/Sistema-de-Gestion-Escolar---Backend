-- Evidencia de nuevas verificaciones; conserva sin cambios las versiones y estados anteriores.
ALTER TABLE versiones_documentos_matricula ADD COLUMN vinculo_parental_coincide BOOLEAN;
ALTER TABLE versiones_documentos_matricula ADD CONSTRAINT ck_version_vinculo_parental
  CHECK (vinculo_parental_coincide IS NULL OR tipo = 'PARTIDA_NACIMIENTO');

-- NOT VALID conserva imágenes históricas; las nuevas cargas y sustituciones sí deben ser PDF.
ALTER TABLE versiones_documentos_matricula ADD CONSTRAINT ck_version_documental_pdf
  CHECK (tipo_contenido = 'application/pdf') NOT VALID;
ALTER TABLE solicitudes_matricula_documentos ADD CONSTRAINT ck_documento_actual_pdf
  CHECK (tipo_contenido = 'application/pdf') NOT VALID;
