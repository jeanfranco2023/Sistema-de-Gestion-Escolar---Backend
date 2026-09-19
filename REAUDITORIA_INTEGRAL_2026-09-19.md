# Reauditoría integral del backend

**Proyecto:** Colegio Shuji Kitamura  
**Fecha:** 2026-09-19  
**Commit auditado:** `9000bf3` (`main`, sincronizado con `origin/main`)  
**Estado del árbol:** limpio  
**Alcance:** arquitectura, dominio, API, JPA, PostgreSQL/Flyway, concurrencia, rendimiento, seguridad, pagos, configuración, CI y pruebas.  
**Restricción respetada:** `.env` no fue leído ni modificado.

## Veredicto actualizado

**APROBADO CON OBSERVACIONES para despliegue controlado.** Los bloqueos críticos de la auditoría anterior fueron resueltos y la suite completa funciona con PostgreSQL real. Antes de exponerlo directamente a Internet conviene cerrar los riesgos operativos enumerados en este informe.

**Calificación: 8.8/10** (anterior: 7.4/10).

| Área | Nota | Estado |
|---|---:|---|
| Arquitectura hexagonal/SOLID | 9.1 | Cumple |
| Dominio | 9.0 | Cumple |
| PostgreSQL/Flyway | 9.2 | Cumple |
| JPA/rendimiento | 8.9 | Cumple con observaciones |
| Concurrencia/idempotencia | 9.4 | Cumple |
| Seguridad de aplicación | 8.6 | Cumple con observaciones |
| Pruebas y CI | 8.1 | Cumple con observaciones |
| Operación en producción | 7.8 | Requiere endurecimiento |

## Correcciones anteriores verificadas

1. **Elevación de privilegios cerrada.** El auto-registro ignora roles del cliente y asigna exclusivamente `APODERADO`. La creación administrativa con roles está separada y protegida con `hasRole('DIRECCION')`. Existe prueba negativa con roles `DIRECCION` y `ADMINISTRADOR`.
2. **Revocación mejorada.** Cada access token consulta que el usuario exista y continúe activo. La vigencia predeterminada bajó de 24 horas a 15 minutos.
3. **Auditoría transaccional corregida.** Se incorporó `TransactionalAuditAspect`, sincronización en adapters de escritura y prueba PostgreSQL que comprueba `usuario_id` en `auditoria_cambios`.
4. **Rate limiting incorporado.** Login, registro y refresh tienen límites por IP y respuesta 429, con prueba automatizada.
5. **Privilegios futuros de Supabase protegidos.** Flyway V4 revoca privilegios predeterminados de tablas, secuencias y rutinas para `anon` y `authenticated`.
6. **Cabeceras HTTP añadidas.** HSTS, `X-Content-Type-Options`, bloqueo de frames y CSP básica.
7. **PostgreSQL disponible.** Flyway validó las cuatro migraciones y las pruebas de persistencia, concurrencia y N+1 pasaron.

## Evidencia ejecutada

- `mvnw clean test -Dtest=ArquitecturaTest,DominioAuditTest,ReglasDominioTest,SeguridadAvanzadaTest`: **44/44**, BUILD SUCCESS.
- `mvnw test`: **53/53**, 0 fallos, 0 errores, BUILD SUCCESS.
- Arquitectura: 4 pruebas.
- Dominio: 13 pruebas.
- Reglas de dominio: 8 pruebas.
- Seguridad: 19 pruebas.
- Persistencia PostgreSQL/Flyway: 6 pruebas.
- Concurrencia PostgreSQL: 2 pruebas.
- Regresión N+1/carga: 1 prueba.
- Flyway: V1, V2, V3 y V4 validadas.
- SQL nativo en Java: **0 coincidencias**.
- Cobertura JaCoCo: **59.57% de instrucciones** y **38.78% de ramas**.
- `git diff --check`: limpio.

La afirmación del commit de “56/56” no coincide con Surefire: el resultado reproducido es **53/53**. No afecta el éxito, pero la documentación debe usar el conteo real.

## Fortalezas confirmadas

- Módulos ordenados de macro a micro: dominio, aplicación e infraestructura.
- Dominio sin dependencias Spring/JPA y servicios sin setters de mutación.
- Puertos de entrada/salida y adapters correctamente separados.
- `@Transactional` y `readOnly=true` aplicados en servicios.
- Relaciones `ManyToOne`/`OneToOne` con `LAZY` explícito.
- Batch prefetching para los focos N+1 detectados.
- `open-in-view=false`, `ddl-auto=validate` y batch JDBC.
- Integridad relacional, restricciones, índices y triggers sólidos.
- Aulas modeladas sin romper `aulaFisica`, con capacidad y exclusividad anual.
- Correlativos de boletas serializados mediante `PESSIMISTIC_WRITE`.
- Idempotencia en pagos, comprobantes y lotes biométricos.
- Mercado Pago con HMAC, comparación constante, timestamp anti-replay, consulta server-to-server y allowlist HTTPS.
- Anti-IDOR exige vínculo y responsabilidad económica.
- Secretos obligatorios y sin valores predeterminados en perfil `prod` mediante validadores fail-fast.
- Refresh tokens hasheados y rotados.
- RLS y revocación de Data API para Supabase.

## Observaciones pendientes

