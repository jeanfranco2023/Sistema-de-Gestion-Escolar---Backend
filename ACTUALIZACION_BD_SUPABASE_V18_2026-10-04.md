# Actualización y verificación de base de datos V18 — 2026-10-04

## Estado

Supabase PostgreSQL 17.6, esquema `public`: Flyway V18 aplicado y 18 migraciones validadas. La inspección previa confirmó que los cambios V18 ya se habían ejecutado manualmente; esta migración los dejó registrados y repetibles en Flyway.

## Cambios alineados

- Correos de usuarios, apoderados y solicitudes públicas: máximo 50 caracteres en DDL, entidades JPA y DTOs.
- DNI: exactamente ocho dígitos cuando `tipo_documento = 'DNI'`, con reglas en PostgreSQL y servicios Java.
- `fn_liberar_reservas_expiradas()` usa `matriculas.fecha_expiracion_reserva`.
- V14–V18 quedan disponibles en el historial de migraciones; el SQL consolidado y DBML describen V18.

## Evidencia

- Inspección de Supabase previa: los cambios V18 ya estaban presentes y los prechecks de correo/DNI devolvieron cero filas incompatibles.
- El respaldo preventivo de atributos retirados no contenía filas.
- Tras aplicar V18, la comparación del esquema público con el consolidado reportó 307 atributos, 224 restricciones y conteos conservados en 37 tablas.
- Verificador temporal: V1–V18 equivalente al consolidado; V18 aplicado dos veces; verificados correo 50/51, DNI válido/inválido, límites de nombres/apellidos y observaciones, y trigger de fecha. Rollback completo.
- Parser DBML oficial: 37 tablas, 307 atributos y 56 relaciones; tabla `roles` solo contiene `id`, `codigo_rol`, `nombre_rol`.
- `scripts/VerificarPersistencia.ps1`: BUILD SUCCESS; 108 pruebas, cero fallos, errores u omitidas. Incluye pruebas de persistencia y concurrencia en esquema temporal de Supabase, eliminado por el script.
- La invocación directa `mvnw test` sin preparación falla en nueve pruebas que intentan conectarse a PostgreSQL local `127.0.0.1:55439`; para la suite integrada, usar el script anterior.

## Artefactos seguros

`src/main/resources/db/schema_completo_colegio_shuji.sql` y `schema_completo_importador_postgresql.sql` son idénticos. Son SQL de inicio para una base vacía, no deben ejecutarse sobre `public`. El dump omitió datos de cuentas y expedientes y excluyó `flyway_schema_history`.

`src/main/resources/db/colegio_shuji_v18.dbml` es la representación del modelo. La migración fuente es `src/main/resources/db/migration/V18__correo_dni_y_reservas_vencidas.sql`.

El log de comprobación no contiene valores de `.env`; el archivo `.env` no fue modificado.
