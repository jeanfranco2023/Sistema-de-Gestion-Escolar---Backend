# Matriz completa de atributos de la base de datos — Colegio Shuji Kitamura

**Fecha:** 2026-09-24  
**Alcance:** migraciones Flyway V1–V12 y alineación de entidades/validaciones del backend.  
**Estado:** V12 y sus cambios de aplicación están preparados localmente; V1–V11 permanecen inmutables. No se ejecutó contra una BD real y `.env` no se tocó.

## Decisiones integradas para V12

| Atributo | Esquema final planteado | Criterio |
|---|---:|---|
| Apellidos paterno y materno | `VARCHAR(20)` cada uno | Campos separados en solicitudes, estudiantes y apoderados. |
| Nombres de solicitud pública | `VARCHAR(20)` | Mantiene el límite indicado para el formulario. Los registros maestros admiten nombres más largos por carga administrativa. |
| `roles.nombre` | `VARCHAR(30)` | Cabe la etiqueta actual más extensa (`Padre de Familia / Tutor`, 24 caracteres). Es variable con límite; `VARCHAR(30)` no reserva 30 caracteres por fila. |
| `competencias.nombre` | `VARCHAR(50)` | Título breve. La descripción curricular extensa va en `descripcion TEXT`. |
| `comunicados_oficiales.titulo` | `VARCHAR(40)` | Acepta títulos breves como “Comunicado por fiestas patrias”; el cuerpo es `TEXT`. |
| `sesiones_refuerzo.tema` | `VARCHAR(40)` | Tema corto; no es la descripción de la sesión. |
| `aulas.codigo / nombre` | `VARCHAR(30) / VARCHAR(40)` | Código estable, etiqueta legible. |
| Email | `VARCHAR(254)` | Límite suficiente para direcciones de correo válidas largas; se amplía en usuarios, apoderados y solicitudes. |
| Dirección | `VARCHAR(200)` | Se conserva el límite actual para no rechazar domicilios completos. |
| `periodos_academicos.activo` | `BOOLEAN NOT NULL DEFAULT TRUE` | Permite dejar inactivo un bimestre sin borrar filas ni su historial. |
| `sesiones_refuerzo.aula_asignada` | `VARCHAR(30)` con FK a `aulas.codigo` | Conserva el contrato actual y agrega integridad referencial a la clave única del aula. |

## Reglas operativas acordadas

- Los períodos conservan nombres fijos como “I Bimestre”, “II Bimestre”, etc. Puede haber cuatro filas y solo tres activas; `activo` controla el calendario y `cerrado` continúa controlando si se pueden editar notas. La fila inactiva y sus referencias no se borran.
- En niveles se conservan `id`, `codigo` y `nombre`: `id` enlaza FKs, `codigo` es la clave semántica usada por V11 y `nombre` es la etiqueta visible (“Educación Inicial”). No duplican la misma función.
- En grados se conservan `nivel_id`, `numero_grado` y `nombre`: el nivel y el número identifican/ordenan; el nombre es la etiqueta que ve el usuario, en especial “Inicial 3 años”.
- En aulas se conservan `codigo`, `nombre` y `ubicacion`: código estable (“AULA_P_101”), etiqueta (“Pabellón Primaria - Aula 101”) y ubicación física (“Pabellón B - Piso 1”).
- PostgreSQL no reserva el máximo de un `VARCHAR(n)` por fila; cambiar límites controla lo que se acepta, no reduce el espacio de valores cortos. `VARCHAR(n)` es variable y además impone el máximo; `TEXT` conviene para descripciones sin longitud funcional fija.

## Salvaguardas antes de aplicar V12 en la base real

| Riesgo | Salvaguarda |
|---|---|
| Filas existentes con apellidos/nombres/títulos/temas sobre el nuevo límite | V12 comprueba longitudes con `char_length`; si encuentra excedentes, aborta sin truncar ni eliminar filas. Se revisan valores y se decide cómo corregirlos antes de reintentar. |
| `aula_asignada` contiene texto que no es un código de `aulas` | V12 comprueba correspondencia ignorando espacios y mayúsculas; si no encuentra aula, aborta sin perder el texto. Si sí corresponde, normaliza al código canónico y agrega la FK con `ON DELETE/UPDATE RESTRICT`. |
| Más de un código de aula coincide tras normalizar espacios y mayúsculas | V12 aborta antes del UPDATE si la coincidencia es ambigua; no asigna una sesión a un aula arbitraria. |
| Datos reales de Supabase no inspeccionados | La matriz y la migración están preparadas; no ejecuté V12 contra la base remota porque no hay una conexión de BD activa en este entorno. |

## Longitudes seleccionadas con criterio de uso

| Campo | Longitud | Motivo |
|---|---:|---|
| `roles.nombre` | 30 | Conserva las etiquetas sembradas actualmente. |
| `competencias.nombre` | 50 | Reservado al título breve; texto curricular ampliado en `descripcion TEXT`. |
| `comunicados_oficiales.titulo` | 40 | Suficiente para el ejemplo proporcionado. |
| `sesiones_refuerzo.tema` | 40 | Suficiente para un tema breve; el detalle va por separado si se necesita. |
| Apellidos | 20 cada apellido | Decisión explícita; paterno y materno quedan en columnas separadas. |
| Email | 254 | Evita rechazar correos válidos largos. |
| Dirección | 200 | Mantiene capacidad para domicilio completo sin estrechar datos existentes. |
| `niveles.nombre` | 30 | Incluye etiquetas institucionales actuales y posibles variantes cortas. |
| `grados.nombre` | 30 | Cubre las etiquetas sembradas como “Inicial 3 años”. |
| `aulas.nombre` | 40 | Cubre las etiquetas sembradas de pabellón y aula. |

## Atributos por entidad
### 1. Seguridad, acceso y auditoría
#### `roles`

