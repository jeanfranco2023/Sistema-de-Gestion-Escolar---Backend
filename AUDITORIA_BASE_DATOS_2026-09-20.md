# Auditoría exclusiva de base de datos

**Fecha:** 2026-09-20  
**Motor objetivo:** PostgreSQL / Supabase  
**Migraciones revisadas:** V1–V4  
**Alcance:** modelo, justificación, integridad, índices, concurrencia, seguridad y correspondencia JPA  
**Veredicto:** **APROBADA CON OBSERVACIONES**  
**Calificación de base de datos tras V5:** **9.2/10**

## Diagnóstico

La base está normalizada de forma razonable, las tablas responden a procesos reales del colegio y no existe una tabla de facturas. El modelo usa `comprobantes_pago` únicamente para boletas y recibos internos, conforme al alcance indicado. Las claves compuestas protegen bien la coherencia académica; los triggers protegen vacantes y saldos ante concurrencia; Flyway, RLS y las revocaciones aíslan la Data API de Supabase.

No se recomienda eliminar ninguna de las 35 tablas actuales. La migración V5 agregó los índices faltantes de claves foráneas. Permanecen como mejoras integrar las aulas de refuerzo con el catálogo `aulas`, retirar índices redundantes y corregir la documentación que todavía declara 33 tablas.

## Justificación de tablas, de macro a micro

### 1. Identidad, acceso y trazabilidad

| Tabla | Justificación | Resultado |
|---|---|---|
| `roles` | Catálogo institucional de perfiles y base del RBAC. | Correcta |
| `usuarios` | Identidad de acceso propia del backend, estado y credenciales. | Correcta |
| `usuario_roles` | Relación N:M entre usuarios y roles; evita columnas de rol repetidas. | Correcta |
| `sesiones` | Persistencia de refresh tokens, expiración, IP y dispositivo para revocación. | Correcta |
| `auditoria_cambios` | Historial inmutable lógico de cambios críticos y actor responsable. | Correcta; definir retención del JSONB |

### 2. Estructura institucional y académica

| Tabla | Justificación | Resultado |
|---|---|---|
| `anios_lectivos` | Delimita cada campaña escolar y su estado operativo. | Correcta |
| `periodos_academicos` | Divide el año en periodos de evaluación y permite su cierre. | Correcta |
| `niveles` | Catálogo de Inicial, Primaria y Secundaria. | Correcta |
| `grados` | Ordena los grados dentro de cada nivel. | Correcta |
| `aulas` | Catálogo estable de ambientes físicos, capacidad, ubicación y disponibilidad; necesario para reportes. | Correcta y necesaria |
| `secciones` | Grupo académico de un grado en un año, con cupo y aula asignada. | Correcta; `aula_fisica` queda redundante frente a `aula_id` |

### 3. Familias y matrícula

| Tabla | Justificación | Resultado |
|---|---|---|
| `apoderados` | Datos del adulto responsable y vínculo opcional con usuario. | Correcta |
| `estudiantes` | Ficha única del alumno, identidad SIAGIE y datos relevantes. | Correcta |
| `estudiante_apoderados` | Relación N:M que distingue parentesco, custodia, recojo y responsabilidad económica. | Correcta y esencial para anti-IDOR |
| `matriculas` | Vincula estudiante, año y sección, y conserva el ciclo de vida de matrícula. | Correcta |

### 4. Currículo y horarios

| Tabla | Justificación | Resultado |
|---|---|---|
| `areas_curriculares` | Catálogo de áreas por nivel educativo. | Correcta |
| `competencias` | Competencias CNEB ordenadas dentro de cada área. | Correcta |
| `asignaciones_docentes` | Determina qué docente enseña un área en una sección y año. | Correcta |
| `bloques_horarios` | Normaliza las franjas horarias y recreos. | Correcta |
| `horarios_seccion` | Programa asignaciones por día y bloque, evitando colisiones de sección y docente. | Correcta |

### 5. Tesorería

| Tabla | Justificación | Resultado |
|---|---|---|
| `conceptos_cobro` | Catálogo de matrícula, pensión, certificado u otros conceptos. | Correcta |
| `obligaciones_pago` | Cuenta por cobrar por matrícula, concepto y cuota, con saldo derivado. | Correcta |
| `pagos_transacciones` | Registro idempotente de pagos de caja y pasarela y su estado. | Correcta |
| `comprobantes_pago` | Emite boletas o recibos internos vinculados uno a uno al pago. No modela facturas. | Correcta |
| `series_comprobante` | Contador serializado por serie para correlativos sin carreras. | Correcta |

### 6. Asistencia y convivencia

| Tabla | Justificación | Resultado |
|---|---|---|
| `lotes_biometrico` | Cabecera e idempotencia de cada archivo biométrico importado. | Correcta |
| `marcas_biometrico_porteria` | Eventos crudos de entrada para trazabilidad y conciliación. | Correcta |
| `asistencias_aula` | Registro pedagógico diario por matrícula. | Correcta |
| `conciliaciones_asistencia` | Resultado diario entre portería y aula, con discrepancias y alertas. | Correcta |
| `incidencias_conductuales` | Historial de convivencia asociado a la matrícula vigente. | Correcta |

