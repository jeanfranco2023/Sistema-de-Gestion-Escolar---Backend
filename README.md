# 🏫 Sistema de Gestión Escolar - Backend API
### Plataforma Web Integral para la I.E.P. Shuji Kitamura (Proyecto Integrador II)

[![Java 21](https://img.shields.io/badge/Java-21%20LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-6-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Mercado Pago](https://img.shields.io/badge/Mercado%20Pago-Checkout%20Pro-009EE3?style=for-the-badge&logo=mercadopago&logoColor=white)](https://www.mercadopago.com.pe/developers)
[![Architecture](https://img.shields.io/badge/Architecture-Hexagonal%20%2F%20DDD-00599C?style=for-the-badge)](https://alistair.cockburn.us/hexagonal-architecture/)

---

## 📌 Descripción General del Proyecto

Backend RESTful de nivel empresarial diseñado para la **centralización de procesos institucionales**, **evaluación formativa por competencias del Currículo Nacional (CNEB - MINEDU)**, **asistencia con conciliación biométrica (escáner facial en portería vs. aula)** y **recaudación digital automatizada de pensiones y matrículas escolares** para la institución educativa privada **I.E.P. Shuji Kitamura**.

Desarrollado bajo los principios de **Domain-Driven Design (DDD)** y un enfoque de **Monolito Modular con Arquitectura Hexagonal Pura (Ports & Adapters)**, garantizando desacoplamiento absoluto entre la lógica de negocio, los contratos de aplicación y las tecnologías de infraestructura y persistencia.

---

## 🏛️ Arquitectura Hexagonal y Principios de Diseño

El sistema está dividido en módulos autónomos (Mini-Hexágonos), donde cada módulo respeta una estricta separación de responsabilidades en tres capas concéntricas:

```text
               ┌────────────────────────────────────────────────────────┐
               │              INFRASTRUCTURE (Adaptadores)              │
               │  ┌──────────────────────────────────────────────────┐  │
               │  │           APPLICATION (Puertos y Casos)          │  │
               │  │  ┌────────────────────────────────────────────┐  │  │
               │  │  │            DOMAIN (Núcleo Puro)            │  │  │
               │  │  │  • Modelos POJO (Sin @Entity ni JPA)       │  │  │
               │  │  │  • Enums y Reglas de Negocio Inmutables    │  │  │
               │  │  │  • Excepciones de Dominio                  │  │  │
               │  │  └────────────────────────────────────────────┘  │  │
               │  │  • Casos de Uso (Input Ports / Use Cases)        │  │
               │  │  • Puertos de Salida (Output Ports)              │  │
               │  │  • DTOs Inmutables (Java Records + Validation)   │  │
               │  │  • Servicios de Aplicación (@Transactional)      │  │
               │  │  • Mappers Tipados (MapStruct)                   │  │
               │  └──────────────────────────────────────────────────┘  │
               │  • Controladores REST (@RestController + OpenAPI)      │
               │  • Repositorios JPA (100% JPQL / Métodos Derivados)    │
               │  • Entidades JPA (@Table, @Id, @Column)                │
               │  • Adaptadores HTTP (RestClient: Mercado Pago, RENIEC) │
               │  • Filtros JWT y Configuración de Seguridad            │
               └────────────────────────────────────────────────────────┘
```

### 🛡️ Guardas y Reglas Arquitecturales Automatizadas (`ArquitecturaTest.java`)
El backend cuenta con pruebas de arquitectura ejecutadas en cada build (`mvnw test`):
1. **Cero Consultas Nativas (0% SQL crudo):** Prohibido estrictamente el uso de `createNativeQuery`, `nativeQuery = true`, `java.sql.*` o `JdbcTemplate`. Todo acceso a datos se realiza vía **100% JPQL o métodos derivados de Spring Data JPA**.
2. **Aislamiento Hexagonal Estricto:** La capa `application` no importa clases de `infrastructure` ni `config`. La capa `domain` no importa frameworks de persistencia (`jakarta.persistence`, `org.springframework`, `org.hibernate`).
3. **Servicios Sin Comodines ni Setters de Negocio:** Prohibidos los imports con comodín (`.*`). Las transacciones se orquestan invocando métodos semánticos del modelo de dominio (`pago.aprobar()`, `comprobante.emitir()`, `estudiante.iniciarRegistroManual()`).
4. **Modelos de Dominio Estandarizados:** Todos los modelos POJO implementan `@Builder`, `@NoArgsConstructor` y `@AllArgsConstructor` de Lombok para garantizar compatibilidad con MapStruct y pruebas unitarias.

---

## 📦 Catálogo de Módulos Funcionales (Mini-Hexágonos)

El backend contiene **420 clases Java** organizadas de forma modular en:

### 1. Seguridad, Autenticación y Usuarios (`com.colegio.shuji.usuario`)
* **Autenticación Stateless:** Generación y validación de tokens JWT (HMAC-SHA256) con expiración configurable y rotación criptográfica de Refresh Tokens en base de datos.
* **Control de Acceso Basado en Roles (RBAC):** Roles institucionales diferenciados (`DIRECCION`, `SECRETARIA`, `DOCENTE`, `AUXILIAR`, `TUTOR`, `APODERADO`).
* **Ciclo de Vida de Cuentas:** Registro institucional, consulta de perfil propio (`GET /api/v1/usuarios/me`), listado filtrado por rol, activación/bloqueo de accesos y cambio seguro de contraseñas con BCrypt.

### 2. Matrícula y Control Dinámico de Vacantes (`com.colegio.shuji.matricula`)
* **Ficha Integral del Estudiante:** Expediente académico, vinculación jerárquica de apoderados y procedencia escolar.
* **Validación de Identidad con RENIEC:** Adaptador para consulta y contrastación de DNI peruano (8 dígitos obligatorios) con fallback seguro.
* **Aforos y Vacantes en Tiempo Real:** Control concurrente de cupos por sección (`cupo_maximo - matriculados_activos`) previniendo sobrecupos en periodos de alta demanda.

### 3. Tesorería, Pagos y Comprobantes (`com.colegio.shuji.tesoreria`)
* **Cronograma Automatizado:** Generación de obligaciones anuales (1 cuota de matrícula + 10 cuotas mensuales de pensión de marzo a diciembre) ligadas al año lectivo activo.
* **Pasarela de Pagos Mercado Pago:**
  * **Checkout Pro:** Generación de preferencias de pago dinámicas con importes en Soles (`PEN`), descripción del concepto y retorno automático a la plataforma.
  * **Webhooks / IPN:** Recepción segura de eventos de pago en `/api/v1/pagos/webhook/mercadopago`, verificación del estado fidedigno (`approved`) mediante `GET /v1/payments/{id}` y registro idempotente por `pasarela_transaccion_id`.
  * **Pasarelas de Respaldo:** Soporte integrado para caja física (efectivo/transferencia) y adaptadores para Culqi y Niubiz.
* **Trazabilidad y Emisión Automática de Boletas:**
  * Al confirmarse un pago aprobado, el sistema emite de manera atómica e inmediata el comprobante en la tabla `comprobantes_pago`.
  * Numeración correlativa automática y secuencial (`maxCorrelativo + 1`) por serie (`B001` para Boletas Electrónicas, `E001` para Recibos Internos de Caja).
  * Restricción `UNIQUE(serie, correlativo)` que garantiza cero duplicidades contables.

### 4. Evaluación Curricular CNEB (`com.colegio.shuji.evaluacion` y `curriculo`)
* **Enfoque por Competencias (MINEDU):** Evaluación formativa con escala cualitativa oficial: `AD` (Logro Destacado), `A` (Logro Esperado), `B` (En Proceso) y `C` (En Inicio).
* **Cálculo de Conclusiones Descriptivas:** Registro de evidencias pedagógicas por competencia y periodo académico.
* **Refuerzo Pedagógico:** Detección automática de estudiantes en riesgo pedagógico e inscripción en talleres de reforzamiento con control de asistencia.

### 5. Asistencia y Conciliación Biométrica Facial (`com.colegio.shuji.asistencia`)
* **Pase de Lista Rápido:** Registro ágil por aula para auxiliares y docentes (`PRESENTE`, `TARDANZA`, `FALTA_JUSTIFICADA`, `FALTA_INJUSTIFICADA`).
* **Ingesta Biométrica de Portería:** Procesamiento de marcas horarias generadas por escáneres faciales en los accesos de la institución.
* **Algoritmo de Conciliación Automática:** Cruce de datos entre portería y aula para identificar discrepancias en tiempo real (evasiones, tardanzas internas o suplantaciones).

### 6. Estructura Académica, Horarios y Comunicaciones (`academico`, `curriculo`, `comunicado`, `convivencia`)
* **Estructura Educativa:** Años lectivos, periodos de evaluación (bimestres/trimestres), niveles (Inicial, Primaria, Secundaria), grados y secciones.
* **Distribución de Carga Horaria:** Horarios escolares matriciales que previenen solapamiento de docentes y aulas físicas.
* **Convivencia Escolar y Cuaderno de Control:** Registro de incidencias disciplinarias, citaciones a apoderados y comunicados institucionales masivos o segmentados.

---

## 🗄️ Modelo de Datos y Persistencia PostgreSQL

El esquema de base de datos (`src/main/resources/db/migration/V1__schema_colegio_shuji.sql`) implementa:
* **33 tablas relacionales** en 3ra Forma Normal (3NF/BCNF).
* **Identificadores Públicos UUIDv4:** Cada tabla cuenta con identificadores `UUID` inmutables expuestos en la API pública para prevenir ataques de enumeración y scraping.
* **Tipos de Datos Especializados:** `CHAR(8)` para DNI peruano, `CHAR(9)` para teléfonos móviles peruanos, `NUMERIC(10,2)` para transacciones financieras en Soles (`PEN`), `JSONB` para auditoría y payloads externos.
* **11 Funciones PL/pgSQL y 15 Triggers:** Para actualización automática de timestamps, auditoría de cambios y sincronización del usuario autenticado vía `SupabaseAuditInterceptor`.
* **28 Índices B-Tree Estratégicos:** Sobre claves naturales, foráneas y campos de consulta masiva.

---

## 🛠️ Stack Tecnológico

| Componente | Tecnología | Versión | Propósito |
| :--- | :--- | :--- | :--- |
| **Lenguaje** | **Java** | 21 LTS | Lenguaje principal (Pattern Matching, Records, Virtual Threads ready). |
| **Framework** | **Spring Boot** | 3.3.x | Framework central de microservicios y APIs REST. |
| **Seguridad** | **Spring Security** | 6.x | Seguridad stateless con JWT (JJWT 0.12.6) y filtros por rol. |
| **ORM / Acceso** | **Spring Data JPA / Hibernate** | 6.x | Mapeo objeto-relacional con consultas 100% JPQL. |
| **Base de Datos** | **PostgreSQL** | 15+ / Supabase | RDBMS relacional transaccional ACID. |
| **Pool Conexiones** | **HikariCP** | 5.x | Pool de conexiones optimizado de alto rendimiento. |
| **Pasarela Pagos** | **Mercado Pago API** | v1 / Preferences | Integración nativa vía `RestClient` (Checkout Pro + IPN Webhooks). |
| **Mapeo de Objetos**| **MapStruct** | 1.6.x | Mapeo tipado en tiempo de compilación entre Dominios, Entidades y DTOs. |
| **Boilerplate** | **Lombok** | 1.18.x | Reducción de código repetitivo (`@Getter`, `@Builder`, etc.). |
| **Validación** | **Jakarta Bean Validation**| 3.0 | Validación declarativa de payloads de entrada (`@NotNull`, `@Size`, etc.). |
| **Documentación** | **SpringDoc OpenAPI** | 2.5.x | Documentación interactiva Swagger UI / OpenAPI 3. |
| **Testing** | **JUnit 5 / Mockito** | 5.x | Pruebas unitarias de dominio y guardas arquitecturales. |

---

## 🚀 Instalación y Despliegue Local

### Prerrequisitos
* **JDK 21 LTS** instalado y configurado en el `PATH` (`java -version`).
* **PostgreSQL 15+** (o proyecto Supabase) con la base de datos creada.
* Conexión a Internet para la descarga de dependencias de Maven.

### 1. Clonar el Repositorio
```bash
git clone https://github.com/jeanfranco2023/Sistema-de-Gestion-Escolar---Backend.git
cd Sistema-de-Gestion-Escolar---Backend
```

### 2. Configurar Variables de Entorno
Copia la plantilla `.env.example` a `.env` y completa los valores de conexión:
```bash
cp .env.example .env
```

Parámetros principales en `.env`:
```properties
# Base de Datos
SUPABASE_DB_HOST=localhost
SUPABASE_DB_PORT=5432
SUPABASE_DB_NAME=shuji_db
SUPABASE_DB_USER=postgres
SUPABASE_DB_PASSWORD=tu_password_local
SUPABASE_DB_SSLMODE=prefer

# JWT
JWT_SECRET=c2h1amlfa2l0YW11cmFfcHJveWVjdG9faW50ZWdyYWRvcl91dHBfMjAyNg==
JWT_EXPIRATION_MS=86400000

# Mercado Pago
MERCADOPAGO_PUBLIC_KEY=APP_USR-86474939-d202-45ae-a7f3-eaeeb7b5606a
MERCADOPAGO_ACCESS_TOKEN=tu_access_token_de_prueba
MERCADOPAGO_WEBHOOK_SECRET=tu_webhook_secret
MERCADOPAGO_RETURN_URL_HOSTS=localhost
```

### 3. Ejecutar la Suite de Pruebas Automatizadas
Verifica que la arquitectura y las reglas de dominio pasen al 100%:
```powershell
# En Windows PowerShell
.\mvnw.cmd test

# En Linux / macOS
./mvnw test
```

### 4. Iniciar la Aplicación
```powershell
# En Windows PowerShell
.\mvnw.cmd spring-boot:run

# En Linux / macOS
./mvnw spring-boot:run
```
El servidor iniciará en el puerto **`8080`** por defecto: `http://localhost:8080`.

---

## 📖 Documentación de la API (Swagger UI)

Una vez iniciado el backend, accede a la documentación interactiva y explorador de endpoints en:
* **Swagger UI:** `http://localhost:8080/swagger-ui.html`
* **Especificación OpenAPI (JSON):** `http://localhost:8080/api-docs`

---

## 🔒 Endpoints Clave y Flujo de Pago

### Autenticación y Usuarios
* `POST /api/v1/auth/login` - Autenticación con credenciales institucionales y generación de JWT.
* `POST /api/v1/auth/refresh` - Renovación de tokens mediante Refresh Token.
* `GET /api/v1/usuarios/me` - Consulta de información y roles del usuario autenticado.

### Tesorería y Mercado Pago
* `POST /api/v1/tesoreria/pagos/mercadopago/preferencia` - Generación de sesión Checkout Pro (devuelve `preferenceId` e `initPoint`).
* `POST /api/v1/pagos/webhook/mercadopago` - Webhook público para notificaciones instantáneas de Mercado Pago (IPN).
* `GET /api/v1/comprobantes/{id}` - Consulta de boletas de pago emitidas.
* `POST /api/v1/tesoreria/pagos/caja` - Registro de cobro presencial en efectivo o transferencia.

---

## 👥 Equipo y Créditos
* **Institución:** I.E.P. Shuji Kitamura
* **Curso:** Curso Integrador II: Sistemas (Universidad Tecnológica del Perú - UTP)
* **Desarrollo y Arquitectura:** Jean Franco & Antigravity (Google DeepMind)