| Atributo | Tipo y reglas DDL | Observación para revisión |
|---|---|---|
| `id` | `SMALLSERIAL PRIMARY KEY` |  |
| `codigo` | `VARCHAR(30) UNIQUE NOT NULL` |  |
| `nombre` | `VARCHAR(30) NOT NULL` | V1–V11: `VARCHAR(50)`; V12: 30. Etiqueta actual más larga = 24 caracteres. |
| `descripcion` | `VARCHAR(255)` |  |

#### `usuarios`

| Atributo | Tipo y reglas DDL | Observación para revisión |
|---|---|---|
| `id` | `BIGSERIAL PRIMARY KEY` |  |
| `uuid` | `UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL` |  |
| `username` | `VARCHAR(50) UNIQUE NOT NULL` |  |
| `email` | `VARCHAR(254) UNIQUE NOT NULL` | V1–V11: `VARCHAR(100)`; V12 amplía el límite a 254. |
| `password_hash` | `VARCHAR(60) NOT NULL` |  |
| `activo` | `BOOLEAN DEFAULT TRUE NOT NULL` |  |
| `created_at` | `TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL` | Revisar coherencia con Java; preferir OffsetDateTime. |
| `updated_at` | `TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL` | Revisar coherencia con Java; preferir OffsetDateTime. |

#### `usuario_roles`

| Atributo | Tipo y reglas DDL | Observación para revisión |
|---|---|---|
| `usuario_id` | `BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE` |  |
| `rol_id` | `SMALLINT NOT NULL REFERENCES roles(id) ON DELETE RESTRICT` |  |

#### `sesiones`

| Atributo | Tipo y reglas DDL | Observación para revisión |
|---|---|---|
| `id` | `BIGSERIAL PRIMARY KEY` |  |
| `usuario_id` | `BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE` |  |
| `token_hash` | `VARCHAR(64) NOT NULL` |  |
| `ip_address` | `INET` |  |
| `user_agent` | `TEXT` |  |
| `expira_at` | `TIMESTAMPTZ NOT NULL` | Revisar coherencia con Java; preferir OffsetDateTime. |
| `created_at` | `TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL` | Revisar coherencia con Java; preferir OffsetDateTime. |

#### `auditoria_cambios`

| Atributo | Tipo y reglas DDL | Observación para revisión |
|---|---|---|
| `id` | `BIGSERIAL PRIMARY KEY` |  |
| `tabla_afectada` | `VARCHAR(50) NOT NULL` |  |
| `registro_id` | `BIGINT NOT NULL` |  |
| `accion` | `VARCHAR(10) NOT NULL CHECK (accion IN ('INSERT', 'UPDATE', 'DELETE'))` |  |
| `datos_anteriores` | `JSONB` |  |
| `datos_nuevos` | `JSONB` |  |
| `usuario_id` | `BIGINT REFERENCES usuarios(id) ON DELETE SET NULL` |  |
| `created_at` | `TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL` | Revisar coherencia con Java; preferir OffsetDateTime. |

### 2. Calendario y estructura académica
#### `anios_lectivos`

| Atributo | Tipo y reglas DDL | Observación para revisión |
|---|---|---|
| `id` | `SMALLSERIAL PRIMARY KEY` |  |
| `anio` | `SMALLINT UNIQUE NOT NULL CHECK (anio >= 2024)` |  |
| `fecha_inicio` | `DATE NOT NULL` |  |
| `fecha_fin` | `DATE NOT NULL` |  |
| `abierto` | `BOOLEAN DEFAULT TRUE NOT NULL` |  |

#### `periodos_academicos`

| Atributo | Tipo y reglas DDL | Observación para revisión |
|---|---|---|
| `id` | `SMALLSERIAL PRIMARY KEY` |  |
| `anio_lectivo_id` | `SMALLINT NOT NULL REFERENCES anios_lectivos(id) ON DELETE RESTRICT` |  |
| `numero_periodo` | `SMALLINT NOT NULL CHECK (numero_periodo BETWEEN 1 AND 4)` |  |
| `nombre` | `VARCHAR(30) NOT NULL` | Nombre fijo de presentación (“I Bimestre”). `numero_periodo` conserva el orden; V12 agrega `activo` para ciclos de tres o cuatro bimestres. |
| `fecha_inicio` | `DATE NOT NULL` |  |
| `fecha_fin` | `DATE NOT NULL` |  |
| `cerrado` | `BOOLEAN DEFAULT FALSE NOT NULL` | Cierre de edición de notas; conserva el significado actual. |
| `activo` | `BOOLEAN NOT NULL DEFAULT TRUE` | V12: desactiva el período sin borrar la fila ni sus datos históricos. |

#### `niveles`

| Atributo | Tipo y reglas DDL | Observación para revisión |
|---|---|---|
| `id` | `SMALLSERIAL PRIMARY KEY` |  |
| `codigo` | `VARCHAR(20) UNIQUE NOT NULL CHECK (codigo IN ('INICIAL', 'PRIMARIA', 'SECUNDARIA'))` | Clave semántica distinta de `id` y `nombre`; V11 usa esta columna al sembrar grados y secciones. |
| `nombre` | `VARCHAR(30) NOT NULL` | Etiqueta visible como “Educación Inicial”; V12 baja el límite actual de 50 a 30. |

#### `grados`

| Atributo | Tipo y reglas DDL | Observación para revisión |
|---|---|---|
| `id` | `SMALLSERIAL PRIMARY KEY` |  |
| `nivel_id` | `SMALLINT NOT NULL REFERENCES niveles(id) ON DELETE RESTRICT` |  |
| `numero_grado` | `SMALLINT NOT NULL CHECK (numero_grado BETWEEN 1 AND 6)` |  |
| `nombre` | `VARCHAR(30) NOT NULL` | Etiqueta visible como “Inicial 3 años”; `nivel_id + numero_grado` ordenan e identifican el grado. V12 baja 50 a 30. |

#### `secciones`

