# Depuración de atributos de la base de datos — 2026-09-30

Estado local: V1–V16, 37 tablas, 307 atributos y 222 restricciones. Se revisaron los identificadores, sus referencias en repositorios, DTO, servicios y frontend, y la correspondencia dominio–persistencia. La migración no elimina registros; la verificación utiliza esquemas temporales. No se modificó .env. Actualización posterior: public ya migrado a V17; consultar ACTUALIZACION_BD_SUPABASE_2026-09-30.md.

## Eliminaciones justificadas

| Elemento | Decisión y evidencia |
|---|---|
| UUID de usuarios, apoderados, estudiantes, matrículas, pagos_transacciones y comprobantes_pago | Eliminar: el ID numérico es PK y destino de las FK; no existen búsquedas, rutas ni procesos basados en esos UUID. Frontend solo los declaraba. |
| roles.descripcion | Eliminar: ningún proceso ni pantalla la consume; código y nombre ya identifican el rol. |
| ComunicadoOficial.uuid e IncidenciaConductual.uuid | Eliminar del dominio: nunca se persistían ni tenían uso funcional. |
| Nivel.descripcion | Eliminar del dominio: no existía en la entidad JPA ni en la respuesta del catálogo. |
| InscripcionRefuerzo.matriculaId | Eliminar del dominio: no se persistía ni se usaba; la calificación de origen conserva el vínculo con matrícula. |

## Campos conservados que pueden parecer redundantes

| Campo o grupo | Justificación |
|---|---|
| solicitudes_matricula_publica.id UUID | Es su única PK. Se utiliza en consulta pública, validación documental, almacenamiento y referencia de pago. |
| solicitudes_matricula_documentos.solicitud_id UUID | FK de la solicitud; no duplica un ID numérico. |
| roles.codigo_rol / nombre_rol | Código estable para autorización; nombre legible para mostrar. ID es la clave relacional. |
| niveles.codigo_nivel / nombre_nivel | El código corresponde al enum funcional usado por oferta y validación; el nombre es la etiqueta. |
| aulas.codigo_aula / nombre_aula / ubicacion | Código identificador, etiqueta para reportes y localización dentro del colegio son datos distintos. |
| Referencias de año, sección, nivel y matrícula | Relaciones y coherencia académica temporal. No retirar una FK solo por poder recorrer otra relación. |
| calificacion_origen_id en refuerzo | Trazabilidad de la calificación que motivó la inscripción. |
| Saldos e indicadores GENERATED | Derivados por PostgreSQL; no se editan independientemente. Soportan filtros sin inconsistencias por doble escritura. |
| Estados, activo y fechas | Conservan historial, reservas, cierres y bajas lógicas. |
| Identificadores de proveedor, serie y correlativo | Conciliación, idempotencia y numeración del comprobante; funciones distintas de la PK. |
| Hashes, tokens y auditoría | Seguridad y trazabilidad. Un ID o UUID no sustituye la autorización de acceso. |
| Filtros de publicación de comunicado | Seleccionan destinatarios; su persistencia efectiva está en comunicado_destinatarios. No se modificó este flujo. |

Los UUID usados para claves aleatorias de idempotencia o tokens se conservan. La eliminación afecta los campos uuid en las respuestas REST indicadas; los tipos del frontend se actualizaron conjuntamente. Otros consumidores externos deberán dejar de depender de esos campos.

## Matriz completa del estado final

Conservar significa que no se identificó una redundancia eliminable con la evidencia actual. No presupone que cada campo opcional tenga datos en desarrollo. Los límites VARCHAR son máximos funcionales: no reservan espacio vacío por cada fila.

