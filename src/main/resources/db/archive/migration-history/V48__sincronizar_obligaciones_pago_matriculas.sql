-- =============================================================================
-- V48__sincronizar_obligaciones_pago_matriculas.sql
-- 1. Asegura que toda matrícula existente tenga su obligación de pago de matrícula (cuota 0)
-- 2. Para matrículas ya confirmadas (MATRICULADO), registra el pago aprobado para que figuren como PAGADO_TOTAL
-- 3. Para matrículas en solicitud o reserva (SOLICITADA, RESERVADA_TEMPORAL), su obligación queda en PENDIENTE
-- =============================================================================

-- 1. Generar la obligación de matrícula (cuota 0) para todas las matrículas que no la tengan
INSERT INTO public.obligaciones_pago (
    matricula_id,
    concepto_id,
    tipo_concepto,
    numero_cuota,
    descripcion,
    fecha_vencimiento,
    monto_base,
    monto_mora,
    monto_descuento,
    total_pagado,
    estado
)
SELECT
    m.id,
    c.id,
    c.tipo_concepto,
    0::SMALLINT,
    'Derecho de Matrícula y Admisión 2026',
    DATE '2026-03-31',
    COALESCE(c.monto_sugerido, 350.00),
    0.00,
    0.00,
    0.00,
    'PENDIENTE'
FROM public.matriculas m
JOIN public.conceptos_cobro c ON c.tipo_concepto = 'MATRICULA'
WHERE NOT EXISTS (
    SELECT 1
    FROM public.obligaciones_pago op
    WHERE op.matricula_id = m.id
      AND op.tipo_concepto = 'MATRICULA'
      AND op.numero_cuota = 0
)
ON CONFLICT (matricula_id, concepto_id, numero_cuota) DO NOTHING;

-- 2. Para todas las matrículas con estado MATRICULADO cuya obligación de matrícula siga pendiente,
-- registrar la transacción aprobada en caja para que el trigger oficial las deje en PAGADO_TOTAL.
INSERT INTO public.pagos_transacciones (
    obligacion_pago_id,
    pasarela_transaccion_id,
    pasarela_proveedor,
    metodo_pago,
    monto_pagado,
    estado_pago,
    payload_webhook
)
SELECT
    op.id,
    'SYNC-MAT-PAGADA-' || op.id,
    'CAJA_EFECTIVO',
    'EFECTIVO',
    (op.monto_base + op.monto_mora - op.monto_descuento - op.totalPagado_calc),
    'APROBADO',
    '{"origen":"SINCRONIZACION_INSTITUCIONAL_V48","estado":"APROBADO"}'::jsonb
FROM (
    SELECT
        o.id,
        o.matricula_id,
        o.monto_base,
        o.monto_mora,
        o.monto_descuento,
        o.total_pagado AS totalPagado_calc,
        o.estado
    FROM public.obligaciones_pago o
    WHERE o.tipo_concepto = 'MATRICULA'
      AND o.numero_cuota = 0
      AND o.estado <> 'PAGADO_TOTAL'
) op
JOIN public.matriculas m ON m.id = op.matricula_id
WHERE m.estado_matricula = 'MATRICULADO'
  AND (op.monto_base + op.monto_mora - op.monto_descuento - op.totalPagado_calc) > 0
ON CONFLICT (pasarela_transaccion_id) DO NOTHING;