| Atributo | Tipo y reglas DDL | Observación para revisión |
|---|---|---|
| `id` | `SERIAL PRIMARY KEY` |  |
| `anio_lectivo_id` | `SMALLINT NOT NULL REFERENCES anios_lectivos(id) ON DELETE RESTRICT` |  |
| `grado_id` | `SMALLINT NOT NULL` |  |
| `nivel_id` | `SMALLINT NOT NULL` |  |
| `letra` | `CHAR(1) NOT NULL CHECK (letra ~ '^[A-Z]$')` |  |
| `cupo_maximo` | `SMALLINT NOT NULL CHECK (cupo_maximo > 0)` |  |
| `vacantes_ocupadas` | `SMALLINT DEFAULT 0 NOT NULL CHECK (vacantes_ocupadas >= 0 AND vacantes_ocupadas <= cupo_maximo)` |  |
| `aula_fisica` | `VARCHAR(30)` | Código textual duplicado: V3 añade `aula_id` FK y copia aquí `aulas.codigo`. Evaluar retirar tras adaptar lectores. |

#### `aulas`

| Atributo | Tipo y reglas DDL | Observación para revisión |
|---|---|---|
| `id` | `SERIAL PRIMARY KEY` |  |
| `codigo` | `VARCHAR(30) UNIQUE NOT NULL` |  |
| `nombre` | `VARCHAR(40) NOT NULL` | Etiqueta legible separada del código y la ubicación. V1–V11: `VARCHAR(100)`; V12 aplica 40. |
| `ubicacion` | `VARCHAR(150)` |  |
| `capacidad` | `SMALLINT NOT NULL CHECK (capacidad > 0)` |  |
| `activa` | `BOOLEAN DEFAULT TRUE NOT NULL` |  |

### 3. Personas y matrícula
#### `apoderados`

| Atributo | Tipo y reglas DDL | Observación para revisión |
|---|---|---|
| `id` | `BIGSERIAL PRIMARY KEY` |  |
| `uuid` | `UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL` |  |
| `usuario_id` | `BIGINT UNIQUE REFERENCES usuarios(id) ON DELETE SET NULL` |  |
| `tipo_documento` | `VARCHAR(10) DEFAULT 'DNI' NOT NULL CHECK (tipo_documento IN ('DNI', 'CE', 'PASAPORTE'))` |  |
| `numero_documento` | `VARCHAR(15) NOT NULL` |  |
| `nombres` | `VARCHAR(100) NOT NULL` | Límite maestro amplio para ingreso administrativo; las solicitudes públicas limitan nombres a 20. |
| `apellido_paterno` | `VARCHAR(20) NOT NULL` | V1–V11: `VARCHAR(80)`; V12 aplica 20. Campo separado de apellido materno. |
| `apellido_materno` | `VARCHAR(20) NOT NULL` | V1–V11: `VARCHAR(80)`; V12 aplica 20. Campo separado de apellido paterno. |
| `celular` | `VARCHAR(9) NOT NULL CHECK (celular ~ '^9[0-9]{8}$')` |  |
| `email` | `VARCHAR(254)` | V1–V11: `VARCHAR(100)`; V12 amplía a 254. |
| `direccion` | `VARCHAR(200) NOT NULL` |  |
| `ubigeo_inei` | `VARCHAR(6) NOT NULL CHECK (ubigeo_inei ~ '^[0-9]{6}$')` |  |
| `validado_reniec` | `BOOLEAN DEFAULT FALSE NOT NULL` |  |
| `origen_registro` | `VARCHAR(20) DEFAULT 'RENIEC_API' NOT NULL CHECK (origen_registro IN ('RENIEC_API', 'MANUAL_CONTINGENCIA'))` |  |
| `created_at` | `TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL` | Revisar coherencia con Java; preferir OffsetDateTime. |

#### `estudiantes`

| Atributo | Tipo y reglas DDL | Observación para revisión |
|---|---|---|
| `id` | `BIGSERIAL PRIMARY KEY` |  |
| `uuid` | `UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL` |  |
| `tipo_documento` | `VARCHAR(10) DEFAULT 'DNI' NOT NULL CHECK (tipo_documento IN ('DNI', 'CE', 'PASAPORTE'))` |  |
| `numero_documento` | `VARCHAR(15) NOT NULL` |  |
| `nombres` | `VARCHAR(100) NOT NULL` | Límite maestro amplio para ingreso administrativo; solicitudes públicas limitan nombres a 20. |
| `apellido_paterno` | `VARCHAR(20) NOT NULL` | V1–V11: `VARCHAR(80)`; V12 aplica 20. Campo separado de apellido materno. |
| `apellido_materno` | `VARCHAR(20) NOT NULL` | V1–V11: `VARCHAR(80)`; V12 aplica 20. Campo separado de apellido paterno. |
| `fecha_nacimiento` | `DATE NOT NULL` |  |
| `genero` | `CHAR(1) NOT NULL CHECK (genero IN ('M', 'F'))` |  |
| `codigo_estudiante_siagie` | `VARCHAR(14) UNIQUE` |  |
| `validado_reniec` | `BOOLEAN DEFAULT FALSE NOT NULL` |  |
| `origen_registro` | `VARCHAR(20) DEFAULT 'RENIEC_API' NOT NULL CHECK (origen_registro IN ('RENIEC_API', 'MANUAL_CONTINGENCIA'))` |  |
| `grupo_sanguineo` | `VARCHAR(5)` |  |
| `alergias_condiciones` | `TEXT` |  |
| `activo` | `BOOLEAN DEFAULT TRUE NOT NULL` |  |
| `created_at` | `TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL` | Revisar coherencia con Java; preferir OffsetDateTime. |

#### `estudiante_apoderados`

