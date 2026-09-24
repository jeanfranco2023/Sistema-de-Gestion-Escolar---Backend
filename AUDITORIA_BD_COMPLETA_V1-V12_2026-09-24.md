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

1. **Pago público de matrícula fijado a S/ 1.00.** SolicitudMatriculaPublicaService declara MONTO_PRUEBA, rechaza cualquier otro importe y V8 mantiene CHECK (pago_monto = 1.00). El catálogo MATRÍCULA_ANUAL está sembrado en S/ 350.00. Antes de producción, el importe debe salir del concepto aprobado y no de un valor de prueba.
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
