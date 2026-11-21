-- Agrega modalidad de ingreso (REGULAR / TRASLADO), colegio de procedencia y documento acreditativo de estudios

ALTER TABLE public.solicitudes_matricula_publica
  ADD COLUMN IF NOT EXISTS modalidad_ingreso character varying(20) DEFAULT 'REGULAR' NOT NULL,
  ADD COLUMN IF NOT EXISTS colegio_procedencia character varying(150),
  ADD COLUMN IF NOT EXISTS estado_certificado character varying(15) DEFAULT 'PENDIENTE' NOT NULL,
  ADD COLUMN IF NOT EXISTS observacion_certificado character varying(500);

ALTER TABLE public.solicitudes_matricula_publica
  DROP CONSTRAINT IF EXISTS solicitudes_matricula_publica_modalidad_ingreso_check,
  ADD CONSTRAINT solicitudes_matricula_publica_modalidad_ingreso_check
    CHECK (((modalidad_ingreso)::text = ANY ((ARRAY['REGULAR'::character varying, 'TRASLADO'::character varying])::text[])));

ALTER TABLE public.solicitudes_matricula_publica
  DROP CONSTRAINT IF EXISTS solicitudes_matricula_publica_estado_certificado_check,
  ADD CONSTRAINT solicitudes_matricula_publica_estado_certificado_check
    CHECK (((estado_certificado)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'EN_VALIDACION'::character varying, 'VALIDADO'::character varying, 'OBSERVADO'::character varying])::text[])));

ALTER TABLE public.matriculas
  ADD COLUMN IF NOT EXISTS modalidad_ingreso character varying(20) DEFAULT 'REGULAR' NOT NULL,
  ADD COLUMN IF NOT EXISTS colegio_procedencia character varying(150);

ALTER TABLE public.matriculas
  DROP CONSTRAINT IF EXISTS matriculas_modalidad_ingreso_check,
  ADD CONSTRAINT matriculas_modalidad_ingreso_check
    CHECK (((modalidad_ingreso)::text = ANY ((ARRAY['REGULAR'::character varying, 'TRASLADO'::character varying])::text[])));

ALTER TABLE public.solicitudes_matricula_documentos
  DROP CONSTRAINT IF EXISTS solicitudes_matricula_documentos_tipo_check,
  ADD CONSTRAINT solicitudes_matricula_documentos_tipo_check
    CHECK (((tipo)::text = ANY ((ARRAY[
      'PARTIDA_NACIMIENTO'::character varying,
      'DNI_C4'::character varying,
      'RECIBO_SERVICIO'::character varying,
      'CERTIFICADO_ESTUDIOS'::character varying,
      'CONSTANCIA_PAGO'::character varying
    ])::text[])));

ALTER TABLE public.versiones_documentos_matricula
  DROP CONSTRAINT IF EXISTS versiones_documentos_matricula_tipo_check,
  ADD CONSTRAINT versiones_documentos_matricula_tipo_check
    CHECK (((tipo)::text = ANY ((ARRAY[
      'PARTIDA_NACIMIENTO'::character varying,
      'DNI_C4'::character varying,
      'RECIBO_SERVICIO'::character varying,
      'CERTIFICADO_ESTUDIOS'::character varying,
      'CONSTANCIA_PAGO'::character varying
    ])::text[])));
