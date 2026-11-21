INSERT INTO public.anios_lectivos (anio, fecha_inicio, fecha_fin, abierto)
SELECT 2027, (fecha_inicio + interval '1 year')::date, (fecha_fin + interval '1 year')::date, true
FROM public.anios_lectivos WHERE anio = 2026
ON CONFLICT (anio) DO NOTHING;

INSERT INTO public.secciones (anio_lectivo_id, grado_id, nivel_id, letra, cupo_maximo, aula_id)
SELECT nuevo.id, s.grado_id, s.nivel_id, s.letra, s.cupo_maximo, s.aula_id
FROM public.secciones s
JOIN public.anios_lectivos anterior ON anterior.id = s.anio_lectivo_id AND anterior.anio = 2026
JOIN public.anios_lectivos nuevo ON nuevo.anio = 2027
ON CONFLICT (anio_lectivo_id, grado_id, letra) DO NOTHING;

ALTER TABLE public.conceptos_cobro ALTER COLUMN dia_vencimiento SET DEFAULT 20;
UPDATE public.conceptos_cobro
SET dia_vencimiento = 20
WHERE codigo_concepto = 'PENSION_MENSUAL' AND dia_vencimiento <> 20;
