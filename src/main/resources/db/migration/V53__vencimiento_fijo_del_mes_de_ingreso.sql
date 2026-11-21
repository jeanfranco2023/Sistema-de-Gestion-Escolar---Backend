CREATE OR REPLACE FUNCTION public.generar_pensiones_matricula(p_matricula_id bigint)
RETURNS void LANGUAGE sql SET search_path = '' AS $$
  INSERT INTO public.obligaciones_pago
    (matricula_id, concepto_id, tipo_concepto, numero_cuota, descripcion, fecha_vencimiento, monto_base)
  SELECT m.id, c.id, 'PENSION', (mes.numero - 2)::smallint,
    U&'Pensi\00F3n ' || (ARRAY['marzo','abril','mayo','junio','julio','agosto','septiembre',
                        'octubre','noviembre','diciembre'])[mes.numero - 2] || ' ' || a.anio,
    make_date(a.anio, mes.numero, c.dia_vencimiento),
    c.monto_sugerido
  FROM public.matriculas m
  JOIN public.anios_lectivos a ON a.id = m.anio_lectivo_id
  JOIN public.conceptos_cobro c ON c.codigo_concepto = 'PENSION_MENSUAL' AND c.tipo_concepto = 'PENSION'
  CROSS JOIN generate_series(3, 12) AS mes(numero)
  WHERE m.id = p_matricula_id AND a.abierto
    AND m.estado_matricula = 'MATRICULADO' AND m.seccion_id IS NOT NULL
    AND make_date(a.anio, mes.numero, 1) >= date_trunc('month', m.fecha_matricula AT TIME ZONE 'America/Lima')::date
    AND EXISTS (
      SELECT 1 FROM public.obligaciones_pago pago
      WHERE pago.matricula_id = m.id AND pago.tipo_concepto = 'MATRICULA'
        AND pago.estado = 'PAGADO_TOTAL' AND pago.saldo_pendiente = 0
    )
    AND NOT EXISTS (
      SELECT 1 FROM public.obligaciones_pago existente
      WHERE existente.matricula_id = m.id AND existente.tipo_concepto = 'PENSION'
        AND (existente.numero_cuota = mes.numero - 2
          OR date_trunc('month', existente.fecha_vencimiento)::date = make_date(a.anio, mes.numero, 1))
    )
  ON CONFLICT (matricula_id, concepto_id, numero_cuota) DO NOTHING;
$$;