| Atributo | Tipo y reglas DDL | Observación para revisión |
|---|---|---|
| `id` | `BIGSERIAL PRIMARY KEY` |  |
| `estudiante_id` | `BIGINT NOT NULL REFERENCES estudiantes(id) ON DELETE RESTRICT` |  |
| `apoderado_id` | `BIGINT NOT NULL REFERENCES apoderados(id) ON DELETE RESTRICT` |  |
| `parentesco` | `VARCHAR(30) NOT NULL CHECK (parentesco IN ('PADRE', 'MADRE', 'TUTOR_LEGAL', 'ABUELO_A', 'OTRO'))` |  |
| `es_responsable_economico` | `BOOLEAN DEFAULT FALSE NOT NULL` |  |
| `tiene_custodia` | `BOOLEAN DEFAULT TRUE NOT NULL` |  |
| `permite_recojo` | `BOOLEAN DEFAULT TRUE NOT NULL` |  |

#### `matriculas`

| Atributo | Tipo y reglas DDL | Observación para revisión |
|---|---|---|
| `id` | `BIGSERIAL PRIMARY KEY` |  |
| `uuid` | `UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL` |  |
| `anio_lectivo_id` | `SMALLINT NOT NULL` |  |
| `estudiante_id` | `BIGINT NOT NULL REFERENCES estudiantes(id) ON DELETE RESTRICT` |  |
| `seccion_id` | `INT NOT NULL` |  |
| `fecha_matricula` | `TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL` | Revisar coherencia con Java; preferir OffsetDateTime. |
| `estado_matricula` | `VARCHAR(25) DEFAULT 'SOLICITADA' NOT NULL CHECK (estado_matricula IN ('SOLICITADA', 'RESERVADA_TEMPORAL', 'MATRICULADO', 'TRASLADADO', 'RETIRADO', 'CANCELADA'))` |  |
| `reserva_expira_at` | `TIMESTAMPTZ` | Revisar coherencia con Java; preferir OffsetDateTime. |
| `observaciones` | `TEXT` |  |

#### `solicitudes_matricula_publica`

| Atributo | Tipo y reglas DDL | Observación para revisión |
|---|---|---|
| `id` | `UUID PRIMARY KEY` |  |
| `token_hash` | `CHAR(64) NOT NULL UNIQUE` |  |
| `estado` | `VARCHAR(30) NOT NULL CHECK (estado IN ( 'DOCUMENTOS_PENDIENTES', 'DOCUMENTOS_OBSERVADOS', 'DOCUMENTOS_VALIDADOS', 'PAGO_PENDIENTE', 'MATRICULADA', 'RECHAZADA' ))` |  |
| `anio_lectivo_id` | `SMALLINT NOT NULL REFERENCES anios_lectivos(id) ON DELETE RESTRICT` |  |
| `seccion_id` | `INT NOT NULL` |  |
| `numero_documento_estudiante` | `VARCHAR(8) NOT NULL CHECK (numero_documento_estudiante ~ '^[0-9]{8}$')` |  |
| `nombres_estudiante` | `VARCHAR(20) NOT NULL` | V1–V11: `VARCHAR(100)`; V12 y DTO público aplican 20. |
| `apellido_paterno_estudiante` | `VARCHAR(20) NOT NULL` | V1–V11: `VARCHAR(80)`; V12 aplica 20 en columna separada. |
| `apellido_materno_estudiante` | `VARCHAR(20) NOT NULL` | V1–V11: `VARCHAR(80)`; V12 aplica 20 en columna separada. |
| `fecha_nacimiento_estudiante` | `DATE NOT NULL` |  |
| `genero_estudiante` | `CHAR(1) NOT NULL CHECK (genero_estudiante IN ('M', 'F'))` |  |
| `numero_documento_apoderado` | `VARCHAR(8) NOT NULL CHECK (numero_documento_apoderado ~ '^[0-9]{8}$')` |  |
| `nombres_apoderado` | `VARCHAR(20) NOT NULL` | V1–V11: `VARCHAR(100)`; V12 y DTO público aplican 20. |
| `apellido_paterno_apoderado` | `VARCHAR(20) NOT NULL` | V1–V11: `VARCHAR(80)`; V12 aplica 20 en columna separada. |
| `apellido_materno_apoderado` | `VARCHAR(20) NOT NULL` | V1–V11: `VARCHAR(80)`; V12 aplica 20 en columna separada. |
| `celular_apoderado` | `VARCHAR(9) NOT NULL CHECK (celular_apoderado ~ '^9[0-9]{8}$')` |  |
| `email_apoderado` | `VARCHAR(254)` | V1–V11: `VARCHAR(100)`; V12 amplía a 254. |
| `direccion_apoderado` | `VARCHAR(200) NOT NULL` | Se conserva el límite actual para domicilios completos. |
| `ubigeo_apoderado` | `VARCHAR(6) NOT NULL CHECK (ubigeo_apoderado ~ '^[0-9]{6}$')` |  |
| `parentesco` | `VARCHAR(30) NOT NULL CHECK (parentesco IN ('PADRE', 'MADRE', 'TUTOR_LEGAL', 'ABUELO_A', 'OTRO'))` |  |
| `consentimiento_gemini` | `BOOLEAN NOT NULL CHECK (consentimiento_gemini IS TRUE)` |  |
| `estado_partida` | `VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado_partida IN ('PENDIENTE', 'VALIDADO', 'OBSERVADO'))` |  |
| `observacion_partida` | `VARCHAR(500)` |  |
| `estado_dni_c4` | `VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado_dni_c4 IN ('PENDIENTE', 'VALIDADO', 'OBSERVADO'))` |  |
| `observacion_dni_c4` | `VARCHAR(500)` |  |
| `estado_recibo` | `VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado_recibo IN ('PENDIENTE', 'VALIDADO', 'OBSERVADO'))` |  |
| `observacion_recibo` | `VARCHAR(500)` |  |
| `pago_preferencia_id` | `VARCHAR(100)` |  |
| `pago_enlace` | `VARCHAR(1000)` |  |
| `pago_id` | `VARCHAR(100)` |  |
| `pago_monto` | `NUMERIC(10,2) NOT NULL DEFAULT 1.00 CHECK (pago_monto = 1.00)` |  |
| `pago_expira_at` | `TIMESTAMPTZ` | Revisar coherencia con Java; preferir OffsetDateTime. |
| `vacante_reservada` | `BOOLEAN NOT NULL DEFAULT FALSE` |  |
| `estudiante_id` | `BIGINT REFERENCES estudiantes(id) ON DELETE RESTRICT` |  |
| `apoderado_id` | `BIGINT REFERENCES apoderados(id) ON DELETE RESTRICT` |  |
| `matricula_id` | `BIGINT REFERENCES matriculas(id) ON DELETE RESTRICT` |  |
| `created_at` | `TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP` | Revisar coherencia con Java; preferir OffsetDateTime. |
| `updated_at` | `TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP` | Revisar coherencia con Java; preferir OffsetDateTime. |
| `version` | `BIGINT NOT NULL DEFAULT 0` |  |

