Las migraciones V1–V49 se conservan aquí como historial de auditoría y no son cargadas por Flyway.

Las bases nuevas parten de `../migration/B49__esquema_consolidado.sql`. Este baseline contiene el esquema, no filas de usuarios, alumnos ni matrículas. Las nuevas modificaciones deben agregarse como V50 en adelante dentro de `db/migration`.
