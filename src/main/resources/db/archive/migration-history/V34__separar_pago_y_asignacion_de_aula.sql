-- El pago completa el trámite; la vacante se consume cuando Secretaría asigna sección.
ALTER TABLE matriculas
    ALTER COLUMN seccion_id DROP NOT NULL,
    DROP CONSTRAINT matriculas_estado_matricula_check,
    ADD CONSTRAINT matriculas_estado_matricula_check
        CHECK (estado_matricula IN (
            'SOLICITADA',
            'RESERVADA_TEMPORAL',
            'PENDIENTE_ASIGNACION',
            'MATRICULADO',
            'TRASLADADO',
            'RETIRADO',
            'CANCELADA'
        ));

ALTER TABLE solicitudes_matricula_publica
    ADD COLUMN grado_id SMALLINT,
    ALTER COLUMN estado TYPE VARCHAR(50);

ALTER TABLE solicitudes_matricula_publica
    DROP CONSTRAINT solicitudes_matricula_publica_estado_check,
    ADD CONSTRAINT solicitudes_matricula_publica_estado_check
        CHECK (estado IN (
            'DOCUMENTOS_PENDIENTES',
            'DOCUMENTOS_EN_VALIDACION',
            'DOCUMENTOS_OBSERVADOS',
            'DOCUMENTOS_VALIDADOS',
            'AUTORIZACION_EDAD_PENDIENTE',
            'PAGO_PENDIENTE',
            'PAGO_CONFIRMADO_PENDIENTE_ASIGNACION',
            'MATRICULADA',
            'RECHAZADA'
        ));

UPDATE solicitudes_matricula_publica solicitud
SET grado_id = seccion.grado_id
FROM secciones seccion
WHERE solicitud.seccion_id = seccion.id
  AND solicitud.grado_id IS NULL;

ALTER TABLE solicitudes_matricula_publica
    ALTER COLUMN grado_id SET NOT NULL,
    ALTER COLUMN seccion_id DROP NOT NULL;

ALTER TABLE solicitudes_matricula_publica
    ADD CONSTRAINT fk_solicitud_matricula_grado
        FOREIGN KEY (grado_id) REFERENCES grados (id);

ALTER TABLE matriculas
    ADD CONSTRAINT ck_matricula_pendiente_sin_seccion
        CHECK (estado_matricula <> 'PENDIENTE_ASIGNACION' OR seccion_id IS NULL),
    ADD CONSTRAINT ck_matriculado_con_seccion
        CHECK (estado_matricula <> 'MATRICULADO' OR seccion_id IS NOT NULL);
