-- A student may have only one economic guardian, including under concurrent inserts.
CREATE UNIQUE INDEX ux_estudiante_apoderados_un_responsable_economico
    ON estudiante_apoderados (estudiante_id)
    WHERE es_responsable_economico IS TRUE;