#### `solicitudes_matricula_documentos`

| Atributo | Tipo y reglas DDL | Observación para revisión |
|---|---|---|
| `solicitud_id` | `UUID NOT NULL REFERENCES solicitudes_matricula_publica(id) ON DELETE CASCADE` |  |
| `tipo` | `VARCHAR(30) NOT NULL CHECK (tipo IN ('PARTIDA_NACIMIENTO', 'DNI_C4', 'RECIBO_SERVICIO'))` |  |
| `bucket` | `VARCHAR(100) NOT NULL` |  |
| `object_key` | `VARCHAR(500) NOT NULL` |  |
| `mime_type` | `VARCHAR(100) NOT NULL CHECK (mime_type IN ('application/pdf', 'image/jpeg', 'image/png', 'image/webp'))` |  |
| `tamano_bytes` | `BIGINT NOT NULL CHECK (tamano_bytes BETWEEN 1 AND 5242880)` |  |
| `sha256` | `CHAR(64) NOT NULL` | Revisar alineación DDL/Java. |
| `actualizado_at` | `TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP` | Revisar coherencia con Java; preferir OffsetDateTime. |

### 4. Currículo y organización docente
#### `areas_curriculares`

| Atributo | Tipo y reglas DDL | Observación para revisión |
|---|---|---|
| `id` | `SMALLSERIAL PRIMARY KEY` |  |
| `nivel_id` | `SMALLINT NOT NULL REFERENCES niveles(id) ON DELETE RESTRICT` |  |
| `codigo` | `VARCHAR(20) NOT NULL` |  |
| `nombre` | `VARCHAR(100) NOT NULL` | Longitud exacta pendiente. |

#### `competencias`

| Atributo | Tipo y reglas DDL | Observación para revisión |
|---|---|---|
| `id` | `SMALLSERIAL PRIMARY KEY` |  |
| `area_id` | `SMALLINT NOT NULL REFERENCES areas_curriculares(id) ON DELETE CASCADE` |  |
| `numero_orden` | `SMALLINT NOT NULL CHECK (numero_orden > 0)` |  |
| `nombre` | `VARCHAR(50) NOT NULL` | V1–V11: `VARCHAR(150)`; V12 aplica 50 al título. Los descriptores largos van en `descripcion TEXT`. |
| `descripcion` | `TEXT` |  |

#### `asignaciones_docentes`

| Atributo | Tipo y reglas DDL | Observación para revisión |
|---|---|---|
| `id` | `BIGSERIAL PRIMARY KEY` |  |
| `docente_usuario_id` | `BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT` |  |
| `seccion_id` | `INT NOT NULL` |  |
| `anio_lectivo_id` | `SMALLINT NOT NULL` |  |
| `nivel_id` | `SMALLINT NOT NULL` |  |
| `area_curricular_id` | `SMALLINT NOT NULL` |  |

#### `bloques_horarios`

| Atributo | Tipo y reglas DDL | Observación para revisión |
|---|---|---|
| `id` | `SMALLSERIAL PRIMARY KEY` |  |
| `numero_bloque` | `SMALLINT UNIQUE NOT NULL` |  |
| `hora_inicio` | `TIME NOT NULL` |  |
| `hora_fin` | `TIME NOT NULL` |  |
| `es_recreo` | `BOOLEAN DEFAULT FALSE NOT NULL` |  |

#### `horarios_seccion`

| Atributo | Tipo y reglas DDL | Observación para revisión |
|---|---|---|
| `id` | `BIGSERIAL PRIMARY KEY` |  |
| `anio_lectivo_id` | `SMALLINT NOT NULL REFERENCES anios_lectivos(id) ON DELETE RESTRICT` |  |
| `seccion_id` | `INT NOT NULL` |  |
| `docente_usuario_id` | `BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT` |  |
| `asignacion_docente_id` | `BIGINT NOT NULL` |  |
| `dia_semana` | `SMALLINT NOT NULL CHECK (dia_semana BETWEEN 1 AND 5)` |  |

### 5. Tesorería y comprobantes
#### `conceptos_cobro`

| Atributo | Tipo y reglas DDL | Observación para revisión |
|---|---|---|
| `id` | `SMALLSERIAL PRIMARY KEY` |  |
| `codigo` | `VARCHAR(30) UNIQUE NOT NULL` |  |
| `nombre` | `VARCHAR(100) NOT NULL` | Longitud exacta pendiente. |
| `tipo_concepto` | `VARCHAR(20) NOT NULL CHECK (tipo_concepto IN ('MATRICULA', 'PENSION', 'CERTIFICADO', 'OTRO'))` |  |
| `monto_sugerido` | `NUMERIC(10, 2) NOT NULL CHECK (monto_sugerido >= 0.00)` |  |

#### `obligaciones_pago`

