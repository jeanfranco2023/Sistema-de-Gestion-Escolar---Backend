-- V54 se conserva para respetar la suma de comprobación de Flyway.
-- Antes de habilitar un entorno que haya ejecutado V54, verificar quién recibió el rol
-- ADMINISTRADOR, aprovisionar una credencial independiente y revocar sesiones previas.
-- Esta migración conserva las cuentas, roles e historial que ya existan.

CREATE SEQUENCE public.solicitudes_cambio_folio_seq AS BIGINT START WITH 26;

-- Reservar CH-001 a CH-025 y continuar después del mayor folio registrado.
SELECT setval(
    'public.solicitudes_cambio_folio_seq',
    GREATEST(25, COALESCE(MAX(substring(codigo FROM 4)::bigint), 0)),
    true
)
FROM public.solicitudes_cambio
WHERE codigo ~ '^CH-[0-9]+$';

ALTER TABLE public.solicitudes_cambio
    DROP CONSTRAINT IF EXISTS solicitudes_cambio_idempotency_key_key,
    ADD CONSTRAINT uq_solicitudes_cambio_solicitante_idempotencia
        UNIQUE (solicitante_id, idempotency_key),
    ADD CONSTRAINT fk_solicitudes_cambio_solicitante
        FOREIGN KEY (solicitante_id) REFERENCES public.usuarios(id) NOT VALID,
    ADD CONSTRAINT fk_solicitudes_cambio_responsable
        FOREIGN KEY (responsable_id) REFERENCES public.usuarios(id) NOT VALID;

-- NOT VALID protege las nuevas escrituras sin corregir por suposición el historial.
ALTER TABLE public.solicitudes_cambio_eventos
    ADD CONSTRAINT fk_solicitudes_cambio_eventos_actor
        FOREIGN KEY (actor_id) REFERENCES public.usuarios(id) NOT VALID,
    ADD CONSTRAINT uq_solicitudes_cambio_eventos_solicitud_id UNIQUE (solicitud_id, id);

ALTER TABLE public.solicitudes_cambio_notificaciones
    ADD CONSTRAINT fk_solicitudes_cambio_notif_evento
        FOREIGN KEY (solicitud_id, evento_id)
        REFERENCES public.solicitudes_cambio_eventos(solicitud_id, id) NOT VALID,
    ADD CONSTRAINT chk_solicitudes_cambio_notif_estado
        CHECK (estado_envio IN ('PENDIENTE', 'ENVIADO', 'FALLIDO')) NOT VALID,
    ADD CONSTRAINT chk_solicitudes_cambio_notif_intentos CHECK (intentos >= 0) NOT VALID;