| Tabla | Atributo | Tipo | NULL | Decisión | Criterio |
|---|---|---|---|---|---|
| anios_lectivos | `id` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| anios_lectivos | `anio` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| anios_lectivos | `fecha_inicio` | date | No | Conservar | Tipo temporal nativo; sin conversión a texto. |
| anios_lectivos | `fecha_fin` | date | No | Conservar | Tipo temporal nativo; sin conversión a texto. |
| anios_lectivos | `abierto` | boolean | No | Conservar | Estado lógico: un byte; conserva los registros. |
| apoderados | `id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| apoderados | `usuario_id` | bigint | Sí | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| apoderados | `tipo_documento` | character varying(10) | No | Conservar | Código documental; conserva ceros iniciales y formatos admitidos. |
| apoderados | `numero_documento` | character varying(15) | No | Conservar | Código documental; conserva ceros iniciales y formatos admitidos. |
| apoderados | `nombres_apoderado` | character varying(20) | No | Conservar | 20 por decisión del proyecto; cada apellido se captura separado. |
| apoderados | `apellido_paterno_apoderado` | character varying(20) | No | Conservar | 20 por decisión del proyecto; cada apellido se captura separado. |
| apoderados | `apellido_materno_apoderado` | character varying(20) | No | Conservar | 20 por decisión del proyecto; cada apellido se captura separado. |
| apoderados | `celular` | character varying(9) | No | Conservar | 9 dígitos y formato peruano validado por CHECK. |
| apoderados | `correo_electronico` | character varying(254) | Sí | Conservar | 254 conserva el límite de correo del sistema; VARCHAR no reserva espacio vacío. |
| apoderados | `direccion` | character varying(150) | No | Conservar | 150 para vía, número, urbanización y referencias; ubigeo separado. |
| apoderados | `ubigeo_inei` | character varying(6) | No | Conservar | 6 dígitos INEI; código, no cantidad numérica. |
| apoderados | `validado_reniec` | boolean | No | Conservar | Estado lógico: un byte; conserva los registros. |
| apoderados | `origen_registro` | character varying(20) | No | Conservar | Valor enumerado con CHECK; margen explícito para estados válidos. |
| apoderados | `fecha_creacion` | timestamp with time zone | No | Conservar | Fecha/hora con zona; conserva el instante y contexto de auditoría. |
| areas_curriculares | `id` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| areas_curriculares | `nivel_id` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| areas_curriculares | `codigo_area` | character varying(20) | No | Conservar | Límite funcional del catálogo/metadato; no reserva el máximo en disco. |
| areas_curriculares | `nombre_area` | character varying(60) | No | Conservar | 60 para denominaciones curriculares; descripción extensa fuera del nombre. |
| asignaciones_docentes | `id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| asignaciones_docentes | `docente_usuario_id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| asignaciones_docentes | `seccion_id` | integer | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| asignaciones_docentes | `anio_lectivo_id` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| asignaciones_docentes | `nivel_id` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| asignaciones_docentes | `area_curricular_id` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| asistencias_aula | `id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| asistencias_aula | `matricula_id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| asistencias_aula | `fecha_sesion` | date | No | Conservar | Tipo temporal nativo; sin conversión a texto. |
| asistencias_aula | `hora_registro` | time without time zone | No | Conservar | Tipo temporal nativo; sin conversión a texto. |
| asistencias_aula | `estado` | character varying(25) | No | Conservar | Valor enumerado con CHECK; margen explícito para estados válidos. |
| asistencias_aula | `auxiliar_usuario_id` | bigint | Sí | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| asistencias_aula | `justificada` | boolean | No | Conservar | Estado lógico: un byte; conserva los registros. |
| asistencias_aula | `motivo_justificacion` | text | Sí | Conservar | TEXT con CHECK máximo 4000; texto variable, sin reserva fija. |
| asistencias_aula | `documento_sustento_url` | character varying(255) | Sí | Conservar | URL externa variable; no reducir basándose en un título o nombre. |
| auditoria_cambios | `id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| auditoria_cambios | `tabla_afectada` | character varying(50) | No | Conservar | Límite funcional del catálogo/metadato; no reserva el máximo en disco. |
| auditoria_cambios | `registro_id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| auditoria_cambios | `accion` | character varying(10) | No | Conservar | Valor enumerado con CHECK; margen explícito para estados válidos. |
| auditoria_cambios | `datos_anteriores` | jsonb | Sí | Conservar | Instantánea estructurada de auditoría; no sustituir por VARCHAR. |
| auditoria_cambios | `datos_nuevos` | jsonb | Sí | Conservar | Instantánea estructurada de auditoría; no sustituir por VARCHAR. |
| auditoria_cambios | `usuario_id` | bigint | Sí | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| auditoria_cambios | `fecha_creacion` | timestamp with time zone | No | Conservar | Fecha/hora con zona; conserva el instante y contexto de auditoría. |
| aulas | `id` | integer | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| aulas | `codigo_aula` | character varying(30) | No | Conservar | 30 ya acordados; conserva las referencias y códigos institucionales. |
| aulas | `nombre_aula` | character varying(40) | No | Conservar | Límite funcional del catálogo/metadato; no reserva el máximo en disco. |
| aulas | `ubicacion` | character varying(60) | Sí | Conservar | 60 para pabellón/piso/referencia dentro de la institución. |
| aulas | `capacidad` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| aulas | `activa` | boolean | No | Conservar | Estado lógico: un byte; conserva los registros. |
| bloques_horarios | `id` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| bloques_horarios | `numero_bloque` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| bloques_horarios | `hora_inicio` | time without time zone | No | Conservar | Tipo temporal nativo; sin conversión a texto. |
| bloques_horarios | `hora_fin` | time without time zone | No | Conservar | Tipo temporal nativo; sin conversión a texto. |
| bloques_horarios | `es_recreo` | boolean | No | Conservar | Estado lógico: un byte; conserva los registros. |
| calificaciones_cneb | `id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| calificaciones_cneb | `matricula_id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| calificaciones_cneb | `anio_lectivo_id` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| calificaciones_cneb | `seccion_id` | integer | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| calificaciones_cneb | `periodo_academico_id` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| calificaciones_cneb | `asignacion_docente_id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| calificaciones_cneb | `area_curricular_id` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| calificaciones_cneb | `docente_usuario_id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| calificaciones_cneb | `competencia_id` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| calificaciones_cneb | `calificacion_cualitativa` | character varying(2) | No | Conservar | Valor enumerado con CHECK; margen explícito para estados válidos. |
| calificaciones_cneb | `conclusion_descriptiva` | text | Sí | Conservar | TEXT con CHECK máximo 4000; texto variable, sin reserva fija. |
| calificaciones_cneb | `sugerencia_ia_utilizada` | boolean | No | Conservar | Estado lógico: un byte; conserva los registros. |
| calificaciones_cneb | `requiere_refuerzo` | boolean | Sí | Conservar | Estado lógico: un byte; conserva los registros. |
| calificaciones_cneb | `fecha_creacion` | timestamp with time zone | No | Conservar | Fecha/hora con zona; conserva el instante y contexto de auditoría. |
| calificaciones_cneb | `fecha_actualizacion` | timestamp with time zone | No | Conservar | Fecha/hora con zona; conserva el instante y contexto de auditoría. |
| competencias | `id` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| competencias | `area_id` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| competencias | `numero_orden` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| competencias | `nombre_competencia` | character varying(50) | No | Conservar | 50 pactados para etiqueta; desarrollo completo en descripción. |
| competencias | `descripcion` | text | Sí | Conservar | TEXT con CHECK máximo 4000; texto variable, sin reserva fija. |
| comprobantes_pago | `id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| comprobantes_pago | `pago_transaccion_id` | bigint | No | Conservar | Identificador de proveedor externo; no imponer un formato numérico local. |
| comprobantes_pago | `tipo_comprobante` | character varying(20) | No | Conservar | Valor enumerado con CHECK; margen explícito para estados válidos. |
| comprobantes_pago | `serie` | character varying(4) | No | Conservar | Límite funcional del catálogo/metadato; no reserva el máximo en disco. |
| comprobantes_pago | `correlativo` | integer | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| comprobantes_pago | `fecha_emision` | timestamp with time zone | No | Conservar | Fecha/hora con zona; conserva el instante y contexto de auditoría. |
| comprobantes_pago | `monto_total` | numeric(10,2) | No | Conservar | Precisión decimal exacta para importes; mantener escala y coherencia de FK/finanzas. |
| comprobantes_pago | `estado_comprobante` | character varying(20) | No | Conservar | Valor enumerado con CHECK; margen explícito para estados válidos. |
| comprobantes_pago | `fecha_anulacion` | timestamp with time zone | Sí | Conservar | Fecha/hora con zona; conserva el instante y contexto de auditoría. |
| comprobantes_pago | `motivo_anulacion` | character varying(255) | Sí | Conservar | Límite funcional del catálogo/metadato; no reserva el máximo en disco. |
| comprobantes_pago | `url_pdf_comprobante` | character varying(255) | Sí | Conservar | URL externa variable; no reducir basándose en un título o nombre. |
| comunicado_destinatarios | `id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| comunicado_destinatarios | `comunicado_id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| comunicado_destinatarios | `apoderado_id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| comunicado_destinatarios | `leido` | boolean | No | Conservar | Estado lógico: un byte; conserva los registros. |
| comunicado_destinatarios | `fecha_lectura` | timestamp with time zone | Sí | Conservar | Fecha/hora con zona; conserva el instante y contexto de auditoría. |
| comunicado_destinatarios | `acuse_confirmado` | boolean | No | Conservar | Estado lógico: un byte; conserva los registros. |
| comunicado_destinatarios | `fecha_acuse` | timestamp with time zone | Sí | Conservar | Fecha/hora con zona; conserva el instante y contexto de auditoría. |
| comunicados_oficiales | `id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| comunicados_oficiales | `titulo_comunicado` | character varying(40) | No | Conservar | 40 pactados para título/tema breve; contenido separado. |
| comunicados_oficiales | `contenido` | text | No | Conservar | TEXT con CHECK máximo 20000; texto variable, sin reserva fija. |
| comunicados_oficiales | `remitente_usuario_id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| comunicados_oficiales | `fecha_publicacion` | timestamp with time zone | No | Conservar | Fecha/hora con zona; conserva el instante y contexto de auditoría. |
| comunicados_oficiales | `requiere_acuse` | boolean | No | Conservar | Estado lógico: un byte; conserva los registros. |
| conceptos_cobro | `id` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| conceptos_cobro | `codigo_concepto` | character varying(30) | No | Conservar | Límite funcional del catálogo/metadato; no reserva el máximo en disco. |
| conceptos_cobro | `nombre_concepto` | character varying(50) | No | Conservar | 50: el catálogo existente contiene un nombre de 42 caracteres. |
| conceptos_cobro | `tipo_concepto` | character varying(20) | No | Conservar | Valor enumerado con CHECK; margen explícito para estados válidos. |
| conceptos_cobro | `monto_sugerido` | numeric(10,2) | No | Conservar | Precisión decimal exacta para importes; mantener escala y coherencia de FK/finanzas. |
| conciliaciones_asistencia | `id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| conciliaciones_asistencia | `fecha` | date | No | Conservar | Tipo temporal nativo; sin conversión a texto. |
| conciliaciones_asistencia | `estudiante_id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| conciliaciones_asistencia | `marco_porteria` | boolean | No | Conservar | Estado lógico: un byte; conserva los registros. |
| conciliaciones_asistencia | `presente_aula` | boolean | No | Conservar | Estado lógico: un byte; conserva los registros. |
| conciliaciones_asistencia | `tipo_discrepancia` | character varying(35) | No | Conservar | Valor enumerado con CHECK; margen explícito para estados válidos. |
| conciliaciones_asistencia | `alerta_notificada` | boolean | No | Conservar | Estado lógico: un byte; conserva los registros. |
| estudiante_apoderados | `id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| estudiante_apoderados | `estudiante_id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| estudiante_apoderados | `apoderado_id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| estudiante_apoderados | `parentesco` | character varying(30) | No | Conservar | Valor enumerado con CHECK; margen explícito para estados válidos. |
| estudiante_apoderados | `es_responsable_economico` | boolean | No | Conservar | Estado lógico: un byte; conserva los registros. |
| estudiante_apoderados | `tiene_custodia` | boolean | No | Conservar | Estado lógico: un byte; conserva los registros. |
| estudiante_apoderados | `permite_recojo` | boolean | No | Conservar | Estado lógico: un byte; conserva los registros. |
| estudiantes | `id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| estudiantes | `tipo_documento` | character varying(10) | No | Conservar | Código documental; conserva ceros iniciales y formatos admitidos. |
| estudiantes | `numero_documento` | character varying(15) | No | Conservar | Código documental; conserva ceros iniciales y formatos admitidos. |
| estudiantes | `nombres_estudiante` | character varying(20) | No | Conservar | 20 por decisión del proyecto; cada apellido se captura separado. |
| estudiantes | `apellido_paterno_estudiante` | character varying(20) | No | Conservar | 20 por decisión del proyecto; cada apellido se captura separado. |
| estudiantes | `apellido_materno_estudiante` | character varying(20) | No | Conservar | 20 por decisión del proyecto; cada apellido se captura separado. |
| estudiantes | `fecha_nacimiento` | date | No | Conservar | Tipo temporal nativo; sin conversión a texto. |
| estudiantes | `genero` | character(1) | No | Conservar | Valor enumerado con CHECK; margen explícito para estados válidos. |
| estudiantes | `codigo_estudiante_siagie` | character varying(14) | Sí | Conservar | Límite funcional del catálogo/metadato; no reserva el máximo en disco. |
| estudiantes | `validado_reniec` | boolean | No | Conservar | Estado lógico: un byte; conserva los registros. |
| estudiantes | `origen_registro` | character varying(20) | No | Conservar | Valor enumerado con CHECK; margen explícito para estados válidos. |
| estudiantes | `grupo_sanguineo` | character varying(5) | Sí | Conservar | Límite funcional del catálogo/metadato; no reserva el máximo en disco. |
| estudiantes | `alergias_condiciones` | text | Sí | Conservar | TEXT con CHECK máximo 4000; texto variable, sin reserva fija. |
| estudiantes | `activo` | boolean | No | Conservar | Estado lógico: un byte; conserva los registros. |
| estudiantes | `fecha_creacion` | timestamp with time zone | No | Conservar | Fecha/hora con zona; conserva el instante y contexto de auditoría. |
| grados | `id` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| grados | `nivel_id` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| grados | `numero_grado` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| grados | `nombre_grado` | character varying(30) | No | Conservar | Límite funcional del catálogo/metadato; no reserva el máximo en disco. |
| horarios_seccion | `id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| horarios_seccion | `anio_lectivo_id` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| horarios_seccion | `seccion_id` | integer | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| horarios_seccion | `docente_usuario_id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| horarios_seccion | `asignacion_docente_id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| horarios_seccion | `dia_semana` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| horarios_seccion | `bloque_horario_id` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| incidencias_conductuales | `id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| incidencias_conductuales | `matricula_id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| incidencias_conductuales | `fecha_incidencia` | date | No | Conservar | Tipo temporal nativo; sin conversión a texto. |
| incidencias_conductuales | `tipo_falta` | character varying(20) | No | Conservar | Valor enumerado con CHECK; margen explícito para estados válidos. |
| incidencias_conductuales | `descripcion` | text | No | Conservar | TEXT con CHECK máximo 10000; texto variable, sin reserva fija. |
| incidencias_conductuales | `reportado_por_usuario_id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| incidencias_conductuales | `requiere_citacion` | boolean | No | Conservar | Estado lógico: un byte; conserva los registros. |
| incidencias_conductuales | `estado` | character varying(20) | No | Conservar | Valor enumerado con CHECK; margen explícito para estados válidos. |
| incidencias_conductuales | `fecha_creacion` | timestamp with time zone | No | Conservar | Fecha/hora con zona; conserva el instante y contexto de auditoría. |
| inscripciones_refuerzo | `id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| inscripciones_refuerzo | `sesion_refuerzo_id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| inscripciones_refuerzo | `estudiante_id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| inscripciones_refuerzo | `calificacion_origen_id` | bigint | Sí | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| inscripciones_refuerzo | `estado_asistencia` | character varying(20) | No | Conservar | Valor enumerado con CHECK; margen explícito para estados válidos. |
| inscripciones_refuerzo | `observaciones` | text | Sí | Conservar | TEXT con CHECK máximo 500; texto variable, sin reserva fija. |
| lotes_biometrico | `id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| lotes_biometrico | `nombre_archivo` | character varying(150) | No | Conservar | Límite funcional del catálogo/metadato; no reserva el máximo en disco. |
| lotes_biometrico | `total_filas` | integer | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| lotes_biometrico | `marcas_validas` | integer | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| lotes_biometrico | `marcas_erroneas` | integer | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| lotes_biometrico | `importado_por_usuario_id` | bigint | Sí | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| lotes_biometrico | `fecha_creacion` | timestamp with time zone | No | Conservar | Fecha/hora con zona; conserva el instante y contexto de auditoría. |
| lotes_biometrico | `huella_contenido` | character varying(64) | No | Conservar | Digest criptográfico: longitud fija del algoritmo; no reducir. |
| marcas_biometrico_porteria | `id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| marcas_biometrico_porteria | `lote_id` | bigint | Sí | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| marcas_biometrico_porteria | `dni_leido` | character varying(15) | No | Conservar | Código documental; conserva ceros iniciales y formatos admitidos. |
| marcas_biometrico_porteria | `fecha_hora` | timestamp with time zone | No | Conservar | Fecha/hora con zona; conserva el instante y contexto de auditoría. |
| marcas_biometrico_porteria | `dispositivo_codigo` | character varying(30) | No | Conservar | Límite funcional del catálogo/metadato; no reserva el máximo en disco. |
| marcas_biometrico_porteria | `estudiante_id` | bigint | Sí | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| marcas_biometrico_porteria | `estado_procesamiento` | character varying(25) | No | Conservar | Valor enumerado con CHECK; margen explícito para estados válidos. |
| marcas_biometrico_porteria | `fecha_creacion` | timestamp with time zone | No | Conservar | Fecha/hora con zona; conserva el instante y contexto de auditoría. |
| matriculas | `id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| matriculas | `anio_lectivo_id` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| matriculas | `estudiante_id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| matriculas | `seccion_id` | integer | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| matriculas | `fecha_matricula` | timestamp with time zone | No | Conservar | Fecha/hora con zona; conserva el instante y contexto de auditoría. |
| matriculas | `estado_matricula` | character varying(25) | No | Conservar | Valor enumerado con CHECK; margen explícito para estados válidos. |
| matriculas | `fecha_expiracion_reserva` | timestamp with time zone | Sí | Conservar | Fecha/hora con zona; conserva el instante y contexto de auditoría. |
| matriculas | `observaciones` | text | Sí | Conservar | TEXT con CHECK máximo 500; texto variable, sin reserva fija. |
| niveles | `id` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| niveles | `codigo_nivel` | character varying(20) | No | Conservar | Valor enumerado con CHECK; margen explícito para estados válidos. |
| niveles | `nombre_nivel` | character varying(30) | No | Conservar | Límite funcional del catálogo/metadato; no reserva el máximo en disco. |
| obligaciones_pago | `id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| obligaciones_pago | `matricula_id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| obligaciones_pago | `concepto_id` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| obligaciones_pago | `tipo_concepto` | character varying(20) | No | Conservar | Límite funcional del catálogo/metadato; no reserva el máximo en disco. |
| obligaciones_pago | `numero_cuota` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| obligaciones_pago | `descripcion` | character varying(150) | No | Conservar | Límite funcional del catálogo/metadato; no reserva el máximo en disco. |
| obligaciones_pago | `fecha_vencimiento` | date | No | Conservar | Tipo temporal nativo; sin conversión a texto. |
| obligaciones_pago | `monto_base` | numeric(10,2) | No | Conservar | Precisión decimal exacta para importes; mantener escala y coherencia de FK/finanzas. |
| obligaciones_pago | `monto_mora` | numeric(10,2) | No | Conservar | Precisión decimal exacta para importes; mantener escala y coherencia de FK/finanzas. |
| obligaciones_pago | `monto_descuento` | numeric(10,2) | No | Conservar | Precisión decimal exacta para importes; mantener escala y coherencia de FK/finanzas. |
| obligaciones_pago | `total_pagado` | numeric(10,2) | No | Conservar | Precisión decimal exacta para importes; mantener escala y coherencia de FK/finanzas. |
| obligaciones_pago | `saldo_pendiente` | numeric(10,2) | Sí | Conservar | Precisión decimal exacta para importes; mantener escala y coherencia de FK/finanzas. |
| obligaciones_pago | `estado` | character varying(25) | No | Conservar | Valor enumerado con CHECK; margen explícito para estados válidos. |
| pagos_transacciones | `id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| pagos_transacciones | `obligacion_pago_id` | bigint | No | Conservar | Identificador de proveedor externo; no imponer un formato numérico local. |
| pagos_transacciones | `pasarela_proveedor` | character varying(30) | No | Conservar | Valor enumerado con CHECK; margen explícito para estados válidos. |
| pagos_transacciones | `pasarela_transaccion_id` | character varying(100) | No | Conservar | Identificador de proveedor externo; no imponer un formato numérico local. |
| pagos_transacciones | `metodo_pago` | character varying(30) | No | Conservar | Valor enumerado con CHECK; margen explícito para estados válidos. |
| pagos_transacciones | `monto_pagado` | numeric(10,2) | No | Conservar | Precisión decimal exacta para importes; mantener escala y coherencia de FK/finanzas. |
| pagos_transacciones | `fecha_pago` | timestamp with time zone | No | Conservar | Fecha/hora con zona; conserva el instante y contexto de auditoría. |
| pagos_transacciones | `estado_pago` | character varying(20) | No | Conservar | Valor enumerado con CHECK; margen explícito para estados válidos. |
| pagos_transacciones | `payload_webhook` | jsonb | Sí | Conservar | Instantánea estructurada de auditoría; no sustituir por VARCHAR. |
| periodos_academicos | `id` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| periodos_academicos | `anio_lectivo_id` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| periodos_academicos | `numero_periodo` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| periodos_academicos | `nombre_periodo` | character varying(30) | No | Conservar | Límite funcional del catálogo/metadato; no reserva el máximo en disco. |
| periodos_academicos | `fecha_inicio` | date | No | Conservar | Tipo temporal nativo; sin conversión a texto. |
| periodos_academicos | `fecha_fin` | date | No | Conservar | Tipo temporal nativo; sin conversión a texto. |
| periodos_academicos | `cerrado` | boolean | No | Conservar | Estado lógico: un byte; conserva los registros. |
| periodos_academicos | `activo` | boolean | No | Conservar | Estado lógico: un byte; conserva los registros. |
| roles | `id` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| roles | `codigo_rol` | character varying(20) | No | Conservar | Límite funcional del catálogo/metadato; no reserva el máximo en disco. |
| roles | `nombre_rol` | character varying(30) | No | Conservar | Límite funcional del catálogo/metadato; no reserva el máximo en disco. |
| secciones | `id` | integer | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| secciones | `anio_lectivo_id` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| secciones | `grado_id` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| secciones | `nivel_id` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| secciones | `letra` | character(1) | No | Conservar | Límite funcional del catálogo/metadato; no reserva el máximo en disco. |
| secciones | `cupo_maximo` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| secciones | `vacantes_ocupadas` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| secciones | `aula_id` | integer | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| series_comprobante | `serie` | character varying(4) | No | Conservar | Límite funcional del catálogo/metadato; no reserva el máximo en disco. |
| series_comprobante | `ultimo_correlativo` | integer | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| series_comprobante | `fecha_actualizacion` | timestamp with time zone | No | Conservar | Fecha/hora con zona; conserva el instante y contexto de auditoría. |
| sesiones | `id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| sesiones | `usuario_id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| sesiones | `huella_token` | character varying(64) | No | Conservar | Digest criptográfico: longitud fija del algoritmo; no reducir. |
| sesiones | `direccion_ip` | inet | Sí | Conservar | Tipo nativo IPv4/IPv6; evita tamaños VARCHAR arbitrarios. |
| sesiones | `agente_usuario` | text | Sí | Conservar | Texto técnico variable de navegador; no representa un nombre/título y no reserva espacio fijo. |
| sesiones | `fecha_expiracion` | timestamp with time zone | No | Conservar | Fecha/hora con zona; conserva el instante y contexto de auditoría. |
| sesiones | `fecha_creacion` | timestamp with time zone | No | Conservar | Fecha/hora con zona; conserva el instante y contexto de auditoría. |
| sesiones_refuerzo | `id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| sesiones_refuerzo | `anio_lectivo_id` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| sesiones_refuerzo | `periodo_academico_id` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| sesiones_refuerzo | `area_curricular_id` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| sesiones_refuerzo | `docente_usuario_id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| sesiones_refuerzo | `tema_refuerzo` | character varying(40) | No | Conservar | 40 pactados para título/tema breve; contenido separado. |
| sesiones_refuerzo | `fecha_programada` | date | No | Conservar | Tipo temporal nativo; sin conversión a texto. |
| sesiones_refuerzo | `hora_inicio` | time without time zone | No | Conservar | Tipo temporal nativo; sin conversión a texto. |
| sesiones_refuerzo | `hora_fin` | time without time zone | No | Conservar | Tipo temporal nativo; sin conversión a texto. |
| sesiones_refuerzo | `codigo_aula` | character varying(30) | Sí | Conservar | 30 ya acordados; conserva las referencias y códigos institucionales. |
| solicitudes_matricula_documentos | `solicitud_id` | uuid | No | Conservar | Clave primaria de solicitud pública o FK que la referencia; uso comprobado en el flujo público. |
| solicitudes_matricula_documentos | `tipo` | character varying(30) | No | Conservar | Valor enumerado con CHECK; margen explícito para estados válidos. |
| solicitudes_matricula_documentos | `contenedor` | character varying(100) | No | Conservar | Referencia técnica de Storage, no título visible; conservar capacidad de rutas/configuración. |
| solicitudes_matricula_documentos | `ruta_objeto` | character varying(500) | No | Conservar | Referencia técnica de Storage, no título visible; conservar capacidad de rutas/configuración. |
| solicitudes_matricula_documentos | `tipo_contenido` | character varying(30) | No | Conservar | 30 cubre los MIME admitidos; CHECK restringe formatos. |
| solicitudes_matricula_documentos | `tamano_bytes` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| solicitudes_matricula_documentos | `huella_sha256` | character(64) | No | Conservar | Digest criptográfico: longitud fija del algoritmo; no reducir. |
| solicitudes_matricula_documentos | `fecha_actualizacion` | timestamp with time zone | No | Conservar | Fecha/hora con zona; conserva el instante y contexto de auditoría. |
| solicitudes_matricula_publica | `id` | uuid | No | Conservar | Clave primaria de solicitud pública o FK que la referencia; uso comprobado en el flujo público. |
| solicitudes_matricula_publica | `huella_token` | character varying(64) | No | Conservar | Digest criptográfico: longitud fija del algoritmo; no reducir. |
| solicitudes_matricula_publica | `estado` | character varying(30) | No | Conservar | Valor enumerado con CHECK; margen explícito para estados válidos. |
| solicitudes_matricula_publica | `anio_lectivo_id` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| solicitudes_matricula_publica | `seccion_id` | integer | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| solicitudes_matricula_publica | `numero_documento_estudiante` | character varying(8) | No | Conservar | Código documental; conserva ceros iniciales y formatos admitidos. |
| solicitudes_matricula_publica | `nombres_estudiante` | character varying(20) | No | Conservar | 20 por decisión del proyecto; cada apellido se captura separado. |
| solicitudes_matricula_publica | `apellido_paterno_estudiante` | character varying(20) | No | Conservar | 20 por decisión del proyecto; cada apellido se captura separado. |
| solicitudes_matricula_publica | `apellido_materno_estudiante` | character varying(20) | No | Conservar | 20 por decisión del proyecto; cada apellido se captura separado. |
| solicitudes_matricula_publica | `fecha_nacimiento_estudiante` | date | No | Conservar | Tipo temporal nativo; sin conversión a texto. |
| solicitudes_matricula_publica | `genero_estudiante` | character(1) | No | Conservar | Valor enumerado con CHECK; margen explícito para estados válidos. |
| solicitudes_matricula_publica | `numero_documento_apoderado` | character varying(8) | No | Conservar | Código documental; conserva ceros iniciales y formatos admitidos. |
| solicitudes_matricula_publica | `nombres_apoderado` | character varying(20) | No | Conservar | 20 por decisión del proyecto; cada apellido se captura separado. |
| solicitudes_matricula_publica | `apellido_paterno_apoderado` | character varying(20) | No | Conservar | 20 por decisión del proyecto; cada apellido se captura separado. |
| solicitudes_matricula_publica | `apellido_materno_apoderado` | character varying(20) | No | Conservar | 20 por decisión del proyecto; cada apellido se captura separado. |
| solicitudes_matricula_publica | `celular_apoderado` | character varying(9) | No | Conservar | 9 dígitos y formato peruano validado por CHECK. |
| solicitudes_matricula_publica | `correo_apoderado` | character varying(254) | Sí | Conservar | 254 conserva el límite de correo del sistema; VARCHAR no reserva espacio vacío. |
| solicitudes_matricula_publica | `direccion_apoderado` | character varying(150) | No | Conservar | 150 para vía, número, urbanización y referencias; ubigeo separado. |
| solicitudes_matricula_publica | `ubigeo_apoderado` | character varying(6) | No | Conservar | 6 dígitos INEI; código, no cantidad numérica. |
| solicitudes_matricula_publica | `parentesco` | character varying(30) | No | Conservar | Valor enumerado con CHECK; margen explícito para estados válidos. |
| solicitudes_matricula_publica | `consentimiento_gemini` | boolean | No | Conservar | Estado lógico: un byte; conserva los registros. |
| solicitudes_matricula_publica | `estado_partida` | character varying(15) | No | Conservar | Valor enumerado con CHECK; margen explícito para estados válidos. |
| solicitudes_matricula_publica | `observacion_partida` | character varying(500) | Sí | Conservar | Límite funcional del catálogo/metadato; no reserva el máximo en disco. |
| solicitudes_matricula_publica | `estado_dni_c4` | character varying(15) | No | Conservar | Valor enumerado con CHECK; margen explícito para estados válidos. |
| solicitudes_matricula_publica | `observacion_dni_c4` | character varying(500) | Sí | Conservar | Límite funcional del catálogo/metadato; no reserva el máximo en disco. |
| solicitudes_matricula_publica | `estado_recibo` | character varying(15) | No | Conservar | Valor enumerado con CHECK; margen explícito para estados válidos. |
| solicitudes_matricula_publica | `observacion_recibo` | character varying(500) | Sí | Conservar | Límite funcional del catálogo/metadato; no reserva el máximo en disco. |
| solicitudes_matricula_publica | `pago_preferencia_id` | character varying(100) | Sí | Conservar | Identificador de proveedor externo; no imponer un formato numérico local. |
| solicitudes_matricula_publica | `pago_enlace` | character varying(1000) | Sí | Conservar | URL externa variable; no reducir basándose en un título o nombre. |
| solicitudes_matricula_publica | `pago_id` | character varying(100) | Sí | Conservar | Identificador de proveedor externo; no imponer un formato numérico local. |
| solicitudes_matricula_publica | `pago_monto` | numeric(10,2) | No | Conservar | Precisión decimal exacta para importes; mantener escala y coherencia de FK/finanzas. |
| solicitudes_matricula_publica | `fecha_expiracion_pago` | timestamp with time zone | Sí | Conservar | Fecha/hora con zona; conserva el instante y contexto de auditoría. |
| solicitudes_matricula_publica | `vacante_reservada` | boolean | No | Conservar | Estado lógico: un byte; conserva los registros. |
| solicitudes_matricula_publica | `estudiante_id` | bigint | Sí | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| solicitudes_matricula_publica | `apoderado_id` | bigint | Sí | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| solicitudes_matricula_publica | `matricula_id` | bigint | Sí | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| solicitudes_matricula_publica | `fecha_creacion` | timestamp with time zone | No | Conservar | Fecha/hora con zona; conserva el instante y contexto de auditoría. |
| solicitudes_matricula_publica | `fecha_actualizacion` | timestamp with time zone | No | Conservar | Fecha/hora con zona; conserva el instante y contexto de auditoría. |
| solicitudes_matricula_publica | `version` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| solicitudes_matricula_publica | `comprobante_pago_codigo` | character varying(40) | Sí | Conservar | Límite funcional del catálogo/metadato; no reserva el máximo en disco. |
| solicitudes_matricula_publica | `fecha_emision_documentos` | timestamp with time zone | Sí | Conservar | Fecha/hora con zona; conserva el instante y contexto de auditoría. |
| usuario_roles | `usuario_id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| usuario_roles | `rol_id` | smallint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| usuarios | `id` | bigint | No | Conservar | Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo. |
| usuarios | `nombre_usuario` | character varying(30) | No | Conservar | 30 para el identificador de acceso; independiente de los nombres personales. |
| usuarios | `correo_electronico` | character varying(254) | No | Conservar | 254 conserva el límite de correo del sistema; VARCHAR no reserva espacio vacío. |
| usuarios | `usuario_password` | character varying(60) | No | Conservar | BCrypt existente: 60 caracteres; nunca contraseña en claro. |
| usuarios | `activo` | boolean | No | Conservar | Estado lógico: un byte; conserva los registros. |
| usuarios | `fecha_creacion` | timestamp with time zone | No | Conservar | Fecha/hora con zona; conserva el instante y contexto de auditoría. |
| usuarios | `fecha_actualizacion` | timestamp with time zone | No | Conservar | Fecha/hora con zona; conserva el instante y contexto de auditoría. |

