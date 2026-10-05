# Auditoría integral de base de datos — Colegio Shuji Kitamura

**Fecha:** 2026-09-24  
**Alcance:** lectura estática de Flyway V1–V12, esquema consolidado y mapeos JPA relevantes. No se consultó la instancia real de Supabase ni se leyeron credenciales de env.  
**Veredicto:** **OBSERVADO — 7/10.** La base tiene buenas restricciones e integridad, pero quedan riesgos de seguridad y consistencia que impiden aprobarla sin reservas para producción.

## Cobertura revisada

El consolidado contiene 37 tablas y 55 claves foráneas declaradas. Revisé tipos de datos, claves primarias y foráneas, índices, restricciones, disparadores, RLS, migraciones de seguridad y mapeos JPA que afectan la validación de esquema. La compilación limpia del backend terminó con BUILD SUCCESS; no se ejecutaron pruebas ni consultas contra una base real.

## Puntos correctos

- La estructura cubre seguridad, calendario académico, matrícula, currículo, asistencia, evaluación, tesorería y comunicaciones. No hay tabla de facturas. comprobantes_pago modela boletas y recibos internos.
- Las relaciones compuestas protegen coherencia entre año, sección, nivel, área, asignación docente, competencia y calificación. Las restricciones CHECK y UNIQUE cubren estados, importes, DNI, horarios, cuotas, idempotencia y correlativos.
- BIGSERIAL se usa en flujos de crecimiento alto; SMALLSERIAL queda en catálogos acotados. Los importes usan NUMERIC, las fechas globales TIMESTAMPTZ, los datos estructurados JSONB y los archivos documentales se referencian en Storage en vez de guardarse como binarios en PostgreSQL.
- El control de vacantes usa una actualización condicional atómica. Los pagos bloquean la obligación antes de actualizar acumulados; el comprobante bloquea el pago y la aplicación bloquea la serie. La restricción única de serie/correlativo es una última barrera ante duplicados.
- Los estados activo/cerrado y las nuevas longitudes de V12 están alineados entre DDL, entidad y DTO para los campos revisados. La migración no trunca ni elimina datos: aborta ante textos incompatibles o aulas sin correspondencia.
- Flyway gestiona el esquema; Hibernate valida en vez de alterar DDL, y open-in-view está desactivado. Las migraciones V2–V4 revocan acceso directo de anon/authenticated y activan RLS en las tablas existentes entonces; V3 y V11 protegen explícitamente aulas y documentos.

## Hallazgos y mejoras pendientes

### Prioridad alta

1. **Pago de matrícula S/ 350 en sandbox — corregido localmente; V13 pendiente de aplicar.** El servicio usa el importe configurable, valida el pago confirmado contra ese valor y solo acepta credenciales TEST con enlace sandbox.
2. **Credencial fija de administrador en V6/V7.** V6 crea una cuenta conocida y V7 incluye la contraseña de práctica en texto de comentario y fija su hash. Rotar/deshabilitar esa cuenta y evitar que nuevas instalaciones reciban una credencial compartida. No editar migraciones ya aplicadas: resolver con una migración posterior y procedimiento seguro. El script consolidado sí omite esa cuenta/contraseña.
3. **TIMESTAMPTZ mapeado a LocalDateTime.** UsuarioEntity, SesionEntity y AuditoriaCambiosEntity usan LocalDateTime para columnas TIMESTAMPTZ y construyen valores con LocalDateTime.now(). Puede perderse la zona y puede fallar la validación de tipos de Hibernate; usar Instant/OffsetDateTime y verificar contra PostgreSQL real.
4. **CHAR(1) de género no alineado en solicitud pública.** V8 define genero_estudiante CHAR(1); SolicitudMatriculaPublicaEntity lo mapea como enum STRING sin JdbcTypeCode(CHAR), mientras que el campo equivalente en EstudianteEntity sí declara CHAR explícitamente. Alinear el mapeo o el tipo DDL y probar ddl-auto=validate.

### Prioridad media

