-- Enforce the requested section's grade/year and the linked student's enrollment identity.
-- Section on the current matrícula is intentionally not linked here: later section changes
-- must not be blocked by the originating public request.

ALTER TABLE secciones
    ADD CONSTRAINT uq_secciones_id_anio_grado
    UNIQUE (id, anio_lectivo_id, grado_id);

ALTER TABLE solicitudes_matricula_publica
    DROP CONSTRAINT solicitudes_matricula_publica_seccion_id_anio_lectivo_id_fkey;

ALTER TABLE solicitudes_matricula_publica
    ADD CONSTRAINT fk_solicitud_seccion_anio_grado
    FOREIGN KEY (seccion_id, anio_lectivo_id, grado_id)
    REFERENCES secciones (id, anio_lectivo_id, grado_id)
    ON DELETE RESTRICT;

ALTER TABLE matriculas
    ADD CONSTRAINT uq_matriculas_id_estudiante_anio
    UNIQUE (id, estudiante_id, anio_lectivo_id);

ALTER TABLE solicitudes_matricula_publica
    ADD CONSTRAINT ck_solicitud_matricula_requiere_estudiante
    CHECK (matricula_id IS NULL OR estudiante_id IS NOT NULL);

ALTER TABLE solicitudes_matricula_publica
    ADD CONSTRAINT fk_solicitud_matricula_estudiante_anio
    FOREIGN KEY (matricula_id, estudiante_id, anio_lectivo_id)
    REFERENCES matriculas (id, estudiante_id, anio_lectivo_id)
    ON DELETE RESTRICT;