## Restricciones verificadas

| Tabla | Restricción | Definición |
|---|---|---|
| anios_lectivos | `anios_lectivos_anio_check` | `CHECK ((anio >= 2024))` |
| anios_lectivos | `anios_lectivos_anio_key` | `UNIQUE (anio)` |
| anios_lectivos | `anios_lectivos_check` | `CHECK ((fecha_fin > fecha_inicio))` |
| anios_lectivos | `anios_lectivos_pkey` | `PRIMARY KEY (id)` |
| apoderados | `apoderados_celular_check` | `CHECK (((celular)::text ~ '^9[0-9]{8}$'::text))` |
| apoderados | `apoderados_origen_registro_check` | `CHECK (((origen_registro)::text = ANY ((ARRAY['RENIEC_API'::character varying, 'MANUAL_CONTINGENCIA'::character varying])::text[])))` |
| apoderados | `apoderados_pkey` | `PRIMARY KEY (id)` |
| apoderados | `apoderados_tipo_documento_check` | `CHECK (((tipo_documento)::text = ANY ((ARRAY['DNI'::character varying, 'CE'::character varying, 'PASAPORTE'::character varying])::text[])))` |
| apoderados | `apoderados_tipo_documento_numero_documento_key` | `UNIQUE (tipo_documento, numero_documento)` |
| apoderados | `apoderados_ubigeo_inei_check` | `CHECK (((ubigeo_inei)::text ~ '^[0-9]{6}$'::text))` |
| apoderados | `apoderados_usuario_id_fkey` | `FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE SET NULL` |
| apoderados | `apoderados_usuario_id_key` | `UNIQUE (usuario_id)` |
| areas_curriculares | `areas_curriculares_id_nivel_id_key` | `UNIQUE (id, nivel_id)` |
| areas_curriculares | `areas_curriculares_nivel_id_codigo_key` | `UNIQUE (nivel_id, codigo_area)` |
| areas_curriculares | `areas_curriculares_nivel_id_fkey` | `FOREIGN KEY (nivel_id) REFERENCES niveles(id) ON DELETE RESTRICT` |
| areas_curriculares | `areas_curriculares_pkey` | `PRIMARY KEY (id)` |
| asignaciones_docentes | `asignaciones_docentes_area_curricular_id_nivel_id_fkey` | `FOREIGN KEY (area_curricular_id, nivel_id) REFERENCES areas_curriculares(id, nivel_id) ON DELETE RESTRICT` |
| asignaciones_docentes | `asignaciones_docentes_docente_usuario_id_fkey` | `FOREIGN KEY (docente_usuario_id) REFERENCES usuarios(id) ON DELETE RESTRICT` |
| asignaciones_docentes | `asignaciones_docentes_id_anio_lectivo_id_seccion_id_docente_key` | `UNIQUE (id, anio_lectivo_id, seccion_id, docente_usuario_id)` |
| asignaciones_docentes | `asignaciones_docentes_id_seccion_id_area_curricular_id_anio_key` | `UNIQUE (id, seccion_id, area_curricular_id, anio_lectivo_id, docente_usuario_id)` |
| asignaciones_docentes | `asignaciones_docentes_pkey` | `PRIMARY KEY (id)` |
| asignaciones_docentes | `asignaciones_docentes_seccion_id_anio_lectivo_id_nivel_id_fkey` | `FOREIGN KEY (seccion_id, anio_lectivo_id, nivel_id) REFERENCES secciones(id, anio_lectivo_id, nivel_id) ON DELETE RESTRICT` |
| asignaciones_docentes | `asignaciones_docentes_seccion_id_area_curricular_id_anio_le_key` | `UNIQUE (seccion_id, area_curricular_id, anio_lectivo_id)` |
| asistencias_aula | `asistencias_aula_auxiliar_usuario_id_fkey` | `FOREIGN KEY (auxiliar_usuario_id) REFERENCES usuarios(id) ON DELETE RESTRICT` |
| asistencias_aula | `asistencias_aula_estado_check` | `CHECK (((estado)::text = ANY ((ARRAY['PRESENTE'::character varying, 'TARDANZA'::character varying, 'FALTA_INJUSTIFICADA'::character varying, 'FALTA_JUSTIFICADA'::character varying])::text[])))` |
| asistencias_aula | `asistencias_aula_matricula_id_fecha_sesion_key` | `UNIQUE (matricula_id, fecha_sesion)` |
| asistencias_aula | `asistencias_aula_matricula_id_fkey` | `FOREIGN KEY (matricula_id) REFERENCES matriculas(id) ON DELETE RESTRICT` |
| asistencias_aula | `asistencias_aula_pkey` | `PRIMARY KEY (id)` |
| asistencias_aula | `chk_asistencias_justificacion_longitud` | `CHECK ((char_length(motivo_justificacion) <= 4000))` |
| auditoria_cambios | `auditoria_cambios_accion_check` | `CHECK (((accion)::text = ANY ((ARRAY['INSERT'::character varying, 'UPDATE'::character varying, 'DELETE'::character varying])::text[])))` |
| auditoria_cambios | `auditoria_cambios_pkey` | `PRIMARY KEY (id)` |
| auditoria_cambios | `auditoria_cambios_usuario_id_fkey` | `FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE SET NULL` |
| aulas | `aulas_capacidad_check` | `CHECK ((capacidad > 0))` |
| aulas | `aulas_codigo_key` | `UNIQUE (codigo_aula)` |
| aulas | `aulas_pkey` | `PRIMARY KEY (id)` |
| aulas | `chk_aulas_codigo_sin_espacios_externos` | `CHECK (((codigo_aula)::text = btrim((codigo_aula)::text)))` |
| bloques_horarios | `bloques_horarios_check` | `CHECK ((hora_fin > hora_inicio))` |
| bloques_horarios | `bloques_horarios_numero_bloque_key` | `UNIQUE (numero_bloque)` |
| bloques_horarios | `bloques_horarios_pkey` | `PRIMARY KEY (id)` |
| calificaciones_cneb | `calificaciones_cneb_asignacion_docente_id_seccion_id_area__fkey` | `FOREIGN KEY (asignacion_docente_id, seccion_id, area_curricular_id, anio_lectivo_id, docente_usuario_id) REFERENCES asignaciones_docentes(id, seccion_id, area_curricular_id, anio_lectivo_id, docente_usuario_id) ON DELETE RESTRICT` |
| calificaciones_cneb | `calificaciones_cneb_calificacion_cualitativa_check` | `CHECK (((calificacion_cualitativa)::text = ANY ((ARRAY['AD'::character varying, 'A'::character varying, 'B'::character varying, 'C'::character varying])::text[])))` |
| calificaciones_cneb | `calificaciones_cneb_competencia_id_area_curricular_id_fkey` | `FOREIGN KEY (competencia_id, area_curricular_id) REFERENCES competencias(id, area_id) ON DELETE RESTRICT` |
| calificaciones_cneb | `calificaciones_cneb_matricula_id_anio_lectivo_id_seccion_i_fkey` | `FOREIGN KEY (matricula_id, anio_lectivo_id, seccion_id) REFERENCES matriculas(id, anio_lectivo_id, seccion_id) ON DELETE RESTRICT` |
| calificaciones_cneb | `calificaciones_cneb_matricula_id_competencia_id_periodo_aca_key` | `UNIQUE (matricula_id, competencia_id, periodo_academico_id)` |
| calificaciones_cneb | `calificaciones_cneb_periodo_academico_id_anio_lectivo_id_fkey` | `FOREIGN KEY (periodo_academico_id, anio_lectivo_id) REFERENCES periodos_academicos(id, anio_lectivo_id) ON DELETE RESTRICT` |
| calificaciones_cneb | `calificaciones_cneb_pkey` | `PRIMARY KEY (id)` |
| calificaciones_cneb | `chk_calificaciones_conclusion_longitud` | `CHECK ((char_length(conclusion_descriptiva) <= 4000))` |
| competencias | `chk_competencias_descripcion_longitud` | `CHECK ((char_length(descripcion) <= 4000))` |
| competencias | `competencias_id_area_id_key` | `UNIQUE (id, area_id)` |
| competencias | `competencias_numero_orden_check` | `CHECK ((numero_orden > 0))` |
| competencias | `competencias_pkey` | `PRIMARY KEY (id)` |
| competencias | `fk_competencias_area_id` | `FOREIGN KEY (area_id) REFERENCES areas_curriculares(id) ON UPDATE RESTRICT ON DELETE RESTRICT` |
| comprobantes_pago | `comprobantes_pago_check` | `CHECK (((((estado_comprobante)::text = 'EMITIDO'::text) AND (fecha_anulacion IS NULL) AND (motivo_anulacion IS NULL)) OR (((estado_comprobante)::text = 'ANULADO'::text) AND (fecha_anulacion IS NOT NULL) AND (motivo_anulacion IS NOT NULL))))` |
| comprobantes_pago | `comprobantes_pago_correlativo_check` | `CHECK ((correlativo > 0))` |
| comprobantes_pago | `comprobantes_pago_estado_comprobante_check` | `CHECK (((estado_comprobante)::text = ANY ((ARRAY['EMITIDO'::character varying, 'ANULADO'::character varying])::text[])))` |
| comprobantes_pago | `comprobantes_pago_monto_total_check` | `CHECK ((monto_total > 0.00))` |
| comprobantes_pago | `comprobantes_pago_pago_transaccion_id_fkey` | `FOREIGN KEY (pago_transaccion_id) REFERENCES pagos_transacciones(id) ON DELETE RESTRICT` |
| comprobantes_pago | `comprobantes_pago_pago_transaccion_id_key` | `UNIQUE (pago_transaccion_id)` |
| comprobantes_pago | `comprobantes_pago_pkey` | `PRIMARY KEY (id)` |
| comprobantes_pago | `comprobantes_pago_serie_check` | `CHECK (((serie)::text ~ '^[BE][0-9]{3}$'::text))` |
| comprobantes_pago | `comprobantes_pago_serie_correlativo_key` | `UNIQUE (serie, correlativo)` |
| comprobantes_pago | `comprobantes_pago_tipo_comprobante_check` | `CHECK (((tipo_comprobante)::text = ANY ((ARRAY['BOLETA'::character varying, 'RECIBO_INTERNO'::character varying])::text[])))` |
| comprobantes_pago | `fk_comprobantes_pago_serie` | `FOREIGN KEY (serie) REFERENCES series_comprobante(serie) ON UPDATE RESTRICT ON DELETE RESTRICT` |
| comunicado_destinatarios | `comunicado_destinatarios_apoderado_id_fkey` | `FOREIGN KEY (apoderado_id) REFERENCES apoderados(id) ON DELETE RESTRICT` |
| comunicado_destinatarios | `comunicado_destinatarios_comunicado_id_apoderado_id_key` | `UNIQUE (comunicado_id, apoderado_id)` |
| comunicado_destinatarios | `comunicado_destinatarios_comunicado_id_fkey` | `FOREIGN KEY (comunicado_id) REFERENCES comunicados_oficiales(id) ON DELETE RESTRICT` |
| comunicado_destinatarios | `comunicado_destinatarios_pkey` | `PRIMARY KEY (id)` |
| comunicados_oficiales | `chk_comunicados_contenido_longitud` | `CHECK ((char_length(contenido) <= 20000))` |
| comunicados_oficiales | `comunicados_oficiales_pkey` | `PRIMARY KEY (id)` |
| comunicados_oficiales | `comunicados_oficiales_remitente_usuario_id_fkey` | `FOREIGN KEY (remitente_usuario_id) REFERENCES usuarios(id) ON DELETE RESTRICT` |
| conceptos_cobro | `conceptos_cobro_codigo_key` | `UNIQUE (codigo_concepto)` |
| conceptos_cobro | `conceptos_cobro_id_tipo_concepto_key` | `UNIQUE (id, tipo_concepto)` |
| conceptos_cobro | `conceptos_cobro_monto_sugerido_check` | `CHECK ((monto_sugerido >= 0.00))` |
| conceptos_cobro | `conceptos_cobro_pkey` | `PRIMARY KEY (id)` |
| conceptos_cobro | `conceptos_cobro_tipo_concepto_check` | `CHECK (((tipo_concepto)::text = ANY ((ARRAY['MATRICULA'::character varying, 'PENSION'::character varying, 'CERTIFICADO'::character varying, 'OTRO'::character varying])::text[])))` |
| conciliaciones_asistencia | `conciliaciones_asistencia_estudiante_id_fkey` | `FOREIGN KEY (estudiante_id) REFERENCES estudiantes(id) ON DELETE RESTRICT` |
| conciliaciones_asistencia | `conciliaciones_asistencia_fecha_estudiante_id_key` | `UNIQUE (fecha, estudiante_id)` |
| conciliaciones_asistencia | `conciliaciones_asistencia_pkey` | `PRIMARY KEY (id)` |
| conciliaciones_asistencia | `conciliaciones_asistencia_tipo_discrepancia_check` | `CHECK (((tipo_discrepancia)::text = ANY ((ARRAY['ASISTENCIA_CONCILIADA'::character varying, 'DISCREPANCIA_FUGA'::character varying, 'DISCREPANCIA_OMISION_PORTERIA'::character varying, 'PENDIENTE_CIERRE_TURNO'::character varying])::text[])))` |
| estudiante_apoderados | `estudiante_apoderados_apoderado_id_fkey` | `FOREIGN KEY (apoderado_id) REFERENCES apoderados(id) ON DELETE RESTRICT` |
| estudiante_apoderados | `estudiante_apoderados_estudiante_id_apoderado_id_key` | `UNIQUE (estudiante_id, apoderado_id)` |
| estudiante_apoderados | `estudiante_apoderados_estudiante_id_fkey` | `FOREIGN KEY (estudiante_id) REFERENCES estudiantes(id) ON DELETE RESTRICT` |
| estudiante_apoderados | `estudiante_apoderados_parentesco_check` | `CHECK (((parentesco)::text = ANY ((ARRAY['PADRE'::character varying, 'MADRE'::character varying, 'TUTOR_LEGAL'::character varying, 'ABUELO_A'::character varying, 'OTRO'::character varying])::text[])))` |
| estudiante_apoderados | `estudiante_apoderados_pkey` | `PRIMARY KEY (id)` |
| estudiantes | `chk_estudiantes_salud_longitud` | `CHECK ((char_length(alergias_condiciones) <= 4000))` |
| estudiantes | `estudiantes_codigo_estudiante_siagie_key` | `UNIQUE (codigo_estudiante_siagie)` |
| estudiantes | `estudiantes_genero_check` | `CHECK ((genero = ANY (ARRAY['M'::bpchar, 'F'::bpchar])))` |
| estudiantes | `estudiantes_origen_registro_check` | `CHECK (((origen_registro)::text = ANY ((ARRAY['RENIEC_API'::character varying, 'MANUAL_CONTINGENCIA'::character varying])::text[])))` |
| estudiantes | `estudiantes_pkey` | `PRIMARY KEY (id)` |
| estudiantes | `estudiantes_tipo_documento_check` | `CHECK (((tipo_documento)::text = ANY ((ARRAY['DNI'::character varying, 'CE'::character varying, 'PASAPORTE'::character varying])::text[])))` |
| estudiantes | `estudiantes_tipo_documento_numero_documento_key` | `UNIQUE (tipo_documento, numero_documento)` |
| grados | `grados_id_nivel_id_key` | `UNIQUE (id, nivel_id)` |
| grados | `grados_nivel_id_fkey` | `FOREIGN KEY (nivel_id) REFERENCES niveles(id) ON DELETE RESTRICT` |
| grados | `grados_nivel_id_numero_grado_key` | `UNIQUE (nivel_id, numero_grado)` |
| grados | `grados_numero_grado_check` | `CHECK (((numero_grado >= 1) AND (numero_grado <= 6)))` |
| grados | `grados_pkey` | `PRIMARY KEY (id)` |
| horarios_seccion | `horarios_seccion_anio_lectivo_id_fkey` | `FOREIGN KEY (anio_lectivo_id) REFERENCES anios_lectivos(id) ON DELETE RESTRICT` |
| horarios_seccion | `horarios_seccion_asignacion_docente_id_anio_lectivo_id_sec_fkey` | `FOREIGN KEY (asignacion_docente_id, anio_lectivo_id, seccion_id, docente_usuario_id) REFERENCES asignaciones_docentes(id, anio_lectivo_id, seccion_id, docente_usuario_id) ON DELETE RESTRICT` |
| horarios_seccion | `horarios_seccion_bloque_horario_id_fkey` | `FOREIGN KEY (bloque_horario_id) REFERENCES bloques_horarios(id) ON DELETE RESTRICT` |
| horarios_seccion | `horarios_seccion_dia_semana_check` | `CHECK (((dia_semana >= 1) AND (dia_semana <= 5)))` |
| horarios_seccion | `horarios_seccion_docente_usuario_id_dia_semana_bloque_horar_key` | `UNIQUE (docente_usuario_id, dia_semana, bloque_horario_id, anio_lectivo_id)` |
| horarios_seccion | `horarios_seccion_docente_usuario_id_fkey` | `FOREIGN KEY (docente_usuario_id) REFERENCES usuarios(id) ON DELETE RESTRICT` |
| horarios_seccion | `horarios_seccion_pkey` | `PRIMARY KEY (id)` |
| horarios_seccion | `horarios_seccion_seccion_id_dia_semana_bloque_horario_id_an_key` | `UNIQUE (seccion_id, dia_semana, bloque_horario_id, anio_lectivo_id)` |
| incidencias_conductuales | `chk_incidencias_descripcion_longitud` | `CHECK ((char_length(descripcion) <= 10000))` |
| incidencias_conductuales | `incidencias_conductuales_estado_check` | `CHECK (((estado)::text = ANY ((ARRAY['ABIERTA'::character varying, 'ATENDIDA'::character varying, 'CERRADA'::character varying])::text[])))` |
| incidencias_conductuales | `incidencias_conductuales_matricula_id_fkey` | `FOREIGN KEY (matricula_id) REFERENCES matriculas(id) ON DELETE RESTRICT` |
| incidencias_conductuales | `incidencias_conductuales_pkey` | `PRIMARY KEY (id)` |
| incidencias_conductuales | `incidencias_conductuales_reportado_por_usuario_id_fkey` | `FOREIGN KEY (reportado_por_usuario_id) REFERENCES usuarios(id) ON DELETE RESTRICT` |
| incidencias_conductuales | `incidencias_conductuales_tipo_falta_check` | `CHECK (((tipo_falta)::text = ANY ((ARRAY['LEVE'::character varying, 'GRAVE'::character varying, 'MUY_GRAVE'::character varying])::text[])))` |
| inscripciones_refuerzo | `chk_refuerzo_observaciones_longitud` | `CHECK ((char_length(observaciones) <= 500))` |
| inscripciones_refuerzo | `inscripciones_refuerzo_calificacion_origen_id_fkey` | `FOREIGN KEY (calificacion_origen_id) REFERENCES calificaciones_cneb(id) ON DELETE SET NULL` |
| inscripciones_refuerzo | `inscripciones_refuerzo_estado_asistencia_check` | `CHECK (((estado_asistencia)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'ASISTIO'::character varying, 'FALTO'::character varying, 'JUSTIFICADO'::character varying])::text[])))` |
| inscripciones_refuerzo | `inscripciones_refuerzo_estudiante_id_fkey` | `FOREIGN KEY (estudiante_id) REFERENCES estudiantes(id) ON DELETE RESTRICT` |
| inscripciones_refuerzo | `inscripciones_refuerzo_pkey` | `PRIMARY KEY (id)` |
| inscripciones_refuerzo | `inscripciones_refuerzo_sesion_refuerzo_id_estudiante_id_key` | `UNIQUE (sesion_refuerzo_id, estudiante_id)` |
| inscripciones_refuerzo | `inscripciones_refuerzo_sesion_refuerzo_id_fkey` | `FOREIGN KEY (sesion_refuerzo_id) REFERENCES sesiones_refuerzo(id) ON DELETE RESTRICT` |
| lotes_biometrico | `lotes_biometrico_importado_por_usuario_id_fkey` | `FOREIGN KEY (importado_por_usuario_id) REFERENCES usuarios(id) ON DELETE SET NULL` |
| lotes_biometrico | `lotes_biometrico_marcas_erroneas_check` | `CHECK ((marcas_erroneas >= 0))` |
| lotes_biometrico | `lotes_biometrico_marcas_validas_check` | `CHECK ((marcas_validas >= 0))` |
| lotes_biometrico | `lotes_biometrico_pkey` | `PRIMARY KEY (id)` |
| lotes_biometrico | `lotes_biometrico_total_filas_check` | `CHECK ((total_filas >= 0))` |
| lotes_biometrico | `uq_lotes_biometrico_hash_contenido` | `UNIQUE (huella_contenido)` |
| marcas_biometrico_porteria | `marcas_biometrico_porteria_estado_procesamiento_check` | `CHECK (((estado_procesamiento)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'CONCILIADO'::character varying, 'DNI_NO_IDENTIFICADO'::character varying, 'DUPLICADO'::character varying])::text[])))` |
| marcas_biometrico_porteria | `marcas_biometrico_porteria_estudiante_id_fkey` | `FOREIGN KEY (estudiante_id) REFERENCES estudiantes(id) ON DELETE SET NULL` |
| marcas_biometrico_porteria | `marcas_biometrico_porteria_lote_id_fkey` | `FOREIGN KEY (lote_id) REFERENCES lotes_biometrico(id) ON DELETE SET NULL` |
| marcas_biometrico_porteria | `marcas_biometrico_porteria_pkey` | `PRIMARY KEY (id)` |
| matriculas | `chk_matriculas_observaciones_longitud` | `CHECK ((char_length(observaciones) <= 500))` |
| matriculas | `matriculas_anio_lectivo_id_estudiante_id_key` | `UNIQUE (anio_lectivo_id, estudiante_id)` |
| matriculas | `matriculas_check` | `CHECK ((((estado_matricula)::text <> 'RESERVADA_TEMPORAL'::text) OR (fecha_expiracion_reserva IS NOT NULL)))` |
| matriculas | `matriculas_estado_matricula_check` | `CHECK (((estado_matricula)::text = ANY ((ARRAY['SOLICITADA'::character varying, 'RESERVADA_TEMPORAL'::character varying, 'MATRICULADO'::character varying, 'TRASLADADO'::character varying, 'RETIRADO'::character varying, 'CANCELADA'::character varying])::text[])))` |
| matriculas | `matriculas_estudiante_id_fkey` | `FOREIGN KEY (estudiante_id) REFERENCES estudiantes(id) ON DELETE RESTRICT` |
| matriculas | `matriculas_id_anio_lectivo_id_key` | `UNIQUE (id, anio_lectivo_id)` |
| matriculas | `matriculas_id_anio_lectivo_id_seccion_id_key` | `UNIQUE (id, anio_lectivo_id, seccion_id)` |
| matriculas | `matriculas_pkey` | `PRIMARY KEY (id)` |
| matriculas | `matriculas_seccion_id_anio_lectivo_id_fkey` | `FOREIGN KEY (seccion_id, anio_lectivo_id) REFERENCES secciones(id, anio_lectivo_id) ON DELETE RESTRICT` |
| niveles | `niveles_codigo_check` | `CHECK (((codigo_nivel)::text = ANY ((ARRAY['INICIAL'::character varying, 'PRIMARIA'::character varying, 'SECUNDARIA'::character varying])::text[])))` |
| niveles | `niveles_codigo_key` | `UNIQUE (codigo_nivel)` |
| niveles | `niveles_pkey` | `PRIMARY KEY (id)` |
| obligaciones_pago | `obligaciones_pago_check` | `CHECK ((total_pagado <= ((monto_base + monto_mora) - monto_descuento)))` |
| obligaciones_pago | `obligaciones_pago_check1` | `CHECK (((((tipo_concepto)::text = 'MATRICULA'::text) AND (numero_cuota = 0)) OR (((tipo_concepto)::text = 'PENSION'::text) AND ((numero_cuota >= 1) AND (numero_cuota <= 10))) OR ((tipo_concepto)::text <> ALL ((ARRAY['MATRICULA'::character varying, 'PENSION'::character varying])::text[]))))` |
| obligaciones_pago | `obligaciones_pago_concepto_id_tipo_concepto_fkey` | `FOREIGN KEY (concepto_id, tipo_concepto) REFERENCES conceptos_cobro(id, tipo_concepto) ON DELETE RESTRICT` |
| obligaciones_pago | `obligaciones_pago_estado_check` | `CHECK (((estado)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'PAGADO_TOTAL'::character varying, 'PAGADO_PARCIAL'::character varying, 'VENCIDO'::character varying, 'CANCELADO_POR_TRASLADO'::character varying])::text[])))` |
| obligaciones_pago | `obligaciones_pago_matricula_id_concepto_id_numero_cuota_key` | `UNIQUE (matricula_id, concepto_id, numero_cuota)` |
| obligaciones_pago | `obligaciones_pago_matricula_id_fkey` | `FOREIGN KEY (matricula_id) REFERENCES matriculas(id) ON DELETE RESTRICT` |
| obligaciones_pago | `obligaciones_pago_monto_base_check` | `CHECK ((monto_base >= 0.00))` |
| obligaciones_pago | `obligaciones_pago_monto_descuento_check` | `CHECK ((monto_descuento >= 0.00))` |
| obligaciones_pago | `obligaciones_pago_monto_mora_check` | `CHECK ((monto_mora >= 0.00))` |
| obligaciones_pago | `obligaciones_pago_numero_cuota_check` | `CHECK (((numero_cuota >= 0) AND (numero_cuota <= 10)))` |
| obligaciones_pago | `obligaciones_pago_pkey` | `PRIMARY KEY (id)` |
| obligaciones_pago | `obligaciones_pago_total_pagado_check` | `CHECK ((total_pagado >= 0.00))` |
| pagos_transacciones | `pagos_transacciones_estado_pago_check` | `CHECK (((estado_pago)::text = ANY ((ARRAY['APROBADO'::character varying, 'RECHAZADO'::character varying, 'REVERTIDO'::character varying])::text[])))` |
| pagos_transacciones | `pagos_transacciones_metodo_pago_check` | `CHECK (((metodo_pago)::text = ANY ((ARRAY['TARJETA_CREDITO'::character varying, 'TARJETA_DEBITO'::character varying, 'YAPE'::character varying, 'PLIN'::character varying, 'TRANSFERENCIA'::character varying, 'EFECTIVO'::character varying])::text[])))` |
| pagos_transacciones | `pagos_transacciones_monto_pagado_check` | `CHECK ((monto_pagado > 0.00))` |
| pagos_transacciones | `pagos_transacciones_obligacion_pago_id_fkey` | `FOREIGN KEY (obligacion_pago_id) REFERENCES obligaciones_pago(id) ON DELETE RESTRICT` |
| pagos_transacciones | `pagos_transacciones_pasarela_proveedor_check` | `CHECK (((pasarela_proveedor)::text = ANY ((ARRAY['CULQI'::character varying, 'NIUBIZ'::character varying, 'MERCADO_PAGO'::character varying, 'CAJA_EFECTIVO'::character varying, 'TRANSFERENCIA'::character varying])::text[])))` |
| pagos_transacciones | `pagos_transacciones_pasarela_transaccion_id_key` | `UNIQUE (pasarela_transaccion_id)` |
| pagos_transacciones | `pagos_transacciones_pkey` | `PRIMARY KEY (id)` |
| periodos_academicos | `periodos_academicos_anio_lectivo_id_fkey` | `FOREIGN KEY (anio_lectivo_id) REFERENCES anios_lectivos(id) ON DELETE RESTRICT` |
| periodos_academicos | `periodos_academicos_anio_lectivo_id_numero_periodo_key` | `UNIQUE (anio_lectivo_id, numero_periodo)` |
| periodos_academicos | `periodos_academicos_check` | `CHECK ((fecha_fin > fecha_inicio))` |
| periodos_academicos | `periodos_academicos_id_anio_lectivo_id_key` | `UNIQUE (id, anio_lectivo_id)` |
| periodos_academicos | `periodos_academicos_numero_periodo_check` | `CHECK (((numero_periodo >= 1) AND (numero_periodo <= 4)))` |
| periodos_academicos | `periodos_academicos_pkey` | `PRIMARY KEY (id)` |
| roles | `roles_codigo_key` | `UNIQUE (codigo_rol)` |
| roles | `roles_pkey` | `PRIMARY KEY (id)` |
| secciones | `fk_secciones_aula` | `FOREIGN KEY (aula_id) REFERENCES aulas(id) ON DELETE RESTRICT` |
| secciones | `secciones_anio_lectivo_id_fkey` | `FOREIGN KEY (anio_lectivo_id) REFERENCES anios_lectivos(id) ON DELETE RESTRICT` |
| secciones | `secciones_anio_lectivo_id_grado_id_letra_key` | `UNIQUE (anio_lectivo_id, grado_id, letra)` |
| secciones | `secciones_check` | `CHECK (((vacantes_ocupadas >= 0) AND (vacantes_ocupadas <= cupo_maximo)))` |
| secciones | `secciones_cupo_maximo_check` | `CHECK ((cupo_maximo > 0))` |
| secciones | `secciones_grado_id_nivel_id_fkey` | `FOREIGN KEY (grado_id, nivel_id) REFERENCES grados(id, nivel_id) ON DELETE RESTRICT` |
| secciones | `secciones_id_anio_lectivo_id_key` | `UNIQUE (id, anio_lectivo_id)` |
| secciones | `secciones_id_anio_lectivo_id_nivel_id_key` | `UNIQUE (id, anio_lectivo_id, nivel_id)` |
| secciones | `secciones_letra_check` | `CHECK ((letra ~ '^[A-Z]$'::text))` |
| secciones | `secciones_pkey` | `PRIMARY KEY (id)` |
| series_comprobante | `series_comprobante_pkey` | `PRIMARY KEY (serie)` |
| series_comprobante | `series_comprobante_serie_check` | `CHECK (((serie)::text ~ '^[BE][0-9]{3}$'::text))` |
| series_comprobante | `series_comprobante_ultimo_correlativo_check` | `CHECK ((ultimo_correlativo >= 0))` |
| sesiones | `sesiones_pkey` | `PRIMARY KEY (id)` |
| sesiones | `sesiones_usuario_id_fkey` | `FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE` |
| sesiones_refuerzo | `fk_sesiones_refuerzo_aula_codigo` | `FOREIGN KEY (codigo_aula) REFERENCES aulas(codigo_aula) ON UPDATE RESTRICT ON DELETE RESTRICT` |
| sesiones_refuerzo | `sesiones_refuerzo_area_curricular_id_fkey` | `FOREIGN KEY (area_curricular_id) REFERENCES areas_curriculares(id) ON DELETE RESTRICT` |
| sesiones_refuerzo | `sesiones_refuerzo_check` | `CHECK ((hora_fin > hora_inicio))` |
| sesiones_refuerzo | `sesiones_refuerzo_docente_usuario_id_fkey` | `FOREIGN KEY (docente_usuario_id) REFERENCES usuarios(id) ON DELETE RESTRICT` |
| sesiones_refuerzo | `sesiones_refuerzo_periodo_academico_id_anio_lectivo_id_fkey` | `FOREIGN KEY (periodo_academico_id, anio_lectivo_id) REFERENCES periodos_academicos(id, anio_lectivo_id) ON DELETE RESTRICT` |
| sesiones_refuerzo | `sesiones_refuerzo_pkey` | `PRIMARY KEY (id)` |
| solicitudes_matricula_documentos | `fk_solicitudes_documentos_solicitud` | `FOREIGN KEY (solicitud_id) REFERENCES solicitudes_matricula_publica(id) ON UPDATE RESTRICT ON DELETE RESTRICT` |
| solicitudes_matricula_documentos | `solicitudes_matricula_documentos_bucket_object_key_key` | `UNIQUE (contenedor, ruta_objeto)` |
| solicitudes_matricula_documentos | `solicitudes_matricula_documentos_mime_type_check` | `CHECK (((tipo_contenido)::text = ANY (ARRAY[('application/pdf'::character varying)::text, ('image/jpeg'::character varying)::text, ('image/png'::character varying)::text, ('image/webp'::character varying)::text])))` |
| solicitudes_matricula_documentos | `solicitudes_matricula_documentos_pkey` | `PRIMARY KEY (solicitud_id, tipo)` |
| solicitudes_matricula_documentos | `solicitudes_matricula_documentos_tamano_bytes_check` | `CHECK (((tamano_bytes >= 1) AND (tamano_bytes <= 5242880)))` |
| solicitudes_matricula_documentos | `solicitudes_matricula_documentos_tipo_check` | `CHECK (((tipo)::text = ANY ((ARRAY['PARTIDA_NACIMIENTO'::character varying, 'DNI_C4'::character varying, 'RECIBO_SERVICIO'::character varying])::text[])))` |
| solicitudes_matricula_publica | `chk_solicitud_matricula_comprobante_confirmada` | `CHECK ((((estado)::text <> 'MATRICULADA'::text) OR (comprobante_pago_codigo IS NOT NULL)))` |
| solicitudes_matricula_publica | `chk_solicitud_matricula_documentos_emitidos` | `CHECK (((comprobante_pago_codigo IS NULL) = (fecha_emision_documentos IS NULL)))` |
| solicitudes_matricula_publica | `chk_solicitudes_matricula_pago_monto_positivo` | `CHECK ((pago_monto > (0)::numeric))` |
| solicitudes_matricula_publica | `solicitudes_matricula_publica_anio_lectivo_id_fkey` | `FOREIGN KEY (anio_lectivo_id) REFERENCES anios_lectivos(id) ON DELETE RESTRICT` |
| solicitudes_matricula_publica | `solicitudes_matricula_publica_apoderado_id_fkey` | `FOREIGN KEY (apoderado_id) REFERENCES apoderados(id) ON DELETE RESTRICT` |
| solicitudes_matricula_publica | `solicitudes_matricula_publica_celular_apoderado_check` | `CHECK (((celular_apoderado)::text ~ '^9[0-9]{8}$'::text))` |
| solicitudes_matricula_publica | `solicitudes_matricula_publica_check` | `CHECK (((NOT vacante_reservada) OR (fecha_expiracion_pago IS NOT NULL)))` |
| solicitudes_matricula_publica | `solicitudes_matricula_publica_check1` | `CHECK ((((estado)::text <> 'MATRICULADA'::text) OR ((estudiante_id IS NOT NULL) AND (apoderado_id IS NOT NULL) AND (matricula_id IS NOT NULL) AND (pago_id IS NOT NULL))))` |
| solicitudes_matricula_publica | `solicitudes_matricula_publica_consentimiento_gemini_check` | `CHECK ((consentimiento_gemini IS TRUE))` |
| solicitudes_matricula_publica | `solicitudes_matricula_publica_estado_check` | `CHECK (((estado)::text = ANY ((ARRAY['DOCUMENTOS_PENDIENTES'::character varying, 'DOCUMENTOS_OBSERVADOS'::character varying, 'DOCUMENTOS_VALIDADOS'::character varying, 'PAGO_PENDIENTE'::character varying, 'MATRICULADA'::character varying, 'RECHAZADA'::character varying])::text[])))` |
| solicitudes_matricula_publica | `solicitudes_matricula_publica_estado_dni_c4_check` | `CHECK (((estado_dni_c4)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'VALIDADO'::character varying, 'OBSERVADO'::character varying])::text[])))` |
| solicitudes_matricula_publica | `solicitudes_matricula_publica_estado_partida_check` | `CHECK (((estado_partida)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'VALIDADO'::character varying, 'OBSERVADO'::character varying])::text[])))` |
| solicitudes_matricula_publica | `solicitudes_matricula_publica_estado_recibo_check` | `CHECK (((estado_recibo)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'VALIDADO'::character varying, 'OBSERVADO'::character varying])::text[])))` |
| solicitudes_matricula_publica | `solicitudes_matricula_publica_estudiante_id_fkey` | `FOREIGN KEY (estudiante_id) REFERENCES estudiantes(id) ON DELETE RESTRICT` |
| solicitudes_matricula_publica | `solicitudes_matricula_publica_genero_estudiante_check` | `CHECK ((genero_estudiante = ANY (ARRAY['M'::bpchar, 'F'::bpchar])))` |
| solicitudes_matricula_publica | `solicitudes_matricula_publica_matricula_id_fkey` | `FOREIGN KEY (matricula_id) REFERENCES matriculas(id) ON DELETE RESTRICT` |
| solicitudes_matricula_publica | `solicitudes_matricula_publica_numero_documento_apoderado_check` | `CHECK (((numero_documento_apoderado)::text ~ '^[0-9]{8}$'::text))` |
| solicitudes_matricula_publica | `solicitudes_matricula_publica_numero_documento_estudiante_check` | `CHECK (((numero_documento_estudiante)::text ~ '^[0-9]{8}$'::text))` |
| solicitudes_matricula_publica | `solicitudes_matricula_publica_parentesco_check` | `CHECK (((parentesco)::text = ANY ((ARRAY['PADRE'::character varying, 'MADRE'::character varying, 'TUTOR_LEGAL'::character varying, 'ABUELO_A'::character varying, 'OTRO'::character varying])::text[])))` |
| solicitudes_matricula_publica | `solicitudes_matricula_publica_pkey` | `PRIMARY KEY (id)` |
| solicitudes_matricula_publica | `solicitudes_matricula_publica_seccion_id_anio_lectivo_id_fkey` | `FOREIGN KEY (seccion_id, anio_lectivo_id) REFERENCES secciones(id, anio_lectivo_id) ON DELETE RESTRICT` |
| solicitudes_matricula_publica | `solicitudes_matricula_publica_token_hash_key` | `UNIQUE (huella_token)` |
| solicitudes_matricula_publica | `solicitudes_matricula_publica_ubigeo_apoderado_check` | `CHECK (((ubigeo_apoderado)::text ~ '^[0-9]{6}$'::text))` |
| usuario_roles | `usuario_roles_pkey` | `PRIMARY KEY (usuario_id, rol_id)` |
| usuario_roles | `usuario_roles_rol_id_fkey` | `FOREIGN KEY (rol_id) REFERENCES roles(id) ON DELETE RESTRICT` |
| usuario_roles | `usuario_roles_usuario_id_fkey` | `FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE` |
| usuarios | `usuarios_email_key` | `UNIQUE (correo_electronico)` |
| usuarios | `usuarios_pkey` | `PRIMARY KEY (id)` |
| usuarios | `usuarios_username_key` | `UNIQUE (nombre_usuario)` |

## Verificación

- Migraciones V1–V16 y consolidado equivalentes en PostgreSQL/Supabase real.
- Límites 20/21 y 500/501, rechazo sin truncado y trigger de actualización comprobados.
- Los dos esquemas de comparación se revirtieron completamente.
- Backend: 106 pruebas, 0 fallos, 0 errores y 0 omitidas; BUILD SUCCESS. Incluye seis PersistenciaPostgresTest contra PostgreSQL 17.6/Supabase y pruebas de seguridad, controladores, dominio y arquitectura.
- Selección ejecutada: todas excepto CargaYRegresionNPlusOneTest y ConcurrenciaPostgresTest; estos dos grupos no se repitieron por no cambiar el comportamiento de carga ni la asignación de correlativos.
- Frontend: npm run build -- --configuration development completado correctamente.
- Esquema de pruebas JPA eliminado al terminar. Los dos SQL consolidados son idénticos (SHA-256).
