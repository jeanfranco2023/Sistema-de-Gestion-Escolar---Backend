ALTER TABLE solicitudes_matricula_publica
    ADD COLUMN comprobante_pago_codigo VARCHAR(40),
    ADD COLUMN documentos_emitidos_at TIMESTAMPTZ;

-- Preserve completed applications while assigning an internal receipt reference.
UPDATE solicitudes_matricula_publica
SET comprobante_pago_codigo = 'REC-MAT-' || matricula_id::text,
    documentos_emitidos_at = updated_at
WHERE estado = 'MATRICULADA'
  AND matricula_id IS NOT NULL;

CREATE UNIQUE INDEX uq_solicitud_matricula_comprobante
    ON solicitudes_matricula_publica (comprobante_pago_codigo)
    WHERE comprobante_pago_codigo IS NOT NULL;

ALTER TABLE solicitudes_matricula_publica
    ADD CONSTRAINT chk_solicitud_matricula_documentos_emitidos
    CHECK ((comprobante_pago_codigo IS NULL) = (documentos_emitidos_at IS NULL));

ALTER TABLE solicitudes_matricula_publica
    ADD CONSTRAINT chk_solicitud_matricula_comprobante_confirmada
    CHECK (estado <> 'MATRICULADA' OR comprobante_pago_codigo IS NOT NULL);
