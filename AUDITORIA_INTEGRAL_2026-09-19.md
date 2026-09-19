# Auditoría integral del backend — Colegio Shuji Kitamura

**Fecha:** 2026-09-19  
**Estado auditado:** árbol de trabajo local sobre commit `57231f6`, incluyendo cambios sin commit  
**Alcance:** Java/Spring, arquitectura hexagonal, dominio, API, JPA, PostgreSQL/Flyway, concurrencia, rendimiento, seguridad, integraciones, configuración y pruebas.  
**Nota:** no se leyó ni modificó el contenido de `.env`.

## Veredicto

**Estado: NO APROBADO PARA PRODUCCIÓN pública.** La base arquitectónica es buena y los controles de pagos, concurrencia y persistencia muestran una mejora clara. Sin embargo, existe una vulnerabilidad crítica de elevación de privilegios en el registro público y faltan pruebas de integración ejecutables en el entorno actual. Ningún sistema puede garantizar resistencia ante “cualquier” vulneración; esta evaluación mide los controles visibles y verificables en el repositorio.

**Calificación actual: 7.4/10.**

| Área | Nota | Diagnóstico |
|---|---:|---|
| Arquitectura y SOLID | 8.7 | Separación por módulos y puertos consistente; quedan imports comodín y nombres genéricos en controladores. |
| Dominio | 8.8 | Entidades de dominio con comportamiento y mutaciones encapsuladas. |
| Base de datos | 8.3 | Buenas restricciones, índices, triggers, RLS y migraciones; falta validar V1→V3 sobre PostgreSQL real. |
| Persistencia/rendimiento | 8.0 | JPQL, LAZY y prefetch por lotes adecuados; falta paginación general y prueba N+1 ejecutable. |
| Concurrencia | 8.8 | Bloqueos pesimistas dirigidos y contador de series correcto. |
| Seguridad | 5.8 | Webhook, anti-IDOR y secretos de producción están bien encaminados; registro público permite solicitar roles privilegiados. |
| Pruebas/operación | 6.8 | 40 pruebas pasan; 8 de integración no arrancan por ausencia de PostgreSQL local. |

## Hallazgos prioritarios

### CRÍTICO — elevación de privilegios mediante registro público

`POST /api/v1/auth/register` está en la lista pública. El DTO acepta un conjunto `roles` suministrado por el cliente y `AuthService.register()` carga esos roles del catálogo y los asigna sin comprobar quién realiza la operación. Un usuario no autenticado puede solicitar `DIRECCION`, `SECRETARIA` u otro rol existente.

**Corrección obligatoria:** separar auto-registro y alta administrativa. El auto-registro debe asignar un rol fijo y de bajo privilegio decidido en servidor. La creación con roles debe vivir en un endpoint autenticado con `@PreAuthorize("hasRole('DIRECCION')")`. Añadir una prueba que intente registrar anónimamente un usuario con `DIRECCION` y espere 403 o que compruebe que recibe exclusivamente el rol permitido.

### ALTO — revocación tardía de access tokens y usuarios desactivados

`JwtAuthenticationFilter` confía en `userId`, username y roles contenidos en el access token y construye un principal `active=true` sin consultar el estado actual del usuario o una versión de sesión. Con una duración predeterminada de 24 horas, desactivar un usuario o cambiarle roles no invalida inmediatamente sus tokens ya emitidos.

**Mejora:** reducir el access token a 10–15 minutos y validar una versión de sesión/seguridad, `jti` revocable o estado de usuario para operaciones sensibles. Mantener refresh token rotatorio y almacenado como hash, que ya está bien implementado.

### ALTO — auditoría de actor no se sincroniza para la mayoría de transacciones

El filtro llama `SupabaseAuditInterceptor.setCurrentUserId()` antes de entrar al servicio; el interceptor retorna si no existe una transacción activa. Solo `AuthService` vuelve a fijar el actor dentro de su transacción. No se encontró un aspecto/interceptor transaccional que invoque `syncCurrentUserFromSecurityContext()` en los demás servicios. Por ello, los triggers pueden registrar `usuario_id = NULL` en operaciones autenticadas.

**Mejora:** crear un aspecto ejecutado dentro de la transacción, o fijar `set_config(..., true)` al comienzo de cada caso de uso mediante un advice con orden posterior al interceptor transaccional. Añadir prueba PostgreSQL que verifique el actor real en `auditoria_cambios`.

### ALTO — pruebas PostgreSQL obligatorias no son reproducibles automáticamente

Surefire fuerza `shuji.integration=true`, pero las pruebas esperan PostgreSQL en `127.0.0.1:55439` y el repositorio no levanta esa dependencia. El resultado completo fue **48 pruebas: 40 correctas, 8 errores de infraestructura**. No se pudo certificar Flyway V1→V3, triggers, locks concurrentes ni límites de consultas N+1 con base real.

**Mejora:** usar Testcontainers PostgreSQL o un servicio PostgreSQL en CI, aplicar Flyway desde cero y ejecutar concurrencia/N+1 de forma determinista. Separar unitarias e integración con perfiles Maven si se desea una ejecución local rápida.

