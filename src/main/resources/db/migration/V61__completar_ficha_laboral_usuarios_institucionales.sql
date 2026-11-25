-- V61: Completar datos de ficha laboral (telefono, direccion, tipo_contrato, codigo_modular, numero_colegiatura)
-- para usuarios institucionales existentes sin sobrescribir valores ya registrados.

UPDATE public.usuarios u
SET
  telefono = COALESCE(
    NULLIF(TRIM(u.telefono), ''),
    '9' || LPAD((80000000 + u.id)::text, 8, '0')
  ),
  direccion = COALESCE(
    NULLIF(TRIM(u.direccion), ''),
    'Av. Educación N° ' || (100 + u.id)::text || ', Lima'
  ),
  tipo_contrato = COALESCE(
    NULLIF(TRIM(u.tipo_contrato), ''),
    'PLANILLA_COMPLETA'
  ),
  codigo_modular = COALESCE(
    NULLIF(TRIM(u.codigo_modular), ''),
    'CM-' || LPAD((100000 + u.id)::text, 6, '0')
  ),
  numero_colegiatura = CASE
    WHEN EXISTS (
      SELECT 1
      FROM public.usuario_roles ur
      JOIN public.roles r ON r.id = ur.rol_id
      WHERE ur.usuario_id = u.id
        AND UPPER(r.codigo_rol) = 'DOCENTE'
    ) THEN COALESCE(
      NULLIF(TRIM(u.numero_colegiatura), ''),
      'CPPe-' || LPAD((20000 + u.id)::text, 5, '0')
    )
    ELSE u.numero_colegiatura
  END
WHERE EXISTS (
  SELECT 1
  FROM public.usuario_roles ur
  JOIN public.roles r ON r.id = ur.rol_id
  WHERE ur.usuario_id = u.id
    AND UPPER(r.codigo_rol) IN ('ADMINISTRADOR', 'DIRECCION', 'SECRETARIA', 'DOCENTE', 'TUTOR', 'AUXILIAR')
);
