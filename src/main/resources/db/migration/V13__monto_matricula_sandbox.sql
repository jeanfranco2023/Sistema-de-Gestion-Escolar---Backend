-- El importe de matrícula es configurable en la aplicación (por defecto S/ 350.00).
-- La base conserva importes históricos; solo valida que el importe sea positivo.
ALTER TABLE solicitudes_matricula_publica
    DROP CONSTRAINT IF EXISTS solicitudes_matricula_publica_pago_monto_check;

ALTER TABLE solicitudes_matricula_publica
    ALTER COLUMN pago_monto SET DEFAULT 350.00;

ALTER TABLE solicitudes_matricula_publica
    ADD CONSTRAINT chk_solicitudes_matricula_pago_monto_positivo
    CHECK (pago_monto > 0);