### MEDIO — ausencia de rate limiting y protección contra abuso

No se encontró limitación de intentos en login, refresh, registro ni webhooks. Esto permite fuerza bruta, enumeración indirecta, creación masiva de cuentas y consumo de conexiones/API externas.

**Mejora:** rate limiting por IP y cuenta en gateway o Bucket4j/Redis; bloqueo progresivo de login; límites específicos para webhooks y preferencias; métricas y alertas por respuestas 401/403/429.

### MEDIO — listados sin paginación

Numerosos adapters usan `findAll(Sort.by("id"))` y varios puertos retornan `List`. Cuando crezcan matrículas, pagos, comunicaciones, incidencias o marcas biométricas, aumentarán memoria, latencia y duración de transacciones.

**Mejora:** introducir `Pageable`/cursor en recursos operativos y mantener listas completas solo para catálogos pequeños como roles, niveles o aulas.

### MEDIO — logging excesivo por defecto

La configuración base activa DEBUG para la aplicación, DEBUG para SQL y TRACE para binders. Puede exponer DNI, correos, referencias de pagos u otros datos personales en logs y generar volumen elevado.

**Mejora:** mover SQL/binders a perfil `dev`; producción debe usar INFO/WARN, enmascarado de datos, correlación de solicitudes y retención definida.

### MEDIO — documentación API pública y configuración de cabeceras

Swagger/OpenAPI queda público en todos los perfiles. CORS acepta cualquier cabecera y credenciales, aunque limita orígenes configurados. No se observan cabeceras explícitas CSP/HSTS/Permissions-Policy; algunas pueden delegarse al proxy, pero deben documentarse y comprobarse.

**Mejora:** desactivar o proteger Swagger en producción, fijar cabeceras permitidas, habilitar HSTS detrás de HTTPS y documentar controles del reverse proxy.

### MEDIO — IP de cliente confiada sin política de proxy

`AuthController` usa directamente el primer valor de `X-Forwarded-For`. Un cliente puede falsificarlo si la aplicación es accesible sin un proxy confiable.

**Mejora:** configurar `server.forward-headers-strategy` y aceptar cabeceras reenviadas únicamente desde proxies conocidos; en otro caso usar `remoteAddr`.

### BAJO — deuda de Clean Code

Se encontraron imports comodín, principalmente en controladores/repositorios, y métodos REST llamados `operacion0`, `operacion1`, etc. Esto reduce legibilidad y hace más difícil rastrear auditorías y métricas.

**Mejora:** imports explícitos y nombres por intención (`crearSeccion`, `registrarPagoCaja`, `emitirComprobante`). No requiere cambiar rutas ni contratos.

## Controles que cumplen

- Estructura modular `domain/application/infrastructure` y dependencia hacia puertos.
- Prueba arquitectónica confirma ausencia de dependencias Spring/JPA en dominio y ausencia de setters en servicios.
- No se encontró `nativeQuery=true`, `createNativeQuery`, `JdbcTemplate` ni acceso JDBC en código de aplicación; las consultas son JPQL o derivadas.
- Relaciones `ManyToOne`/`OneToOne` auditadas con carga LAZY explícita.
- Servicios críticos usan consultas batch (`IN`, rango de fechas y agrupación en memoria) para los focos N+1 conocidos.
- `open-in-view=false`, `ddl-auto=validate`, batch JDBC y orden de inserts/updates.
- Restricciones, claves foráneas, unicidad e índices cubren las invariantes principales.
- Índice individual de `marcas_biometrico_porteria(fecha_hora)` presente.
- Catálogo `aulas` conserva `aula_fisica` por compatibilidad y agrega FK y exclusividad anual.
- Idempotencia biométrica mediante hash de lote e índice único parcial para marcas válidas.
- Correlativos de comprobantes mediante `series_comprobante` y `PESSIMISTIC_WRITE`; evita `max+1` y el estado abortado por colisiones.
- Solo se admiten series de boleta y nota electrónica (`B`/`E`); no existe serie de factura `F`.
- Pago bloquea la obligación y usa clave idempotente única; comprobante tiene unicidad por pago y por serie/correlativo.
- Anti-IDOR de Mercado Pago exige vínculo del apoderado y `esResponsableEconomico=true`.
- URLs de retorno: HTTPS, absolutas, sin user-info y host en allowlist exacta.
- Webhook Mercado Pago: HMAC-SHA256, `MessageDigest.isEqual`, ventana anti-replay de ±300 segundos, rotación de secreto y consulta posterior a la API de Mercado Pago.
- Validadores del perfil `prod` rechazan secretos predeterminados o ausentes.
- Contraseñas con BCrypt; refresh tokens persistidos como SHA-256 y rotados.
- `.env` está ignorado por Git y no aparece versionado.
- RLS habilitado y permisos revocados para roles `anon` y `authenticated` de Supabase.
- Actuator de métricas protegido por roles y detalles de salud condicionados a autorización.

## Base de datos