| Atributo | Tipo y reglas DDL | Observación para revisión |
|---|---|---|
| `id` | `BIGSERIAL PRIMARY KEY` |  |
| `matricula_id` | `BIGINT NOT NULL REFERENCES matriculas(id) ON DELETE RESTRICT` |  |
| `concepto_id` | `SMALLINT NOT NULL` |  |
| `tipo_concepto` | `VARCHAR(20) NOT NULL` |  |
| `numero_cuota` | `SMALLINT NOT NULL CHECK (numero_cuota BETWEEN 0 AND 10)` |  |
| `fecha_vencimiento` | `DATE NOT NULL` |  |
| `monto_base` | `NUMERIC(10, 2) NOT NULL CHECK (monto_base >= 0.00)` |  |
| `monto_mora` | `NUMERIC(10, 2) DEFAULT 0.00 NOT NULL CHECK (monto_mora >= 0.00)` |  |
| `monto_descuento` | `NUMERIC(10, 2) DEFAULT 0.00 NOT NULL CHECK (monto_descuento >= 0.00)` |  |
| `total_pagado` | `NUMERIC(10, 2) DEFAULT 0.00 NOT NULL CHECK (total_pagado >= 0.00)` |  |
| `saldo_pendiente` | `NUMERIC(10, 2) GENERATED ALWAYS AS (monto_base + monto_mora - monto_descuento - total_pagado) STORED` |  |
| `estado` | `VARCHAR(25) DEFAULT 'PENDIENTE' NOT NULL CHECK (estado IN ('PENDIENTE', 'PAGADO_TOTAL', 'PAGADO_PARCIAL', 'VENCIDO', 'CANCELADO_POR_TRASLADO'))` |  |

#### `pagos_transacciones`

| Atributo | Tipo y reglas DDL | Observación para revisión |
|---|---|---|
| `id` | `BIGSERIAL PRIMARY KEY` |  |
| `uuid` | `UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL` |  |
| `obligacion_pago_id` | `BIGINT NOT NULL REFERENCES obligaciones_pago(id) ON DELETE RESTRICT` |  |
| `pasarela_proveedor` | `VARCHAR(30) NOT NULL CHECK (pasarela_proveedor IN ('CULQI', 'NIUBIZ', 'MERCADO_PAGO', 'CAJA_EFECTIVO', 'TRANSFERENCIA'))` |  |
| `pasarela_transaccion_id` | `VARCHAR(100) UNIQUE NOT NULL` |  |
| `monto_pagado` | `NUMERIC(10, 2) NOT NULL CHECK (monto_pagado > 0.00)` |  |
| `fecha_pago` | `TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL` | Revisar coherencia con Java; preferir OffsetDateTime. |
| `estado_pago` | `VARCHAR(20) DEFAULT 'APROBADO' NOT NULL CHECK (estado_pago IN ('APROBADO', 'RECHAZADO', 'REVERTIDO'))` |  |
| `payload_webhook` | `JSONB` |  |

#### `series_comprobante`

| Atributo | Tipo y reglas DDL | Observación para revisión |
|---|---|---|
| `serie` | `VARCHAR(4) PRIMARY KEY CHECK (serie ~ '^[BE][0-9]{3}$')` |  |
| `ultimo_correlativo` | `INT NOT NULL DEFAULT 0 CHECK (ultimo_correlativo >= 0)` |  |
| `updated_at` | `TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL` | Revisar coherencia con Java; preferir OffsetDateTime. |

#### `comprobantes_pago`

| Atributo | Tipo y reglas DDL | Observación para revisión |
|---|---|---|
| `id` | `BIGSERIAL PRIMARY KEY` |  |
| `uuid` | `UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL` |  |
| `pago_transaccion_id` | `BIGINT UNIQUE NOT NULL REFERENCES pagos_transacciones(id) ON DELETE RESTRICT` |  |
| `tipo_comprobante` | `VARCHAR(20) DEFAULT 'RECIBO_INTERNO' NOT NULL CHECK (tipo_comprobante IN ('BOLETA', 'RECIBO_INTERNO'))` |  |
| `serie` | `VARCHAR(4) NOT NULL CHECK (serie ~ '^[BE][0-9]{3}$')` |  |
| `correlativo` | `INT NOT NULL CHECK (correlativo > 0)` |  |
| `fecha_emision` | `TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL` | Revisar coherencia con Java; preferir OffsetDateTime. |
| `monto_total` | `NUMERIC(10, 2) NOT NULL CHECK (monto_total > 0.00)` |  |
| `estado_comprobante` | `VARCHAR(20) DEFAULT 'EMITIDO' NOT NULL CHECK (estado_comprobante IN ('EMITIDO', 'ANULADO'))` |  |
| `fecha_anulacion` | `TIMESTAMPTZ` | Revisar coherencia con Java; preferir OffsetDateTime. |
| `motivo_anulacion` | `VARCHAR(255)` |  |
| `url_pdf_comprobante` | `VARCHAR(255)` |  |

### 6. Asistencia e incidencias
#### `lotes_biometrico`

| Atributo | Tipo y reglas DDL | Observación para revisión |
|---|---|---|
| `id` | `BIGSERIAL PRIMARY KEY` |  |
| `nombre_archivo` | `VARCHAR(150) NOT NULL` |  |
| `total_filas` | `INT NOT NULL CHECK (total_filas >= 0)` |  |
| `marcas_validas` | `INT NOT NULL CHECK (marcas_validas >= 0)` |  |
| `marcas_erroneas` | `INT NOT NULL CHECK (marcas_erroneas >= 0)` |  |
| `importado_por_usuario_id` | `BIGINT REFERENCES usuarios(id) ON DELETE SET NULL` |  |
| `created_at` | `TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL` | Revisar coherencia con Java; preferir OffsetDateTime. |

#### `marcas_biometrico_porteria`

