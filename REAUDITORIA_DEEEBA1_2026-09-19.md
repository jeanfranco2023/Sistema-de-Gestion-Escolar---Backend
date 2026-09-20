# Reauditoría integral del backend

**Fecha:** 2026-09-19  
**Rama:** `main`  
**Commit auditado:** `deeeba1`  
**Veredicto:** **APROBADO CON OBSERVACIONES MENORES**  
**Calificación:** **9.4/10**

## Evidencia ejecutada

- `mvnw.cmd clean verify`: **BUILD SUCCESS**.
- Surefire: **79 pruebas**, 0 fallos, 0 errores y 0 omitidas, distribuidas en 24 suites.
- Pruebas PostgreSQL reales: migraciones V1–V4, persistencia, concurrencia y regresión N+1 aprobadas.
- JaCoCo: **62.91 % de instrucciones** y **46.55 % de ramas**. Supera los mínimos configurados de 60 % y 35 %.
- El `.env` no fue leído ni modificado.

> El mensaje del commit indica 82 pruebas y 63.25 %, pero la ejecución limpia de este commit produjo 79 pruebas y 62.91 %. El resultado sigue cumpliendo las puertas de calidad.

## Cumplimiento verificado

### Base de datos y persistencia

- No se detectaron `nativeQuery = true`, `createNativeQuery` ni `JdbcTemplate`; las consultas declaradas son JPQL.
- Flyway validó y aplicó correctamente las cuatro migraciones sobre PostgreSQL.
- La serie de boletas se obtiene con `PESSIMISTIC_WRITE`; el correlativo se incrementa y persiste dentro de la misma transacción.
- El pago también se bloquea antes de emitir y existe control idempotente por pago.
- Las pruebas de carga/regresión confirman los caminos críticos tratados contra N+1.
- El modelo contempla aulas y secciones; no se encontró un concepto de factura agregado indebidamente al flujo actual de boletas.

### Arquitectura y dominio

- Se conserva la separación por módulos y capas `domain`, `application` e `infrastructure`.
- Los servicios dependen de puertos; `TesoreriaService` usa `EmitirComprobanteUseCase` y no una implementación concreta.
- Las mutaciones críticas están encapsuladas en métodos de dominio, incluyendo pagos, obligaciones, series y comprobantes.
- No se detectaron imports comodín.

### Seguridad

- El webhook usa HMAC-SHA256, `MessageDigest.isEqual`, ventana anti-replay de 300 segundos y rotación mediante secreto anterior.
- El pago recibido se consulta nuevamente en Mercado Pago y se validan referencia, estado, moneda y monto antes de registrarlo.
- La preferencia usa `X-Idempotency-Key` y las URLs de retorno exigen HTTPS, host permitido y ausencia de credenciales.
- Anti-IDOR: un usuario exclusivamente APODERADO debe tener ficha vinculada y ser responsable económico del estudiante.
- Las autoridades JWT se reconstruyen desde los roles actuales en base de datos.
- Swagger está deshabilitado en producción y el workflow de CI ejecuta OWASP Dependency-Check sin ignorar fallos.

## Observaciones pendientes

1. **Paginación — prioridad media.** Persisten numerosos `findAll(Sort...)` y `UsuarioService` devuelve la colección completa. En volúmenes altos aumentarán memoria, tiempo de respuesta y carga de base de datos. Agregar `Pageable` a listados operativos y conservar listas completas solo para catálogos pequeños.
2. **Rate limiting distribuido — prioridad media.** `RateLimitingFilter` usa `ConcurrentHashMap`; limita por instancia y pierde estado al reiniciar. Para varias réplicas, mover contadores a Redis u otro almacén compartido. `behind-trusted-proxy=true` exige además que el backend solo sea accesible mediante un proxy confiable que reemplace `X-Forwarded-For`.
3. **Escaneo CVE — prioridad operativa.** La puerta OWASP está correctamente configurada en CI, pero esta ejecución local no produjo un informe actualizado de vulnerabilidades. El despliegue debe exigir que ese job termine correctamente y no solo `clean verify`.
4. **Compatibilidad local — prioridad baja.** La prueba local usó PostgreSQL 18.3 y Flyway avisó que su versión está certificada hasta PostgreSQL 16. CI usa PostgreSQL 16; mantener producción en la versión soportada o actualizar Flyway antes de migrar a PostgreSQL 18.
5. **Cobertura — prioridad baja.** La cobertura supera el gate, aunque 62.91 % todavía deja rutas sin verificar. Elevar progresivamente el mínimo, priorizando autorización, webhooks, reversos e idempotencia.

## Criterio para 10/10

- Paginar los listados que pueden crecer sin límite.
- Usar rate limiting compartido en despliegues con más de una instancia y restringir el acceso directo al backend.
- Obtener un Dependency-Check vigente y limpio en CI.
- Alinear la versión real de PostgreSQL con la versión soportada por Flyway.
- Aumentar cobertura de seguridad y concurrencia sin rebajar los gates.

El backend es apto para continuar hacia producción si se cumplen las condiciones operativas indicadas. Ninguna auditoría puede garantizar inmunidad ante cualquier vulneración; este veredicto refleja el código y las pruebas disponibles en el commit señalado.