### 7. Evaluación y refuerzo

| Tabla | Justificación | Resultado |
|---|---|---|
| `calificaciones_cneb` | Nota cualitativa por matrícula, competencia y periodo, con coherencia académica compuesta. | Correcta |
| `sesiones_refuerzo` | Programa actividades de recuperación por periodo, área y docente. | Correcta con observación: usa texto para aula |
| `inscripciones_refuerzo` | Relaciona alumnos derivados, nota origen y asistencia a refuerzo. | Correcta |

### 8. Comunicación

| Tabla | Justificación | Resultado |
|---|---|---|
| `comunicados_oficiales` | Cabecera institucional del comunicado y remitente. | Correcta |
| `comunicado_destinatarios` | Entrega individual, lectura y acuse por apoderado. | Correcta |

## Integridad y concurrencia

- Las FK compuestas impiden mezclar año, sección, nivel, asignación, docente, área y competencia incompatibles.
- Los estados y dominios relevantes tienen `CHECK`; montos, fechas, cupos y horarios tienen validaciones de rango.
- Las restricciones únicas cubren matrícula anual, notas, destinatarios, eventos biométricos, transacciones y correlativos.
- El trigger de vacantes realiza incremento condicionado y evita sobrecupo concurrente.
- El trigger financiero bloquea la obligación con `FOR UPDATE`, evita sobrepago y mantiene el saldo.
- `series_comprobante` se bloquea desde JPA con `PESSIMISTIC_WRITE`; `UNIQUE (serie, correlativo)` añade defensa final.
- Los pagos y comprobantes impiden cambiar su imputación y reactivar registros revertidos o anulados.

## Seguridad Supabase

- V2 revoca tablas, secuencias y rutinas a `anon` y `authenticated`, y habilita RLS en todas las tablas existentes.
- V3 repite la protección para `aulas`.
- V4 revoca privilegios por defecto para objetos futuros creados por el mismo rol que ejecuta Flyway.
- Este diseño es adecuado porque la aplicación accede mediante conexión JDBC y la Data API no forma parte del flujo.
- El propietario de las tablas y roles con `BYPASSRLS` no quedan limitados por RLS; las credenciales JDBC deben pertenecer solo al backend y tener el mínimo privilegio posible.
- La protección coincide con el cambio actual de Supabase que separa exposición de Data API, `GRANT` y RLS.

## Hallazgos y correcciones recomendadas

### Prioridad alta

1. **Unificar aulas de refuerzo.** Reemplazar gradualmente `sesiones_refuerzo.aula_asignada VARCHAR(30)` por `aula_id REFERENCES aulas(id)`. Agregar una restricción que evite reservar la misma aula en fecha y bloque coincidentes. Esta es la principal inconsistencia del modelo actual.

### Prioridad media

2. **Índices de FK implementados.** V5 agregó 13 índices, incluyendo `horarios_seccion(anio_lectivo_id)` y `lotes_biometrico(importado_por_usuario_id)` además de los señalados en la auditoría original.
3. **Eliminar índices duplicados mediante V5.** `idx_estudiante_apoderado_est_apod`, `idx_estudiante_apoderados_estudiante_id`, `idx_obligaciones_pago_matricula_id` e `idx_comprobantes_pago_id` quedan cubiertos por una restricción única u otro índice con el mismo prefijo. No modificar V1–V4 ya aplicadas.
4. **Validar fechas también en la base.** La aplicación controla periodos dentro del año lectivo, pero el DDL solo comprueba inicio menor que fin. Si se admiten escrituras administrativas directas, añadir un trigger de coherencia temporal.

### Prioridad baja

5. **Retirar `secciones.aula_fisica` cuando termine la compatibilidad.** Hoy duplica el código disponible mediante `aula_id`; conservar ambos permite divergencia.
6. **Actualizar el encabezado de V1.** Declara exactamente 33 tablas, pero el esquema final tiene 35: 33 en V1, `series_comprobante` en V2 y `aulas` en V3.
7. **Definir retención de auditoría y sesiones.** Programar purga segura de sesiones expiradas y archivado de `auditoria_cambios`, que crecerá de forma continua.

## Verificación ejecutada

- Revisión estática completa de V1, V2, V3 y V4.
- Correspondencia comprobada: 35 tablas físicas de dominio, 34 entidades JPA más `usuario_roles` como tabla de unión; `flyway_schema_history` es adicional y la administra Flyway.
- La ejecución de `PersistenciaPostgresTest`, `ConcurrenciaPostgresTest` y `CargaYRegresionNPlusOneTest` no pudo iniciarse el 2026-09-20 porque la base desechable configurada en `127.0.0.1:55439` estaba apagada. Resultado: 9 errores de infraestructura, 0 fallos de aserción. No se usó el `.env`.

## Criterio de aprobación

La justificación funcional de las tablas es correcta. La base puede operar con el modelo actual, pero no alcanza 10/10 hasta integrar las aulas de refuerzo, depurar índices redundantes y volver a ejecutar las pruebas sobre PostgreSQL disponible. Las migraciones V1–V4 permanecieron inmutables; los índices nuevos están en V5.