| Atributo | Tipo y reglas DDL | Observación para revisión |
|---|---|---|
| `id` | `BIGSERIAL PRIMARY KEY` |  |
| `lote_id` | `BIGINT REFERENCES lotes_biometrico(id) ON DELETE SET NULL` |  |
| `dni_leido` | `VARCHAR(15) NOT NULL` |  |
| `fecha_hora` | `TIMESTAMPTZ NOT NULL` | Revisar coherencia con Java; preferir OffsetDateTime. |
| `dispositivo_codigo` | `VARCHAR(30) DEFAULT 'PORTERIA_01'` |  |
| `estudiante_id` | `BIGINT REFERENCES estudiantes(id) ON DELETE SET NULL` |  |
| `estado_procesamiento` | `VARCHAR(25) DEFAULT 'PENDIENTE' NOT NULL CHECK (estado_procesamiento IN ('PENDIENTE', 'CONCILIADO', 'DNI_NO_IDENTIFICADO', 'DUPLICADO'))` |  |
| `created_at` | `TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL` | Revisar coherencia con Java; preferir OffsetDateTime. |

#### `asistencias_aula`

| Atributo | Tipo y reglas DDL | Observación para revisión |
|---|---|---|
| `id` | `BIGSERIAL PRIMARY KEY` |  |
| `matricula_id` | `BIGINT NOT NULL REFERENCES matriculas(id) ON DELETE RESTRICT` |  |
| `fecha_sesion` | `DATE NOT NULL` |  |
| `hora_registro` | `TIME NOT NULL` |  |
| `estado` | `VARCHAR(25) NOT NULL CHECK (estado IN ('PRESENTE', 'TARDANZA', 'FALTA_INJUSTIFICADA', 'FALTA_JUSTIFICADA'))` |  |
| `auxiliar_usuario_id` | `BIGINT REFERENCES usuarios(id) ON DELETE RESTRICT` |  |
| `justificada` | `BOOLEAN DEFAULT FALSE NOT NULL` |  |
| `motivo_justificacion` | `TEXT` |  |
| `documento_sustento_url` | `VARCHAR(255)` |  |

#### `conciliaciones_asistencia`

| Atributo | Tipo y reglas DDL | Observación para revisión |
|---|---|---|
| `id` | `BIGSERIAL PRIMARY KEY` |  |
| `fecha` | `DATE NOT NULL` |  |
| `estudiante_id` | `BIGINT NOT NULL REFERENCES estudiantes(id) ON DELETE RESTRICT` |  |
| `marco_porteria` | `BOOLEAN NOT NULL` |  |
| `presente_aula` | `BOOLEAN NOT NULL` |  |
| `tipo_discrepancia` | `VARCHAR(35) NOT NULL CHECK (tipo_discrepancia IN ('ASISTENCIA_CONCILIADA', 'DISCREPANCIA_FUGA', 'DISCREPANCIA_OMISION_PORTERIA', 'PENDIENTE_CIERRE_TURNO'))` |  |
| `alerta_notificada` | `BOOLEAN DEFAULT FALSE NOT NULL` |  |

#### `incidencias_conductuales`

| Atributo | Tipo y reglas DDL | Observación para revisión |
|---|---|---|
| `id` | `BIGSERIAL PRIMARY KEY` |  |
| `matricula_id` | `BIGINT NOT NULL REFERENCES matriculas(id) ON DELETE RESTRICT` |  |
| `fecha_incidencia` | `DATE NOT NULL` |  |
| `tipo_falta` | `VARCHAR(20) NOT NULL CHECK (tipo_falta IN ('LEVE', 'GRAVE', 'MUY_GRAVE'))` |  |
| `descripcion` | `TEXT NOT NULL` |  |
| `reportado_por_usuario_id` | `BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT` |  |
| `requiere_citacion` | `BOOLEAN DEFAULT FALSE NOT NULL` |  |
| `estado` | `VARCHAR(20) DEFAULT 'ABIERTA' NOT NULL CHECK (estado IN ('ABIERTA', 'ATENDIDA', 'CERRADA'))` |  |
| `created_at` | `TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL` | Revisar coherencia con Java; preferir OffsetDateTime. |

### 7. Evaluación y refuerzo
#### `calificaciones_cneb`

| Atributo | Tipo y reglas DDL | Observación para revisión |
|---|---|---|
| `id` | `BIGSERIAL PRIMARY KEY` |  |
| `matricula_id` | `BIGINT NOT NULL` |  |
| `anio_lectivo_id` | `SMALLINT NOT NULL` |  |
| `seccion_id` | `INT NOT NULL` |  |
| `periodo_academico_id` | `SMALLINT NOT NULL` |  |
| `asignacion_docente_id` | `BIGINT NOT NULL` |  |
| `area_curricular_id` | `SMALLINT NOT NULL` |  |
| `docente_usuario_id` | `BIGINT NOT NULL` |  |
| `competencia_id` | `SMALLINT NOT NULL` |  |
| `calificacion_cualitativa` | `VARCHAR(2) NOT NULL CHECK (calificacion_cualitativa IN ('AD', 'A', 'B', 'C'))` |  |
| `conclusion_descriptiva` | `TEXT` |  |
| `sugerencia_ia_utilizada` | `BOOLEAN DEFAULT FALSE NOT NULL` |  |
| `requiere_refuerzo` | `BOOLEAN GENERATED ALWAYS AS (calificacion_cualitativa = 'C') STORED` |  |
| `created_at` | `TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL` | Revisar coherencia con Java; preferir OffsetDateTime. |
| `updated_at` | `TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL` | Revisar coherencia con Java; preferir OffsetDateTime. |

#### `sesiones_refuerzo`