### Alta — rate limiting evadible mediante `X-Forwarded-For`

El filtro acepta el primer `X-Forwarded-For` si tiene formato IP, aunque no demuestra que la solicitud provenga de un proxy confiable. Un cliente con acceso directo puede rotar esa cabecera y evitar el límite. Además, el contador vive en memoria, por lo que se reinicia al desplegar y no se comparte entre réplicas.

**Recomendación:** normalizar IP en un reverse proxy confiable, eliminar cabeceras aportadas por el cliente y usar Redis/gateway para límites distribuidos. Limitar también webhooks y preferencias de pago.

### Media-alta — cambio de roles no se aplica inmediatamente

El filtro comprueba estado activo desde la base, pero construye authorities con los roles contenidos en el JWT. Quitar un rol a un usuario no retira ese privilegio hasta que expire el token, aunque ahora el máximo predeterminado es 15 minutos.

**Recomendación:** construir authorities desde los roles actuales de `Usuario`, o incluir una `securityVersion` y rechazar tokens con versión anterior.

### Media — escaneo OWASP no bloquea CI

El paso de Dependency-Check termina con `|| true`; cualquier CVE o fallo del escáner queda convertido en éxito. En la auditoría local, NVD respondió 403/404 y no existían datos locales, por lo que no hay certificación actual de vulnerabilidades de dependencias.

**Recomendación:** configurar `NVD_API_KEY`, caché de la base y eliminar `|| true`. Mantener el umbral CVSS 8 como puerta real del pipeline.

### Media — configuración productiva hereda logs sensibles y Swagger público

La configuración única mantiene DEBUG para la aplicación y SQL, TRACE para binders, y permite Swagger/OpenAPI. Esto puede exponer datos personales o financieros y aumentar el volumen de logs.

**Recomendación:** crear `application-prod.yml` con niveles INFO/WARN, Hibernate SQL/Binder desactivados y Springdoc deshabilitado o autenticado.

### Media — cobertura insuficiente para calificación máxima

JaCoCo genera informe, pero no tiene regla `check` ni umbral real, pese al comentario del `pom.xml`. La cobertura actual es 59.57% de instrucciones y 38.78% de ramas.

**Recomendación:** agregar cobertura dirigida a autorización por recurso, errores de integración, transacciones, webhooks duplicados, reversión, validación temporal y casos borde del dominio. Establecer inicialmente 65%/45% y elevar gradualmente.

### Media — listados completos sin paginación

Persisten múltiples usos de `findAll()` y respuestas `List`. Funcionan con el volumen actual, pero pagos, matrículas, incidencias, comunicaciones y biometría crecerán.

**Recomendación:** paginar datos operativos; mantener listas completas solamente en catálogos pequeños.

### Baja — imports comodín y deuda de legibilidad

Aunque el commit afirma “zero wildcards”, permanecen imports `*` en DTO, entities, ports, adapters y mappers. Los servicios principales están mejor delimitados, por lo que esto no rompe la arquitectura, pero contradice el objetivo de Clean Code. También quedan nombres REST genéricos `operacionN` en algunos controladores.

**Recomendación:** aplicar Spotless/Checkstyle y renombrar métodos por intención sin cambiar rutas ni contratos.

### Baja — versión de PostgreSQL de pruebas fuera de la matriz declarada

La suite se ejecutó contra PostgreSQL 18.3 y Flyway avisó que esta versión supera su soporte probado, mientras el proyecto declara PostgreSQL 16/Supabase.

**Recomendación:** fijar PostgreSQL 16 en desarrollo/CI o actualizar Flyway después de validar compatibilidad con PostgreSQL 18.

## Seguridad por categoría

| Riesgo | Estado |
|---|---|
| SQL injection | Adecuado: JPQL parametrizado, sin SQL nativo en Java |
| Broken access control | Mejorado; RBAC por endpoint y anti-IDOR financiero |
| Privilege escalation | Corregido en auto-registro |
| Autenticación | Adecuada; BCrypt, JWT firmado, refresh rotatorio |
| Revocación | Usuario inactivo inmediato; cambio de roles hasta 15 minutos |
| Webhook spoofing/replay | Adecuado para Mercado Pago |
| Open redirect/SSRF de retorno | Adecuado mediante HTTPS y allowlist exacta |
| Race conditions financieras | Adecuado con locks y restricciones únicas |
| DoS/fuerza bruta | Parcial: filtro local y XFF evadible |
| Exposición de secretos | Adecuada en repositorio y fail-fast de producción |
| Dependencias vulnerables | No certificado por fallo de actualización NVD |
| Logging de datos sensibles | Pendiente endurecimiento de perfil productivo |
| Acceso directo Supabase | Adecuado con RLS, REVOKE y default privileges |

## Para alcanzar 10/10

1. Hacer distribuido y proxy-aware el rate limiting.
2. Aplicar roles actuales o versionado de seguridad en cada token.
3. Convertir OWASP Dependency-Check en una puerta obligatoria de CI.
4. Crear perfil productivo con logs seguros y Swagger restringido.
5. Paginar listados operativos.
6. Elevar cobertura, especialmente ramas de seguridad y fallos transaccionales.
7. Eliminar imports comodín y nombres `operacionN`.
8. Fijar la misma versión PostgreSQL/Flyway en local, CI y producción.

