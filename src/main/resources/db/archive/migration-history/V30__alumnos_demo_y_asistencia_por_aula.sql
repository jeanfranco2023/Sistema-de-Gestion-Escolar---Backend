-- Personas ficticias con nombres plausibles para desarrollo: hasta 10 matrículas por aula activa.
-- DNI sintéticos, no validados por RENIEC; no se crean apoderados ni pagos.
UPDATE public.estudiantes
SET validado_reniec = FALSE,
    origen_registro = 'MANUAL_CONTINGENCIA'
WHERE numero_documento IN ('71234567', '72345678', '73456789');

UPDATE public.apoderados
SET validado_reniec = FALSE,
    origen_registro = 'MANUAL_CONTINGENCIA'
WHERE numero_documento = '70123456';

UPDATE public.matriculas m
SET observaciones = 'REGISTRO DEMO SINTETICO; identidad y documentos no validados.'
FROM public.estudiantes e
WHERE e.id = m.estudiante_id
  AND e.numero_documento IN ('71234567', '72345678', '73456789');

WITH secciones_objetivo AS (
    SELECT s.id AS seccion_id,
           s.anio_lectivo_id,
           n.codigo_nivel,
           g.numero_grado,
           LEAST(10, s.cupo_maximo)::INTEGER AS objetivo,
           COUNT(m.id) FILTER (
               WHERE m.estado_matricula IN ('MATRICULADO', 'RESERVADA_TEMPORAL')
                 AND e.activo IS TRUE
           )::INTEGER AS existentes
    FROM public.secciones s
    JOIN public.aulas au ON au.id = s.aula_id AND au.activa IS TRUE
    JOIN public.anios_lectivos al ON al.id = s.anio_lectivo_id AND al.anio = 2026
    JOIN public.grados g ON g.id = s.grado_id
    JOIN public.niveles n ON n.id = s.nivel_id
    LEFT JOIN public.matriculas m ON m.seccion_id = s.id
    LEFT JOIN public.estudiantes e ON e.id = m.estudiante_id
    GROUP BY s.id, s.anio_lectivo_id, n.codigo_nivel, g.numero_grado, s.cupo_maximo
), vacantes_demo AS (
    SELECT so.*,
           slot.numero AS numero_demo
    FROM secciones_objetivo so
    CROSS JOIN LATERAL generate_series(1, GREATEST(so.objetivo - so.existentes, 0)) AS slot(numero)
), catalogo_demo AS (
    SELECT ARRAY[
               'Ana Lucía', 'Valeria Fernanda', 'Luciana Isabel', 'Camila Sofía',
               'Daniela Fiorella', 'Mía Alejandra', 'Gabriela Nicole', 'Ariana Belén',
               'Antonella Abril', 'Catalina Lucía', 'Emilia Rafaela', 'Alessia Victoria',
               'Danna Ximena', 'María Fernanda', 'Lucía Valentina'
           ]::TEXT[] AS nombres_femeninos,
           ARRAY[
               'Mateo Sebastián', 'Diego Alejandro', 'Thiago Gabriel', 'Santiago David',
               'Nicolás Andrés', 'Sebastián Alonso', 'Bruno Adrián', 'Renzo Joaquín',
               'Iker Matías', 'Fabián Eduardo', 'André Esteban', 'José Manuel',
               'Luis Ángel', 'Alonso Martín', 'Marcos Eduardo'
           ]::TEXT[] AS nombres_masculinos,
           ARRAY[
               'Quispe', 'Huamán', 'Flores', 'Mendoza', 'García', 'Rojas', 'Vargas',
               'Castillo', 'Cruz', 'Ramírez', 'Chávez', 'Torres', 'Gonzales', 'Sánchez',
               'Fernández', 'Paredes', 'Herrera', 'Díaz', 'Morales', 'Salazar', 'Vásquez',
               'Espinoza', 'López', 'Reyes', 'Aguilar', 'Rivera', 'Cárdenas', 'Valdivia',
               'Campos', 'Navarro'
           ]::TEXT[] AS apellidos
)
INSERT INTO public.estudiantes (
    tipo_documento, numero_documento, nombres_estudiante,
    apellido_paterno_estudiante, apellido_materno_estudiante,
    fecha_nacimiento, genero, validado_reniec, origen_registro, activo
)
SELECT 'DNI',
       '98' || LPAD(vd.seccion_id::TEXT, 3, '0') || LPAD(vd.numero_demo::TEXT, 3, '0'),
       CASE WHEN (vd.seccion_id + vd.numero_demo) % 2 = 0
            THEN cd.nombres_masculinos[((vd.seccion_id * 7 + vd.numero_demo - 1)
                                         % cardinality(cd.nombres_masculinos) + 1)::INTEGER]
            ELSE cd.nombres_femeninos[((vd.seccion_id * 7 + vd.numero_demo - 1)
                                        % cardinality(cd.nombres_femeninos) + 1)::INTEGER]
       END,
       cd.apellidos[((vd.seccion_id * 11 + vd.numero_demo - 1) % cardinality(cd.apellidos) + 1)::INTEGER],
       cd.apellidos[((vd.seccion_id * 11 + vd.numero_demo + 9) % cardinality(cd.apellidos) + 1)::INTEGER],
       MAKE_DATE(
           2026 - CASE vd.codigo_nivel
                      WHEN 'INICIAL' THEN vd.numero_grado + 2
                      WHEN 'PRIMARIA' THEN vd.numero_grado + 5
                      ELSE vd.numero_grado + 11
                  END
                  - CASE WHEN 1 + ((vd.seccion_id + vd.numero_demo) % 12) > 3
                              OR (1 + ((vd.seccion_id + vd.numero_demo) % 12) = 3
                                  AND 1 + ((vd.seccion_id * 3 + vd.numero_demo) % 27) > 2)
                         THEN 1 ELSE 0 END,
           (1 + ((vd.seccion_id + vd.numero_demo) % 12))::INTEGER,
           (1 + ((vd.seccion_id * 3 + vd.numero_demo) % 27))::INTEGER),
       CASE WHEN (vd.seccion_id + vd.numero_demo) % 2 = 0 THEN 'M' ELSE 'F' END,
       FALSE,
       'MANUAL_CONTINGENCIA',
       TRUE