| Atributo | Tipo y reglas DDL | Observación para revisión |
|---|---|---|
| `id` | `BIGSERIAL PRIMARY KEY` |  |
| `anio_lectivo_id` | `SMALLINT NOT NULL` |  |
| `periodo_academico_id` | `SMALLINT NOT NULL` |  |
| `area_curricular_id` | `SMALLINT NOT NULL REFERENCES areas_curriculares(id) ON DELETE RESTRICT` |  |
| `docente_usuario_id` | `BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT` |  |
| `tema` | `VARCHAR(40) NOT NULL` | V1–V11: `VARCHAR(150)`; V12 aplica 40. Debe contener el tema breve. |
| `fecha_programada` | `DATE NOT NULL` |  |
| `hora_inicio` | `TIME NOT NULL` |  |
| `hora_fin` | `TIME NOT NULL` |  |
| `aula_asignada` | `VARCHAR(30) REFERENCES aulas(codigo) ON DELETE RESTRICT ON UPDATE RESTRICT` | V12 preserva el código existente y añade FK e índice; puede ser NULL. |
| `aula_id` (propuesto) | `INT NULL REFERENCES aulas(id) ON DELETE RESTRICT` | Nueva FK a `aulas.id`; nullable mientras se permita programar una sesión sin aula asignada. |

#### `inscripciones_refuerzo`

| Atributo | Tipo y reglas DDL | Observación para revisión |
|---|---|---|
| `id` | `BIGSERIAL PRIMARY KEY` |  |
| `sesion_refuerzo_id` | `BIGINT NOT NULL REFERENCES sesiones_refuerzo(id) ON DELETE RESTRICT` |  |
| `estudiante_id` | `BIGINT NOT NULL REFERENCES estudiantes(id) ON DELETE RESTRICT` |  |
| `calificacion_origen_id` | `BIGINT REFERENCES calificaciones_cneb(id) ON DELETE SET NULL` |  |
| `estado_asistencia` | `VARCHAR(20) DEFAULT 'PENDIENTE' NOT NULL CHECK (estado_asistencia IN ('PENDIENTE', 'ASISTIO', 'FALTO', 'JUSTIFICADO'))` |  |
| `observaciones` | `TEXT` |  |

### 8. Comunicaciones
#### `comunicados_oficiales`

| Atributo | Tipo y reglas DDL | Observación para revisión |
|---|---|---|
| `id` | `BIGSERIAL PRIMARY KEY` |  |
| `titulo` | `VARCHAR(40) NOT NULL` | V1–V11: `VARCHAR(150)`; V12 aplica 40. El cuerpo queda en `contenido TEXT`. |
| `contenido` | `TEXT NOT NULL` |  |
| `remitente_usuario_id` | `BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT` |  |
| `fecha_publicacion` | `TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL` | Revisar coherencia con Java; preferir OffsetDateTime. |
| `requiere_acuse` | `BOOLEAN DEFAULT FALSE NOT NULL` |  |

#### `comunicado_destinatarios`

| Atributo | Tipo y reglas DDL | Observación para revisión |
|---|---|---|
| `id` | `BIGSERIAL PRIMARY KEY` |  |
| `comunicado_id` | `BIGINT NOT NULL REFERENCES comunicados_oficiales(id) ON DELETE RESTRICT` |  |
| `apoderado_id` | `BIGINT NOT NULL REFERENCES apoderados(id) ON DELETE RESTRICT` |  |
| `leido` | `BOOLEAN DEFAULT FALSE NOT NULL` |  |
| `fecha_lectura` | `TIMESTAMPTZ` | Revisar coherencia con Java; preferir OffsetDateTime. |
| `acuse_confirmado` | `BOOLEAN DEFAULT FALSE NOT NULL` |  |
| `fecha_acuse` | `TIMESTAMPTZ` | Revisar coherencia con Java; preferir OffsetDateTime. |

## Cambios aplicados en la migración V12

| Tabla | Cambio |
|---|---|
| `roles` | `nombre VARCHAR(30)` |
| `competencias` | `nombre VARCHAR(50)` |
| `comunicados_oficiales` | `titulo VARCHAR(40)` |
| `sesiones_refuerzo` | `tema VARCHAR(40)`; `aula_asignada` ahora es FK a `aulas(codigo)` con borrado/actualización restringidos e índice de apoyo. |
| `apoderados`, `estudiantes` | `apellido_paterno` y `apellido_materno` separados, cada uno `VARCHAR(20)`. Email apoderado `VARCHAR(254)`. |
| `solicitudes_matricula_publica` | Nombres y apellidos de solicitud `VARCHAR(20)`; email `VARCHAR(254)`. |
| `usuarios` | Email `VARCHAR(254)`. |
| `niveles`, `grados`, `aulas` | Nombres `VARCHAR(30)`, `VARCHAR(30)` y `VARCHAR(40)`, respectivamente. |
| `periodos_academicos` | Añade `activo BOOLEAN NOT NULL DEFAULT TRUE`, distinto de `cerrado`. |
| Datos previos | V12 valida límites y referencias de aula antes de alterar. Si encuentra excedentes o códigos sin aula, aborta sin truncar ni borrar filas. |


**Instalación consolidada:** `src/main/resources/db/schema_completo_colegio_shuji.sql` reúne el esquema V1–V12 para una BD vacía. Omite el usuario/contraseña fija de práctica de V7; se debe crear el administrador por el flujo seguro del proyecto. El archivo de importador se conserva idéntico por compatibilidad.
## Observaciones transversales

1. V8–V11 forman parte del modelo actual; V1–V5 no es consolidado completo.
2. `solicitudes_matricula_documentos.sha256` sigue siendo `CHAR(64)`; queda como observación ajena a los cambios acordados.
3. Unificar TIMESTAMPTZ con OffsetDateTime donde corresponda.
4. V12 cambia entidades JPA, validaciones y servicio para alinear los límites y el estado activo.
5. No hay truncamiento ni borrado de datos; V12 falla antes si la base real contiene valores incompatibles.


**Revisión actualizada (2026-09-24):** V12, entidades JPA, límites de DTO, control de período activo y FK de aula están alineados. El consolidado es para una BD vacía; en una instalación existente se aplica Flyway V12. `.\mvnw.cmd -DskipTests clean compile` terminó con BUILD SUCCESS; no ejecuté pruebas ni apliqué la migración a una BD real. `.env` no se tocó.
