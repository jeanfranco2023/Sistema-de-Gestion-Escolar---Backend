-- =============================================================================
-- V57__sincronizar_cobros_matricula_precargados.sql
-- Garantiza que toda matrícula confirmada (MATRICULADO) tenga su obligación de
-- derecho de matrícula (S/ 350.00), pago aprobado en pagos_transacciones y
-- comprobante B001 emitido en comprobantes_pago.
-- =============================================================================

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
    'Derecho de matrícula anual ' || a.anio,
    make_date(a.anio, 3, COALESCE(c.dia_vencimiento, 10)),
    COALESCE(c.monto_sugerido, 350.00),
    0.00,
    0.00,
    0.00,
    'PENDIENTE'
FROM public.matriculas m
JOIN public.anios_lectivos a ON a.id = m.anio_lectivo_id
JOIN public.conceptos_cobro c ON c.codigo_concepto = 'MATRICULA_ANUAL' AND c.tipo_concepto = 'MATRICULA'
WHERE m.estado_matricula = 'MATRICULADO'
  AND NOT EXISTS (
      SELECT 1
      FROM public.obligaciones_pago op
      WHERE op.matricula_id = m.id
        AND op.tipo_concepto = 'MATRICULA'
        AND op.numero_cuota = 0
  )
ON CONFLICT (matricula_id, concepto_id, numero_cuota) DO NOTHING;

INSERT INTO public.pagos_transacciones (
    obligacion_pago_id,
    pasarela_proveedor,
    pasarela_transaccion_id,
    metodo_pago,
    monto_pagado,
    fecha_pago,
    estado_pago,
    payload_webhook
)
SELECT
    op.id,
    'CAJA_EFECTIVO',
    'SYNC-MAT-PAGADA-' || op.id,
    'EFECTIVO',
    (op.monto_base + op.monto_mora - op.monto_descuento - op.total_pagado),
    CURRENT_TIMESTAMP,
    'APROBADO',
    '{"origen":"SINCRONIZACION_MATRICULA_PRECARGADA","estado":"APROBADO"}'::jsonb
FROM public.obligaciones_pago op
JOIN public.matriculas m ON m.id = op.matricula_id
WHERE m.estado_matricula = 'MATRICULADO'
  AND op.tipo_concepto = 'MATRICULA'
  AND op.numero_cuota = 0
  AND op.estado <> 'PAGADO_TOTAL'
  AND (op.monto_base + op.monto_mora - op.monto_descuento - op.total_pagado) > 0
  AND NOT EXISTS (
      SELECT 1
      FROM public.pagos_transacciones pt
      WHERE pt.obligacion_pago_id = op.id
        AND pt.estado_pago = 'APROBADO'
  )
ON CONFLICT (pasarela_transaccion_id) DO NOTHING;

DO $$
DECLARE
    v_ultimo INTEGER;
    v_nuevo_ultimo INTEGER;
BEGIN
    SELECT ultimo_correlativo INTO v_ultimo
    FROM public.series_comprobante
    WHERE serie = 'B001'
    FOR UPDATE;

    IF v_ultimo IS NOT NULL THEN
        WITH faltantes AS (
            SELECT
                pt.id AS pago_id,
                pt.fecha_pago,
                pt.monto_pagado,
                v_ultimo + ROW_NUMBER() OVER (ORDER BY pt.id) AS nuevo_correlativo
            FROM public.pagos_transacciones pt
            WHERE pt.estado_pago = 'APROBADO'
              AND pt.pasarela_transaccion_id LIKE 'SYNC-MAT-PAGADA-%'
              AND NOT EXISTS (
                  SELECT 1 FROM public.comprobantes_pago cp WHERE cp.pago_transaccion_id = pt.id
              )
        ),
        insertados AS (
            INSERT INTO public.comprobantes_pago (
                pago_transaccion_id,
                tipo_comprobante,
                serie,
                correlativo,
                fecha_emision,
                monto_total,
                estado_comprobante
            )
            SELECT
                f.pago_id,
                'BOLETA',
                'B001',
                f.nuevo_correlativo,
                f.fecha_pago,
                f.monto_pagado,
                'EMITIDO'
            FROM faltantes f
            RETURNING correlativo
        )
        SELECT MAX(correlativo) INTO v_nuevo_ultimo FROM insertados;

        IF v_nuevo_ultimo IS NOT NULL THEN
            UPDATE public.series_comprobante
            SET ultimo_correlativo = v_nuevo_ultimo,
                fecha_actualizacion = CURRENT_TIMESTAMP
            WHERE serie = 'B001';
        END IF;
    END IF;
END $$;