El modelo contiene 33 tablas en V1, más `series_comprobante` en V2 y `aulas` en V3. La separación funcional es coherente: identidad/RBAC, calendario académico, familia/matrícula, currículo, tesorería, asistencia, convivencia, evaluación/refuerzo y comunicaciones.

Observaciones:

1. V2 revoca permisos y activa RLS a las tablas existentes; V3 repite la protección para `aulas`. Conviene agregar `ALTER DEFAULT PRIVILEGES` para que futuras tablas/secuencias nazcan sin permisos de Data API.
2. La unicidad `(anio_lectivo_id, aula_id)` impide dos secciones en el mismo ambiente durante todo el año. Es correcta si cada aula pertenece a una sola sección; si se compartirán aulas por turno, la restricción necesitará turno o franja horaria.
3. La capacidad del aula se valida en dominio al crear la sección. La base no puede comprobar directamente `secciones.cupo_maximo <= aulas.capacidad`; una prueba concurrente y una regla transaccional deben garantizarlo.
4. La migración V3 transforma aulas vacías en códigos `SIN-ASIGNAR-*`; es segura para conservar filas, pero estos registros deben revisarse operacionalmente después del despliegue.
5. No se verificó el plan de ejecución (`EXPLAIN ANALYZE`) contra datos representativos ni estadísticas reales de Supabase.

## Arquitectura y rendimiento

La arquitectura hexagonal se respeta de forma consistente. Los servicios dependen de puertos, los adapters concentran Spring Data/HTTP y los modelos contienen operaciones de negocio. El uso de bloqueos pesimistas está dirigido a métodos `bloquearPorId` y al contador de series; no se observó que todos los `findById` normales bloqueen.

Los focos N+1 previamente señalados están corregidos a nivel de implementación. Aun así, “cero N+1” no puede certificarse de forma absoluta solo con búsqueda estática: requiere ejecutar `CargaYRegresionNPlusOneTest` con PostgreSQL y revisar presupuestos de consultas. También conviene paginar los listados generales y probar volúmenes de 10k/100k registros.

## Seguridad de integraciones y pagos

Mercado Pago presenta controles adecuados: idempotencia, verificación server-to-server del pago, moneda PEN, referencia de obligación, monto validado por el dominio, firma temporal del webhook y allowlist de retornos. La emisión de boleta es idempotente y el correlativo está serializado por fila.

Riesgos residuales:

- Si el secreto webhook queda vacío fuera del perfil `prod`, Mercado Pago acepta eventos sin firma. Debe asegurarse que despliegues reales siempre activen `prod`.
- Culqi/Niubiz usan un secreto genérico y no se observó firma específica del proveedor ni anti-replay equivalente.
- La clave de idempotencia de creación de preferencia deriva de obligación y saldo; es estable, pero debe definirse cómo regenerar una preferencia expirada sin reutilizar indefinidamente la misma respuesta.
- Debe añadirse reconciliación programada para pagos aprobados cuyo webhook no llegue o falle después del cobro.

## Dependencias

OWASP Dependency-Check está configurado con fallo desde CVSS 8. El análisis se intentó, pero NVD respondió 403/404 y la herramienta no tenía datos locales; por tanto, **no hay certificación actual de CVE**. Configurar `NVD_API_KEY`, caché compartida en CI y actualizar Dependency-Check. Spring Boot 3.3.4 y componentes fijados deben mantenerse mediante una política periódica de actualizaciones y Dependabot/Renovate.

## Evidencia de pruebas

- Suite selectiva: **40 pruebas, 0 fallos, 0 errores — BUILD SUCCESS**.
- Suite completa: **48 pruebas, 0 fallos, 8 errores — BUILD FAILURE**.
- Causa de los 8 errores: conexión rechazada a PostgreSQL `127.0.0.1:55439`; no son aserciones funcionales fallidas.
- `git diff --check`: sin errores de whitespace.
- Búsqueda estática: cero SQL nativo en Java.
- Dependency-Check: inconcluso por NVD 403/404 y ausencia de base local.

## Plan para llegar a 10/10

1. Cerrar inmediatamente el registro público con roles elegidos por cliente.
2. Sincronizar el actor de auditoría dentro de todas las transacciones y probarlo en PostgreSQL.
3. Levantar PostgreSQL automáticamente con Testcontainers/CI y dejar 48/48 pruebas verdes.
4. Añadir rate limiting, bloqueo progresivo y pruebas de abuso para auth/webhooks.
5. Implementar revocación/versionado de access tokens y acortar su vigencia.
6. Ejecutar Dependency-Check con NVD API y corregir CVE altas/críticas.
7. Paginar listados de datos crecientes y ejecutar pruebas de carga/N+1 con presupuestos de consultas.
8. Endurecer perfil productivo: logs, Swagger, cabeceras HTTP, proxies confiables y secretos obligatorios.
9. Añadir reconciliación de pagos, pruebas de caída entre pago y boleta y pruebas concurrentes sostenidas.
10. Agregar `ALTER DEFAULT PRIVILEGES` y verificar migraciones V1→V3 sobre una copia anonimizada antes del despliegue.

