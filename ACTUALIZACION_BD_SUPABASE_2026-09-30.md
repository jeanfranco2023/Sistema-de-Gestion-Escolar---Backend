# Actualización de Supabase — 2026-09-30

Estado real: esquema `public` actualizado desde V11 hasta **V17** mediante Flyway.

- V12: longitudes de texto, períodos activos y relación de refuerzo con aulas.
- V13: importe de matrícula positivo y valor predeterminado S/ 350; conserva importes históricos.
- V14: normalización de aulas, integridad e índices.
- V15: nombres SQL españoles, límites de atributos y comprobaciones sin truncado.
- V16: retiro de seis UUID adicionales sin uso y descripción de roles.
- V17: corrige una diferencia previa en dos CHECK que todavía admitían series F para facturas. Conserva los prefijos B/E del modelo acordado. La comprobación previa no encontró series incompatibles.

Las migraciones existentes V1–V11 validaron sus checksums. V12–V16 se aplicaron agrupadas transaccionalmente. V17 se aplicó posteriormente y las 17 versiones quedaron validadas. No se usó repair, clean, baseline ni el consolidado para reemplazar tablas existentes.

## Verificación

- 37 tablas de negocio, 307 atributos y 222 restricciones, excluyendo flyway_schema_history.
- Columnas reales idénticas al inventario verificado.
- Restricciones reales idénticas al inventario tras V17.
- Migraciones V1–V17 y consolidado equivalentes en PostgreSQL real; límites 20/21 y 500/501, rechazo sin truncado y trigger de fecha verificados. Esquemas temporales revertidos.
- Conteos de las 37 tablas conservados antes/después de V17. V12–V16 no contienen DELETE ni TRUNCATE; no se truncaron valores.
- Respaldo previo de los valores retirados: `target/respaldo-depuracion-1790812100055.tsv`, ocho entradas. Contiene identificadores y descripciones; no publicar ni copiar a Obsidian.
- No se modificó `.env` ni se hicieron commit/push.
- Las 106 pruebas y seis pruebas reales de persistencia corresponden a la verificación previa V16. En esta actualización se validó directamente el esquema real y la equivalencia del consolidado; no se repitió la suite completa.

Los dos SQL consolidados se actualizaron a V1–V17. Son para bases vacías; no deben ejecutarse sobre este esquema ya migrado.

Evidencia local: `target/actualizacion-public-v16.log`, `target/actualizacion-public-v17.log` y `target/verificacion-esquema-v17.log`.