5. **RLS incompleto en una tabla con datos personales.** El barrido de V2 no incluye solicitudes_matricula_publica porque V8 la crea después; V11 sí activa RLS en solicitudes_matricula_documentos. V4 revoca privilegios predeterminados, pero conviene añadir RLS fail-closed y confirmar privilegios efectivos en Supabase. La tabla contiene DNI, nacimiento, dirección y correo.
6. **Cinco claves foráneas de solicitudes_matricula_publica sin índice de soporte:** anio_lectivo_id, estudiante_id, apoderado_id, matricula_id y (seccion_id, anio_lectivo_id). V8 sí crea índices por DNI activo, estado/fecha y expiración, pero no para esas relaciones. Añadir índices en una migración nueva, priorizando sección y año; validar su coste con datos reales.
7. **Índices redundantes que elevan escritura y almacenamiento:** idx_estudiante_apoderado_est_apod repite UNIQUE(estudiante_id, apoderado_id); idx_estudiante_apoderados_apoderado_id repite idx_estudiante_apoderados_apod; idx_estudiante_apoderados_estudiante_id repite el prefijo de esa UNIQUE; idx_obligaciones_pago_matricula_id repite idx_obligaciones_matricula; idx_comprobantes_pago_id repite el índice creado por UNIQUE(pago_transaccion_id). Confirmar uso con pg_stat_user_indexes y retirar mediante migración controlada.
8. **Cascadas incompatibles con una política universal de conservación.** Permanecen ON DELETE CASCADE en usuario_roles, sesiones, competencias por área y documentos de solicitud por solicitud. Las dos primeras pueden ser limpieza intencional de seguridad; las últimas pueden borrar historial curricular o metadatos de documentos. Definir por entidad si corresponde RESTRICT, inactivación o eliminación explícita.
9. **Series y comprobantes no están vinculados por FK.** comprobantes_pago acepta series con patrón B/E y series_comprobante siembra B001 y E001, aunque tipo_comprobante solo admite BOLETA y RECIBO_INTERNO y el proyecto no contempla facturas. La aplicación exige una serie configurada, pero el DDL permite cualquier serie con formato válido. Confirmar si E001 se elimina o se reserva para otro comprobante y después agregar FK/validación coherente.
10. **Sala duplicada en secciones.** secciones conserva aula_fisica además de aula_id; V3 sincroniza ambos durante la migración, pero la BD no impide que diverjan en escrituras posteriores. Mantener una sola fuente o agregar una regla de sincronización cuando se cambie el contrato.

### Verificaciones operativas antes de producción

- V12 aún no se ha ejecutado contra Supabase. Debe comprobarse si existen apellidos mayores a 20, nombres de solicitudes mayores a 20, títulos/temas/nombres fuera del nuevo límite y códigos de aula faltantes o ambiguos. V12 aborta sin truncar ni borrar.
- Confirmar en catálogos de PostgreSQL que RLS esté activo y que anon/authenticated no tengan privilegios; verificar también que el bucket de documentos sea privado y que el backend firme las descargas.
- Revisar pg_stat_user_indexes y EXPLAIN (ANALYZE, BUFFERS) con volumen representativo. La auditoría estática no demuestra latencia ni selectividad.
- Definir retención para auditoria_cambios: sus triggers guardan filas OLD/NEW completas en JSONB. Controlar acceso, retención, crecimiento y minimización de PII/payloads.
- El estado de no borrado no es global: hay entidades con activo, otras con cerrado/estado y las cascadas anteriores. Mantener la regla en servicios y permisos, no confiar solo en convenciones de interfaz.

## Resultado técnico

- Integridad relacional: buena.
- Rendimiento estructural: bueno con faltantes y duplicados puntuales de índices.
- Seguridad de acceso directo: razonable en las migraciones existentes, con una brecha de RLS fail-closed en solicitudes públicas y una credencial de práctica que debe retirarse.
- Alineación DDL/JPA: requiere corregir/validar TIMESTAMPTZ y CHAR de género público.
- Preparación productiva: observada hasta resolver prioridad alta y el importe de matrícula de prueba.


## Seguimiento 2026-09-24 — simulación de matrícula por S/ 350

