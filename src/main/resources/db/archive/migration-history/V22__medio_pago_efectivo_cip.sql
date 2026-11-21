-- Distingue la red PagoEfectivo/CIP de caja y de transferencias bancarias directas.
-- Conserva los pagos históricos; no presume el canal bancario de operaciones anteriores.
ALTER TABLE pagos_transacciones DROP CONSTRAINT pagos_transacciones_metodo_pago_check;
ALTER TABLE pagos_transacciones ADD CONSTRAINT pagos_transacciones_metodo_pago_check
    CHECK (metodo_pago IN ('TARJETA_CREDITO', 'TARJETA_DEBITO', 'YAPE', 'PLIN',
                           'TRANSFERENCIA', 'EFECTIVO', 'PAGO_EFECTIVO'));
