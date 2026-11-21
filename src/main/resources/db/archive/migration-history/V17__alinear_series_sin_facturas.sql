-- Corrige bases existentes cuyo CHECK histórico todavía admite facturas (F).
-- No cambia las series ni elimina comprobantes; aborta ante valores incompatibles.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM series_comprobante WHERE serie !~ '^[BE][0-9]{3}$')
       OR EXISTS (SELECT 1 FROM comprobantes_pago WHERE serie !~ '^[BE][0-9]{3}$') THEN
        RAISE EXCEPTION 'V17 cancelada: existen series fuera del formato B/E acordado. No se cambiaron registros.';
    END IF;
END
$$;

ALTER TABLE series_comprobante
    DROP CONSTRAINT series_comprobante_serie_check,
    ADD CONSTRAINT series_comprobante_serie_check CHECK (serie ~ '^[BE][0-9]{3}$');

ALTER TABLE comprobantes_pago
    DROP CONSTRAINT comprobantes_pago_serie_check,
    ADD CONSTRAINT comprobantes_pago_serie_check CHECK (serie ~ '^[BE][0-9]{3}$');