- MATRICULA_PAGO_MONTO configura el importe en PEN y su valor predeterminado es 350.00.
- La migración V13 elimina el límite fijo de S/ 1, deja el valor predeterminado en S/ 350 y conserva todos los pagos históricos; todavía no se ha aplicado en Supabase.
- El backend rechaza access tokens que no empiecen con TEST- y exige/devoluciona sandbox_init_point; ya no recurre al enlace normal de checkout.
- .env no fue leído ni modificado. .env.example documenta el nuevo parámetro. Para probar contra Mercado Pago, el entorno local debe tener las credenciales de prueba configuradas.
- mvnw -Dtest=SolicitudMatriculaPublicaDocumentosTest test: BUILD SUCCESS, 5 pruebas en verde.
- La ejecución completa previa a la segunda prueba nueva reportó 108 pruebas, 1 fallo en ArquitecturaTest.serviciosSinComodinesNiSettersDeNegocio (RefuerzoService) y 9 errores porque PostgreSQL de pruebas no estaba disponible en 127.0.0.1:55439. No valida una transacción real con Mercado Pago.

## Seguimiento local 2026-09-24 — limpieza arquitectónica V14

Se preparó la migración `V14__normalizar_aulas_integridad_y_indices.sql` y se incorporó al final de ambos scripts consolidados. No se ejecutó sobre PostgreSQL ni se corrieron pruebas.

- **Aulas:** `secciones.aula_id` es la única relación persistida; se elimina el duplicado `aula_fisica`. El contrato JSON conserva `aulaFisica` con el código para compatibilidad, derivado de `aulas.codigo`. Las respuestas cargan el aula relacionada en lote. V14 aborta ante códigos duplicados por mayúsculas, espacios externos o discrepancias entre la columna antigua y el aula FK.
- **Tesorería:** se añadió FK entre comprobantes y series, respetando `B001` y `E001`; V14 valida orfandad antes de aplicarla.
- **Retención:** competencias y documentos de solicitudes ya no se eliminan en cascada con su padre; sus FKs usan `RESTRICT`.
- **Índices:** se quitan cinco duplicados exactos o cubiertos por una clave única; se añaden índices para las cinco FKs de solicitudes públicas, usando índices parciales para columnas opcionales.
- **Acceso:** se habilita RLS y se revoca acceso directo a `anon` y `authenticated` en solicitudes públicas con PII.
- **Scripts:** `schema_completo_colegio_shuji.sql` y `schema_completo_importador_postgresql.sql` siguen idénticos y ahora consolidan V1–V14.
- `mvnw -DskipTests -Dmaven.compiler.fork=true compile`: BUILD SUCCESS, sin errores de fuentes; no se ejecutaron pruebas. El JDK imprimió un `AccessDeniedException` al cerrar un JAR cacheado, aunque Maven devolvió código 0.
- `.env` no se leyó ni modificó. El cambio previo en `CorsConfig.java` permanece fuera del alcance.

## Resultados de pruebas locales — 2026-09-24

- La primera suite completa contó **109 tests, 1 fallo y 9 errores**. El fallo de arquitectura por setter directo en `RefuerzoService` se corrigió mediante `SesionRefuerzo.asignarAula(...)`.
- Repetición dirigida: **30/30** para `ArquitecturaTest`, `SeguridadAvanzadaTest`, `MercadoPagoWebhookControllerTest` y `TesoreriaMercadoPagoFlowTest`.
- Suite restante excluyendo las tres clases que requieren PostgreSQL: **100/100 BUILD SUCCESS**.
- Las pruebas de integración/concurrencia no arrancaron porque no hay PostgreSQL en `127.0.0.1:55439`; el motor Docker local tampoco estaba iniciado. No se consultó la base remota.
- OWASP Dependency-Check 9.0.9 no llegó a analizar dependencias: NVD respondió HTTP 403/404 y no existía base CVE local. El resultado queda **pendiente**, no equivale a cero vulnerabilidades.
- `.env` no se leyó ni modificó. `CorsConfig.java` conserva su cambio local previo.
