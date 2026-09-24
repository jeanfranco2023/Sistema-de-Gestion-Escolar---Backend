# Auditoría preliminar rigurosa de base de datos

**Fecha:** 2026-09-24  
**Alcance:** solo PostgreSQL/Supabase, migraciones V1–V11, entidades JPA, tipos, longitudes, claves, relaciones, restricciones, índices y seguridad.  
**Estado:** revisión detenida antes de corregir; se requiere decisión del propietario.

## Puntos que están fallando o requieren decisión

### Críticos

1. **La documentación/script consolidado está incompleto.** El repositorio actual tiene V1–V11. El script `schema_completo_colegio_shuji.sql` y su variante de importador solo consolidan V1–V5. Faltan las tablas y cambios de V8–V11, especialmente `solicitudes_matricula_publica`, `solicitudes_matricula_documentos` y los cambios de documentos/comprobantes.

2. **V7 instala una contraseña administrativa fija y conocida.** `V6` crea `fq94289@gmail.com` y `V7` establece `Admin2026!` mediante BCrypt. Aunque el comentario dice práctica, Flyway ejecuta la migración en cualquier entorno salvo que el pipeline lo impida. Debe eliminarse del conjunto productivo o reemplazarse por bootstrap controlado por secreto/entorno.

3. **Hash documental con tipo físico `CHAR(64)`.** `solicitudes_matricula_documentos.sha256` usa `CHAR(64)`, mientras la entidad Java usa `String` normal. `V9` ya corrigió el mismo problema para `solicitudes_matricula_publica.token_hash`, pasándolo de `CHAR(64)` a `VARCHAR(64)`. Debe aplicarse el mismo criterio al SHA-256 para evitar padding y diferencias de comparación.

4. **Tipos temporales inconsistentes.** Las tablas `usuarios`, `sesiones` y `auditoria_cambios` usan `TIMESTAMPTZ`, pero sus entidades principales usan `LocalDateTime`; el resto del modelo usa `OffsetDateTime`. Esto puede perder el offset al leer/escribir y producir errores de auditoría o expiración en servidores con otra zona horaria. Debe unificarse a `OffsetDateTime` o justificarse explícitamente con UTC y configuración Hibernate.

### Importantes

5. **Longitudes de nombres y apellidos posiblemente sobredimensionadas.** `nombres VARCHAR(100)`, `apellido_paterno VARCHAR(80)` y `apellido_materno VARCHAR(80)` aparecen en `estudiantes`, `apoderados` y solicitudes públicas. Los DTO también aceptan exactamente esos máximos. No es un error técnico por sí mismo, pero si la regla real del colegio es nombre máximo 20, hoy la validación de API y la base permiten más de lo requerido. Debe definirse el máximo oficial antes de reducirlo; una reducción debe aplicarse simultáneamente en DTO, entidad y DDL.

6. **Otros campos que necesitan confirmación funcional de longitud:** `competencias.nombre 150`, `comunicados_oficiales.titulo 150`, `sesiones_refuerzo.tema 150`, `aulas.nombre 100`, `direcciones 200`, observaciones 500/4000 y URLs 255/1000. No deben reducirse solo por intuición porque pueden truncar datos reales.

7. **Aula de refuerzo sin relación con `aulas`.** `sesiones_refuerzo.aula_asignada VARCHAR(30)` no referencia el catálogo `aulas`, por lo que admite códigos inexistentes y no impide reservar el mismo ambiente en horarios solapados.

8. **RLS/grants de la tabla nueva de documentos requieren verificación.** V11 habilita RLS sobre `solicitudes_matricula_documentos`, pero no revoca explícitamente privilegios a `anon` y `authenticated`. V2/V4 lo cubren solo si el mismo rol ejecutó las migraciones y sus privilegios por defecto siguen alineados. Debe verificarse en Supabase con `pg_class`, `pg_policies` y `information_schema.role_table_grants`.

9. **Oferta institucional hardcodeada a 2026.** V11 inserta año, periodos, grados, aulas y secciones de 2026. Es válido como seed inicial, pero no es reutilizable para 2027 y puede bloquear despliegues repetidos si se espera parametrización anual.

### Menores / mantenimiento

10. **V3 y V10 tienen `ALTER TABLE` sin `IF NOT EXISTS`.** Flyway evita repetirlas por historial, por lo que no fallan en el flujo normal; sí dificultan reconstrucciones manuales o scripts consolidados idempotentes.

11. **Índices heredados redundantes.** Existen índices cubiertos por restricciones únicas, por ejemplo en matrícula-apoderado, obligaciones por matrícula y comprobante por pago. No rompen integridad, pero aumentan costo de escritura y almacenamiento.

12. **El encabezado de V1 todavía dice 33 tablas.** El estado final tiene más tablas por `series_comprobante`, `aulas` y las tablas de matrícula pública/documentos.

## Relaciones verificadas como correctas

- Matrícula mantiene coherencia con año y sección mediante FK compuesta.
- Calificaciones mantienen coherencia con matrícula, periodo, asignación, área y competencia mediante FKs compuestas.
- Pagos apuntan a obligaciones y comprobantes a pagos; existe unicidad para idempotencia.
- Solicitud pública mantiene FKs hacia año/sección, estudiante, apoderado y matrícula.
- Documentos públicos usan PK compuesta por solicitud y tipo y FK `ON DELETE CASCADE`.
- Aulas se relacionan con secciones mediante `aula_id` y restricción por año.

## Validación ejecutada

- Se revisaron todas las migraciones V1–V11.
- Se compararon tablas con entidades JPA y DTOs de entrada.
- Se revisaron tipos numéricos, temporales, textos, checks, únicas, FKs, índices, triggers y RLS.
- No se modificó ninguna migración ni `.env`.
- No fue posible ejecutar validación contra PostgreSQL local porque `127.0.0.1:55439` estaba apagado.

## Decisiones que necesito antes de corregir

1. Máximo oficial para nombres, apellidos, nombres de aulas, competencias, temas y direcciones.
2. Si se mantiene soporte únicamente DNI de 8 dígitos en matrícula pública o también CE/PASAPORTE.
3. Si el usuario de práctica `fq94289@gmail.com` debe eliminarse completamente de Flyway.
4. Si `sesiones_refuerzo` debe usar `aula_id` como FK.
5. Si se desea que prepare una nueva versión completa del script consolidado V1–V11.
