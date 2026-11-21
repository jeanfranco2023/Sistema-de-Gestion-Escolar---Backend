ALTER TABLE public.conceptos_cobro
  ADD COLUMN IF NOT EXISTS activo boolean NOT NULL DEFAULT true,
  ADD COLUMN IF NOT EXISTS dia_vencimiento smallint NOT NULL DEFAULT 10
    CHECK (dia_vencimiento BETWEEN 1 AND 28);

INSERT INTO public.conceptos_cobro (codigo_concepto, nombre_concepto, tipo_concepto, monto_sugerido)
VALUES ('PENSION_MENSUAL', U&'Pensi\00F3n mensual', 'PENSION', 120.00)
ON CONFLICT (codigo_concepto) DO NOTHING;

CREATE OR REPLACE FUNCTION public.generar_pensiones_matricula(p_matricula_id bigint)
RETURNS void LANGUAGE sql SET search_path = '' AS $$
  INSERT INTO public.obligaciones_pago
    (matricula_id, concepto_id, tipo_concepto, numero_cuota, descripcion, fecha_vencimiento, monto_base)
  SELECT m.id, c.id, 'PENSION', (mes.numero - 2)::smallint,
    U&'Pensi\00F3n ' || (ARRAY['marzo','abril','mayo','junio','julio','agosto','septiembre',
                        'octubre','noviembre','diciembre'])[mes.numero - 2] || ' ' || a.anio,
    greatest(make_date(a.anio, mes.numero, c.dia_vencimiento),
             (m.fecha_matricula AT TIME ZONE 'America/Lima')::date),
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

CREATE OR REPLACE FUNCTION public.pensiones_al_confirmar_matricula()
RETURNS trigger LANGUAGE plpgsql SET search_path = '' AS $$
BEGIN
  PERFORM public.generar_pensiones_matricula(NEW.id);
  RETURN NULL;
END;
$$;

CREATE OR REPLACE FUNCTION public.pensiones_al_pagar_matricula()
RETURNS trigger LANGUAGE plpgsql SET search_path = '' AS $$
BEGIN
  PERFORM public.generar_pensiones_matricula(NEW.matricula_id);
  RETURN NULL;
END;
$$;

REVOKE EXECUTE ON FUNCTION public.generar_pensiones_matricula(bigint) FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION public.pensiones_al_confirmar_matricula() FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION public.pensiones_al_pagar_matricula() FROM PUBLIC;

CREATE OR REPLACE TRIGGER trg_pensiones_matricula
  AFTER INSERT OR UPDATE OF estado_matricula, seccion_id ON public.matriculas
  FOR EACH ROW WHEN (NEW.estado_matricula = 'MATRICULADO' AND NEW.seccion_id IS NOT NULL)
  EXECUTE FUNCTION public.pensiones_al_confirmar_matricula();

CREATE OR REPLACE TRIGGER trg_pensiones_pago_matricula
  AFTER INSERT OR UPDATE OF estado ON public.obligaciones_pago
  FOR EACH ROW WHEN (NEW.tipo_concepto = 'MATRICULA' AND NEW.estado = 'PAGADO_TOTAL')
  EXECUTE FUNCTION public.pensiones_al_pagar_matricula();

SELECT public.generar_pensiones_matricula(m.id)
FROM public.matriculas m
JOIN public.anios_lectivos a ON a.id = m.anio_lectivo_id
WHERE a.abierto AND m.estado_matricula = 'MATRICULADO' AND m.seccion_id IS NOT NULL
  AND EXISTS (SELECT 1 FROM public.obligaciones_pago o WHERE o.matricula_id = m.id
              AND o.tipo_concepto = 'MATRICULA' AND o.estado = 'PAGADO_TOTAL');
