-- Retiene la regla de edad ordinaria y permite una excepción con decisión institucional auditable.
ALTER TABLE public.solicitudes_matricula_publica
    ADD COLUMN requiere_autorizacion_edad BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN motivo_autorizacion_edad VARCHAR(500),
    ADD COLUMN autorizado_por_usuario_id BIGINT,
    ADD COLUMN fecha_autorizacion_edad TIMESTAMPTZ;

ALTER TABLE public.solicitudes_matricula_publica
    DROP CONSTRAINT IF EXISTS solicitudes_matricula_publica_estado_check,
    ADD CONSTRAINT solicitudes_matricula_publica_estado_check
        CHECK (estado IN (
            'DOCUMENTOS_PENDIENTES', 'DOCUMENTOS_EN_VALIDACION', 'DOCUMENTOS_OBSERVADOS',
            'DOCUMENTOS_VALIDADOS', 'AUTORIZACION_EDAD_PENDIENTE', 'PAGO_PENDIENTE',
            'MATRICULADA', 'RECHAZADA')),
    ADD CONSTRAINT solicitudes_matricula_publica_autorizacion_edad_check
        CHECK (
            (NOT requiere_autorizacion_edad AND motivo_autorizacion_edad IS NULL
                AND autorizado_por_usuario_id IS NULL AND fecha_autorizacion_edad IS NULL)
            OR (requiere_autorizacion_edad AND (
                (motivo_autorizacion_edad IS NULL AND autorizado_por_usuario_id IS NULL
                    AND fecha_autorizacion_edad IS NULL)
                OR (length(btrim(motivo_autorizacion_edad)) BETWEEN 10 AND 500
                    AND autorizado_por_usuario_id IS NOT NULL AND fecha_autorizacion_edad IS NOT NULL))));

ALTER TABLE public.solicitudes_matricula_publica
    ADD CONSTRAINT solicitudes_matricula_publica_autorizado_por_usuario_id_fkey
        FOREIGN KEY (autorizado_por_usuario_id) REFERENCES public.usuarios(id) ON DELETE RESTRICT;

CREATE INDEX idx_solicitudes_matricula_autorizacion_edad
    ON public.solicitudes_matricula_publica (fecha_creacion DESC)
    WHERE estado = 'AUTORIZACION_EDAD_PENDIENTE';