FROM vacantes_demo vd
CROSS JOIN catalogo_demo cd
ON CONFLICT (tipo_documento, numero_documento) DO NOTHING;

-- Las matrículas pasan por el trigger normal de vacantes; no se actualiza el contador a mano.
WITH aulas_demo AS (
    SELECT s.id AS seccion_id,
           s.anio_lectivo_id,
           '98' || LPAD(s.id::TEXT, 3, '0') || LPAD(slot.numero::TEXT, 3, '0') AS dni_demo
    FROM public.secciones s
    JOIN public.aulas au ON au.id = s.aula_id AND au.activa IS TRUE
    JOIN public.anios_lectivos al ON al.id = s.anio_lectivo_id AND al.anio = 2026
    CROSS JOIN LATERAL generate_series(1, LEAST(10, s.cupo_maximo)) AS slot(numero)
)
INSERT INTO public.matriculas (
    anio_lectivo_id, estudiante_id, seccion_id, fecha_matricula,
    estado_matricula, observaciones
)
SELECT ad.anio_lectivo_id,
       e.id,
       ad.seccion_id,
       TIMESTAMPTZ '2026-03-02 08:00:00-05',
       'MATRICULADO',
       'REGISTRO DEMO SINTETICO; identidad no validada; uso exclusivo de desarrollo.'
FROM aulas_demo ad
JOIN public.estudiantes e ON e.tipo_documento = 'DNI' AND e.numero_documento = ad.dni_demo
WHERE NOT EXISTS (
    SELECT 1 FROM public.matriculas m
    WHERE m.anio_lectivo_id = ad.anio_lectivo_id AND m.estudiante_id = e.id
)
ON CONFLICT (anio_lectivo_id, estudiante_id) DO NOTHING;

-- Muestra de asistencia de días lectivos del periodo IV, con ausencias injustificadas
-- y tardanzas. No se inventan justificaciones ni archivos de sustento.
WITH dias_lectivos AS (
    SELECT fecha::DATE AS fecha_sesion
    FROM generate_series(DATE '2026-10-19', DATE '2026-10-28', INTERVAL '1 day') AS dia(fecha)
    WHERE EXTRACT(ISODOW FROM fecha) BETWEEN 1 AND 5
), matriculas_demo AS (
    SELECT m.id
    FROM public.matriculas m
    JOIN public.anios_lectivos al ON al.id = m.anio_lectivo_id AND al.anio = 2026
    JOIN public.aulas au ON au.id = (SELECT s.aula_id FROM public.secciones s WHERE s.id = m.seccion_id)
                         AND au.activa IS TRUE
    WHERE m.estado_matricula IN ('MATRICULADO', 'RESERVADA_TEMPORAL')
), asistencias AS (
    SELECT md.id AS matricula_id,
           dl.fecha_sesion,
           CASE (md.id + (dl.fecha_sesion - DATE '2026-10-19')) % 37
               WHEN 0 THEN 'TARDANZA'
               WHEN 1 THEN 'TARDANZA'
               WHEN 2 THEN 'FALTA_INJUSTIFICADA'
               ELSE 'PRESENTE'
           END AS estado
    FROM matriculas_demo md
    CROSS JOIN dias_lectivos dl
)
INSERT INTO public.asistencias_aula (
    matricula_id, fecha_sesion, hora_registro, estado,
    auxiliar_usuario_id, justificada
)
SELECT a.matricula_id,
       a.fecha_sesion,
       CASE a.estado
           WHEN 'TARDANZA' THEN TIME '08:10:00'
           WHEN 'FALTA_INJUSTIFICADA' THEN TIME '08:00:00'
           ELSE TIME '07:45:00'
       END,
       a.estado,
       u.id,
       FALSE
FROM asistencias a
LEFT JOIN public.usuarios u ON u.correo_electronico = 'auxiliar@shuji.edu.pe'
ON CONFLICT (matricula_id, fecha_sesion) DO NOTHING;
