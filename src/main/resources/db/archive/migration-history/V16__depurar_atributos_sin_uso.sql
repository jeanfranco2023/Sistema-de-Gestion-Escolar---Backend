-- Identificadores numéricos existentes conservados; no se eliminan registros.
-- UUID de solicitudes públicas y sus relaciones se conservan.
ALTER TABLE usuarios DROP COLUMN uuid;
ALTER TABLE apoderados DROP COLUMN uuid;
ALTER TABLE estudiantes DROP COLUMN uuid;
ALTER TABLE matriculas DROP COLUMN uuid;
ALTER TABLE pagos_transacciones DROP COLUMN uuid;
ALTER TABLE comprobantes_pago DROP COLUMN uuid;
ALTER TABLE roles DROP COLUMN descripcion;
