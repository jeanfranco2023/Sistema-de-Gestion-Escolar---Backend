# Manual de Operaciones, Observabilidad y Rotación Segura de Secretos

## 1. Métricas, Salud y Trazabilidad (Spring Boot Actuator & Prometheus)

El backend expone métricas y sondas de salud listas para monitoreo en producción (Prometheus, Grafana, Datadog o Kubernetes):

### Endpoints de Monitoreo
* **Health Probes:** `GET /actuator/health` (público para balanceadores / Kubernetes `livenessProbe` y `readinessProbe`).
  * Incluye validación de conectividad con PostgreSQL (`db` component) y espacio en disco.
* **Métricas Prometheus:** `GET /actuator/prometheus` (formato estándar para scraping de Prometheus / Grafana).
  * Histogramas de percentiles en `http.server.requests` (p95, p99) para detección temprana de degradación de latencia.
  * Métricas del pool HikariCP: `hikaricp.connections.active`, `hikaricp.connections.pending`, `hikaricp.connections.idle`.
* **Métricas JVM:** `jvm.memory.used`, `jvm.gc.pause`, `jvm.threads.live`.

### Trazabilidad y MDC (Mapped Diagnostic Context)
Todas las solicitudes registran en los logs la traza transaccional:
* `traceId` / `correlationId`: Para correlacionar webhooks de pasarela con la emisión de comprobantes.
* `userId`: Identificador del usuario autenticado en JWT para trazabilidad de auditoría.

---

## 2. Protocolo de Rotación Segura de Secretos (Zero Downtime)

### A. Rotación de `JWT_SECRET` (Firma de Tokens de Usuario)
1. **Paso 1 (Compatibilidad Dual):** Configurar el nuevo secreto en el gestor de secretos (AWS Secrets Manager, Supabase Vault, GCP Secret Manager) con prefijo de versión.
2. **Paso 2:** Desplegar actualización admitiendo validación con clave antigua y firma exclusiva con la nueva clave.
3. **Paso 3:** Una vez expirados los tokens activos (tiempo máximo `JWT_EXPIRATION_MS = 86400000`, 24h), retirar la clave antigua.

### B. Rotación de `MERCADOPAGO_WEBHOOK_SECRET`
1. **Generación en Mercado Pago:** Ir a la consola de Mercado Pago Developers -> Tu Aplicación -> Webhooks -> Generar nuevo secreto de firma.
2. **Actualización en Variables de Entorno:**
   ```bash
   MERCADOPAGO_WEBHOOK_SECRET=nuevo_secreto_hexadecimal
   ```
3. **Validación Automática:**
   * La clase `MercadoPagoProductionConfigurationValidator` validará en el inicio bajo perfil `prod` que la clave no esté vacía.
   * `MercadoPagoWebhookSignatureValidator` rechazará inmediatamente firmas de repetición (`> 300s`) y firmas incompatibles con código HTTP 401.

### C. Rotación de `MERCADOPAGO_ACCESS_TOKEN` y Llaves de API
1. Generar nuevo Access Token de producción en Mercado Pago.
2. Actualizar la variable de entorno `MERCADOPAGO_ACCESS_TOKEN`.
3. Reiniciar con estrategia de despliegue progresivo (Rolling Update) para evitar caídas de servicio.

---

## 3. Matriz de Control de Calidad y Pipeline CI/CD

El flujo de integración continua (`.github/workflows/ci.yml`) ejecuta 7 barreras de calidad automatizadas obligatorias antes de cualquier merge a `main`:

1. **Compilación estricta:** Java 21 LTS con validación de generadores MapStruct y Lombok.
2. **Reglas de Arquitectura:** Validación de que la capa de aplicación no importe infraestructura ni SQL nativo.
3. **Pruebas de Dominio Unitarias:** Pruebas atómicas aisladas de reglas de negocio.
4. **Pruebas Avanzadas de Seguridad:** HMAC-SHA256, Anti-Replay, Anti-IDOR, Open Redirect y Whitelist.
5. **Pruebas de Integración PostgreSQL 16:** Validaciones de schema DDL, triggers de base de datos y Flyway.
6. **Pruebas de Alta Concurrencia:** Bloqueo pesimista `PESSIMISTIC_WRITE` en series de comprobantes y control de vacantes.
7. **Pruebas de Carga N+1:** Verificación de procesamiento masivo en tiempo subsegundo con consultas batch.
