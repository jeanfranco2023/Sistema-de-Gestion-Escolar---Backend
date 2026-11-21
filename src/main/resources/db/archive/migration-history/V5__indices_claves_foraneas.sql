-- =============================================================================
-- MIGRACIÓN FLYWAY V5: ÍNDICES DE SOPORTE PARA CLAVES FORÁNEAS
-- =============================================================================
-- PostgreSQL no crea automáticamente índices sobre las columnas que referencian
-- una FK. Estos índices aceleran joins y la comprobación de RESTRICT/SET NULL al
-- modificar filas padre. Se conservan las migraciones históricas V1-V4 intactas.

CREATE INDEX IF NOT EXISTS idx_usuario_roles_rol_id
    ON usuario_roles (rol_id);

CREATE INDEX IF NOT EXISTS idx_auditoria_cambios_usuario_id
    ON auditoria_cambios (usuario_id);

CREATE INDEX IF NOT EXISTS idx_secciones_grado_nivel
    ON secciones (grado_id, nivel_id);

CREATE INDEX IF NOT EXISTS idx_secciones_aula_id
    ON secciones (aula_id);

CREATE INDEX IF NOT EXISTS idx_asignaciones_area_nivel
    ON asignaciones_docentes (area_curricular_id, nivel_id);

CREATE INDEX IF NOT EXISTS idx_horarios_anio_lectivo_id
    ON horarios_seccion (anio_lectivo_id);

CREATE INDEX IF NOT EXISTS idx_lotes_biometrico_importado_por
    ON lotes_biometrico (importado_por_usuario_id);

CREATE INDEX IF NOT EXISTS idx_marcas_porteria_estudiante_id
    ON marcas_biometrico_porteria (estudiante_id);

CREATE INDEX IF NOT EXISTS idx_asistencias_aula_auxiliar_id
    ON asistencias_aula (auxiliar_usuario_id);

CREATE INDEX IF NOT EXISTS idx_conciliaciones_estudiante_id
    ON conciliaciones_asistencia (estudiante_id);

CREATE INDEX IF NOT EXISTS idx_calificaciones_asignacion_periodo
    ON calificaciones_cneb (asignacion_docente_id, periodo_academico_id);

CREATE INDEX IF NOT EXISTS idx_sesiones_refuerzo_docente_id
    ON sesiones_refuerzo (docente_usuario_id);

CREATE INDEX IF NOT EXISTS idx_inscripciones_calificacion_origen_id
    ON inscripciones_refuerzo (calificacion_origen_id);
