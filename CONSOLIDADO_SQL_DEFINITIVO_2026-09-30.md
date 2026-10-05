# SQL definitivo V17 — 2026-09-30

Se sustituyó el consolidado que concatenaba migraciones históricas por el DDL del estado final de Supabase. El anterior creaba `roles.descripcion` en V1 y la eliminaba en V16; el nuevo nunca la define.

Archivo principal: `src/main/resources/db/schema_completo_colegio_shuji.sql`.
Archivo del importador: `src/main/resources/db/schema_completo_importador_postgresql.sql` (contenido idéntico).

`roles` contiene únicamente `id SMALLINT`, `codigo_rol VARCHAR(20)` y `nombre_rol VARCHAR(30)`. Las restricciones PK y UNIQUE se declaran en el mismo archivo. Los INSERT del catálogo también contienen solo esos tres campos.

El SQL incluye 37 tablas, 307 atributos, 222 restricciones, secuencias, índices, funciones, triggers, RLS y catálogos académicos e institucionales. Excluye usuarios, credenciales, expedientes e historial Flyway. No incluye operaciones históricas de creación/eliminación de los UUID retirados ni renombrados de columnas.

Se expresaron los CHECK enumerados con IN/NOT IN en lugar de los casts internos impresos por pg_dump. PostgreSQL confirmó que todas las columnas y restricciones resultantes son idénticas a V1–V17. También pasaron las comprobaciones 20/21, 500/501, rechazo sin truncado y trigger de fecha. La transacción de verificación revirtió completamente ambos esquemas temporales; no se modificó public ni .env.

Este archivo es para una base vacía o para consultar/importar el modelo. La base real ya está en V17. Si se crea otra base mediante el consolidado, registrar baseline V17 antes de iniciar Flyway. Las migraciones históricas siguen intactas para conservar checksums.

Evidencia: `target/verificacion-consolidado-definitivo-v17.log`.
