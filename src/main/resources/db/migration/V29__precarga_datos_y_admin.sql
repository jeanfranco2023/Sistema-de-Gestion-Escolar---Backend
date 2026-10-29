-- Precarga de datos operacionales, catálogo curricular CNEB, horarios, padrón de prueba y usuarios institucionales.
-- Todas las operaciones son idempotentes para permitir múltiples ejecuciones sin duplicar registros.

-- 1. USUARIOS INSTITUCIONALES Y ROLES
-- Contraseña unificada para cuentas de prueba y administración: Admin2026!
-- Hash BCrypt estándar: $2a$10$m.2CkRxR18uYeRfeZKj8uOc3tcA77GRnQ1TVnhS9.GaOKLr2IMUa2

INSERT INTO public.usuarios (nombre_usuario, correo_electronico, usuario_password, activo)
SELECT u.username, u.email, '$2a$10$m.2CkRxR18uYeRfeZKj8uOc3tcA77GRnQ1TVnhS9.GaOKLr2IMUa2', TRUE
FROM (VALUES
    ('admin', 'admin@shuji.edu.pe'),
    ('docente', 'docente@shuji.edu.pe'),
    ('tutor', 'tutor@shuji.edu.pe'),
    ('auxiliar', 'auxiliar@shuji.edu.pe'),
    ('apoderado', 'apoderado@shuji.edu.pe')
) AS u(username, email)
WHERE NOT EXISTS (
    SELECT 1 FROM public.usuarios exist
    WHERE exist.nombre_usuario = u.username OR exist.correo_electronico = u.email
);

-- Asegurar credenciales y activación de cuentas base
UPDATE public.usuarios
SET usuario_password = '$2a$10$m.2CkRxR18uYeRfeZKj8uOc3tcA77GRnQ1TVnhS9.GaOKLr2IMUa2',
    activo = TRUE
WHERE correo_electronico IN (
    'admin@shuji.edu.pe', 'docente@shuji.edu.pe', 'tutor@shuji.edu.pe',
    'auxiliar@shuji.edu.pe', 'apoderado@shuji.edu.pe', 'fq94289@gmail.com'
);

-- Asignar roles a los usuarios institucionales
INSERT INTO public.usuario_roles (usuario_id, rol_id)
SELECT u.id, r.id
FROM public.usuarios u
CROSS JOIN public.roles r
WHERE (u.correo_electronico = 'admin@shuji.edu.pe' AND r.codigo_rol IN ('DIRECCION', 'SECRETARIA'))
   OR (u.correo_electronico = 'fq94289@gmail.com' AND r.codigo_rol = 'SECRETARIA')
   OR (u.correo_electronico = 'docente@shuji.edu.pe' AND r.codigo_rol = 'DOCENTE')
   OR (u.correo_electronico = 'tutor@shuji.edu.pe' AND r.codigo_rol IN ('TUTOR', 'DOCENTE'))
   OR (u.correo_electronico = 'auxiliar@shuji.edu.pe' AND r.codigo_rol = 'AUXILIAR')
   OR (u.correo_electronico = 'apoderado@shuji.edu.pe' AND r.codigo_rol = 'APODERADO')
ON CONFLICT (usuario_id, rol_id) DO NOTHING;

-- 2. BLOQUES HORARIOS DIARIOS
INSERT INTO public.bloques_horarios (numero_bloque, hora_inicio, hora_fin, es_recreo) VALUES
    (1, '08:00:00', '08:45:00', FALSE),
    (2, '08:45:00', '09:30:00', FALSE),
    (3, '09:30:00', '10:15:00', FALSE),
    (4, '10:15:00', '10:45:00', TRUE),
    (5, '10:45:00', '11:30:00', FALSE),
    (6, '11:30:00', '12:15:00', FALSE),
    (7, '12:15:00', '13:00:00', FALSE),
    (8, '13:00:00', '13:45:00', FALSE)
ON CONFLICT (numero_bloque) DO NOTHING;

-- 3. ÁREAS CURRICULARES NACIONALES (CNEB)
INSERT INTO public.areas_curriculares (nivel_id, codigo_area, nombre_area)
SELECT n.id, a.codigo, a.nombre
FROM (VALUES
    ('INICIAL', 'COM', 'Comunicación'),
    ('INICIAL', 'MAT', 'Matemática'),
    ('INICIAL', 'PS', 'Personal Social'),
    ('INICIAL', 'CYT', 'Ciencia y Tecnología'),
    ('INICIAL', 'PSIC', 'Psicomotricidad'),
    ('PRIMARIA', 'MAT', 'Matemática'),
    ('PRIMARIA', 'COM', 'Comunicación'),
    ('PRIMARIA', 'CYT', 'Ciencia y Tecnología'),
    ('PRIMARIA', 'PS', 'Personal Social'),
    ('PRIMARIA', 'ARTE', 'Arte y Cultura'),
    ('PRIMARIA', 'EF', 'Educación Física'),
    ('PRIMARIA', 'ING', 'Inglés'),
    ('PRIMARIA', 'ER', 'Educación Religiosa'),
    ('SECUNDARIA', 'MAT', 'Matemática'),
    ('SECUNDARIA', 'COM', 'Comunicación'),
    ('SECUNDARIA', 'ING', 'Inglés'),
    ('SECUNDARIA', 'ARTE', 'Arte y Cultura'),
    ('SECUNDARIA', 'CCSS', 'Ciencias Sociales'),
    ('SECUNDARIA', 'DPCC', 'Desarrollo Personal y Ciudadanía'),
    ('SECUNDARIA', 'EF', 'Educación Física'),
    ('SECUNDARIA', 'ER', 'Educación Religiosa'),
    ('SECUNDARIA', 'CYT', 'Ciencia y Tecnología'),
    ('SECUNDARIA', 'EPT', 'Educación para el Trabajo')
) AS a(codigo_nivel, codigo, nombre)
JOIN public.niveles n ON n.codigo_nivel = a.codigo_nivel
ON CONFLICT (nivel_id, codigo_area) DO NOTHING;

-- 4. COMPETENCIAS CNEB
INSERT INTO public.competencias (area_id, numero_orden, nombre_competencia, descripcion)
SELECT ac.id, c.numero_orden, c.nombre, c.descripcion
FROM (VALUES
    ('MAT', 1::SMALLINT, 'Resuelve problemas de cantidad', 'Capacidad para traducir cantidades a expresiones numéricas y estimar cálculos'),
    ('MAT', 2::SMALLINT, 'Resuelve problemas de regularidad y cambio', 'Capacidad para traducir datos a expresiones algebraicas y patrones'),
    ('MAT', 3::SMALLINT, 'Resuelve problemas de forma y movimiento', 'Modela objetos con formas geométricas y sus transformaciones'),
    ('MAT', 4::SMALLINT, 'Resuelve problemas de gestión de datos', 'Representa datos con gráficos y medidas estadísticas o probabilísticas'),
    ('COM', 1::SMALLINT, 'Se comunica oralmente en su lengua materna', 'Obtiene información de textos orales e interactúa estratégicamente'),
    ('COM', 2::SMALLINT, 'Lee diversos tipos de textos escritos', 'Identifica información explícita e infiere el sentido global'),
    ('COM', 3::SMALLINT, 'Escribe diversos tipos de textos', 'Adecúa el texto a la situación comunicativa y organiza ideas de forma coherente'),
    ('CYT', 1::SMALLINT, 'Indaga mediante métodos científicos', 'Problematiza situaciones y diseña estrategias para hacer indagación'),
    ('CYT', 2::SMALLINT, 'Explica el mundo físico y sus fenómenos', 'Comprende y usa conocimientos sobre los seres vivos, materia y energía'),
    ('CYT', 3::SMALLINT, 'Diseña soluciones tecnológicas', 'Determina una alternativa de solución tecnológica y evalúa su funcionamiento'),
    ('PS', 1::SMALLINT, 'Construye su identidad', 'Se valora a sí mismo y autorregula sus emociones'),
    ('PS', 2::SMALLINT, 'Convive y participa democráticamente', 'Interactúa con todas las personas y construye normas y acuerdos')
) AS c(codigo_area, numero_orden, nombre, descripcion)
JOIN public.areas_curriculares ac ON ac.codigo_area = c.codigo_area
WHERE NOT EXISTS (
    SELECT 1 FROM public.competencias comp
    WHERE comp.area_id = ac.id AND comp.numero_orden = c.numero_orden
);

-- 5. ASIGNACIONES DOCENTES (2026)
INSERT INTO public.asignaciones_docentes (docente_usuario_id, seccion_id, anio_lectivo_id, nivel_id, area_curricular_id)
SELECT u.id, s.id, a.id, n.id, ac.id
FROM public.anios_lectivos a
JOIN public.secciones s ON s.anio_lectivo_id = a.id AND s.letra = 'A'
JOIN public.grados g ON g.id = s.grado_id
JOIN public.niveles n ON n.id = s.nivel_id
JOIN public.areas_curriculares ac ON ac.nivel_id = n.id
CROSS JOIN public.usuarios u
WHERE a.anio = 2026
  AND (
    (n.codigo_nivel = 'PRIMARIA' AND g.numero_grado = 1 AND ac.codigo_area = 'MAT' AND u.correo_electronico = 'docente@shuji.edu.pe') OR
    (n.codigo_nivel = 'PRIMARIA' AND g.numero_grado = 1 AND ac.codigo_area = 'COM' AND u.correo_electronico = 'docente@shuji.edu.pe') OR
    (n.codigo_nivel = 'PRIMARIA' AND g.numero_grado = 1 AND ac.codigo_area = 'PS'  AND u.correo_electronico = 'tutor@shuji.edu.pe')   OR
    (n.codigo_nivel = 'SECUNDARIA' AND g.numero_grado = 1 AND ac.codigo_area = 'MAT' AND u.correo_electronico = 'docente@shuji.edu.pe') OR
    (n.codigo_nivel = 'SECUNDARIA' AND g.numero_grado = 1 AND ac.codigo_area = 'COM' AND u.correo_electronico = 'tutor@shuji.edu.pe')
  )
ON CONFLICT (seccion_id, area_curricular_id, anio_lectivo_id) DO NOTHING;

-- 6. MALLA HORARIA ACTIVA (1° Primaria A)
INSERT INTO public.horarios_seccion (anio_lectivo_id, seccion_id, docente_usuario_id, asignacion_docente_id, dia_semana, bloque_horario_id, activo)
SELECT ad.anio_lectivo_id, ad.seccion_id, ad.docente_usuario_id, ad.id, h.dia, bh.id, TRUE
FROM public.asignaciones_docentes ad
JOIN public.secciones s ON s.id = ad.seccion_id
JOIN public.grados g ON g.id = s.grado_id
JOIN public.niveles n ON n.id = s.nivel_id
JOIN public.areas_curriculares ac ON ac.id = ad.area_curricular_id
CROSS JOIN (VALUES
    ('PRIMARIA', 1, 'MAT', 1::SMALLINT, 1::SMALLINT), -- Lunes Bloque 1
    ('PRIMARIA', 1, 'MAT', 1::SMALLINT, 2::SMALLINT), -- Lunes Bloque 2
    ('PRIMARIA', 1, 'COM', 1::SMALLINT, 3::SMALLINT), -- Lunes Bloque 3
    ('PRIMARIA', 1, 'COM', 2::SMALLINT, 1::SMALLINT), -- Martes Bloque 1
    ('PRIMARIA', 1, 'COM', 2::SMALLINT, 2::SMALLINT), -- Martes Bloque 2
    ('PRIMARIA', 1, 'PS',  2::SMALLINT, 3::SMALLINT), -- Martes Bloque 3
    ('PRIMARIA', 1, 'MAT', 3::SMALLINT, 1::SMALLINT), -- Miércoles Bloque 1
    ('PRIMARIA', 1, 'MAT', 3::SMALLINT, 2::SMALLINT), -- Miércoles Bloque 2
    ('PRIMARIA', 1, 'PS',  4::SMALLINT, 1::SMALLINT), -- Jueves Bloque 1
    ('PRIMARIA', 1, 'PS',  4::SMALLINT, 2::SMALLINT), -- Jueves Bloque 2
    ('PRIMARIA', 1, 'COM', 5::SMALLINT, 1::SMALLINT), -- Viernes Bloque 1
    ('PRIMARIA', 1, 'COM', 5::SMALLINT, 2::SMALLINT)  -- Viernes Bloque 2
) AS h(nivel, grado, area, dia, num_bloque)
JOIN public.bloques_horarios bh ON bh.numero_bloque = h.num_bloque
WHERE n.codigo_nivel = h.nivel AND g.numero_grado = h.grado AND ac.codigo_area = h.area
  AND NOT EXISTS (
    SELECT 1 FROM public.horarios_seccion hs
    WHERE hs.seccion_id = ad.seccion_id
      AND hs.dia_semana = h.dia
      AND hs.bloque_horario_id = bh.id
      AND hs.anio_lectivo_id = ad.anio_lectivo_id
      AND hs.activo IS TRUE
  );

-- 7. APODERADOS Y ESTUDIANTES DEMO
INSERT INTO public.apoderados (
    tipo_documento, numero_documento, nombres_apoderado, apellido_paterno_apoderado,
    apellido_materno_apoderado, celular, correo_electronico, direccion, ubigeo_inei,
    validado_reniec, origen_registro, usuario_id
)
SELECT 'DNI', '70123456', 'Carlos Alberto', 'Rodríguez', 'Mendoza', '987654321',
       'apoderado@shuji.edu.pe', 'Av. Salaverry 1234 Dpto 402, Jesús María', '150113',
       TRUE, 'RENIEC_API', u.id
FROM public.usuarios u
WHERE u.correo_electronico = 'apoderado@shuji.edu.pe'
ON CONFLICT (tipo_documento, numero_documento) DO UPDATE
    SET usuario_id = EXCLUDED.usuario_id;

INSERT INTO public.estudiantes (
    tipo_documento, numero_documento, nombres_estudiante, apellido_paterno_estudiante,
    apellido_materno_estudiante, fecha_nacimiento, genero, codigo_estudiante_siagie,
    validado_reniec, origen_registro, activo
) VALUES
    ('DNI', '71234567', 'Mateo Alexander', 'Rodríguez', 'Flores', DATE '2019-05-15', 'M', '20267123456701', TRUE, 'RENIEC_API', TRUE),
    ('DNI', '72345678', 'Lucía Valentina', 'Rodríguez', 'Flores', DATE '2020-08-20', 'F', '20267234567802', TRUE, 'RENIEC_API', TRUE),
    ('DNI', '73456789', 'Santiago David', 'Morales', 'Castillo', DATE '2019-03-10', 'M', '20267345678903', TRUE, 'RENIEC_API', TRUE)
ON CONFLICT (tipo_documento, numero_documento) DO NOTHING;

-- Vincular apoderado con estudiantes
INSERT INTO public.estudiante_apoderados (
    estudiante_id, apoderado_id, parentesco, es_responsable_economico, tiene_custodia, permite_recojo
)
SELECT e.id, a.id, 'PADRE', TRUE, TRUE, TRUE
FROM public.estudiantes e
CROSS JOIN public.apoderados a
WHERE a.numero_documento = '70123456'
  AND e.numero_documento IN ('71234567', '72345678')
ON CONFLICT (estudiante_id, apoderado_id) DO NOTHING;

-- 8. MATRÍCULAS 2026
INSERT INTO public.matriculas (
    anio_lectivo_id, estudiante_id, seccion_id, fecha_matricula, estado_matricula, observaciones
)
SELECT a.id, e.id, s.id, CURRENT_TIMESTAMP, 'MATRICULADO', 'Matrícula confirmada con documentación validada y pago completo.'
FROM public.anios_lectivos a
JOIN public.secciones s ON s.anio_lectivo_id = a.id AND s.letra = 'A'
JOIN public.grados g ON g.id = s.grado_id
JOIN public.niveles n ON n.id = s.nivel_id
CROSS JOIN public.estudiantes e
WHERE a.anio = 2026
  AND (
    (e.numero_documento = '71234567' AND n.codigo_nivel = 'PRIMARIA' AND g.numero_grado = 1) OR
    (e.numero_documento = '72345678' AND n.codigo_nivel = 'INICIAL'  AND g.numero_grado = 3) OR
    (e.numero_documento = '73456789' AND n.codigo_nivel = 'PRIMARIA' AND g.numero_grado = 1)
  )
ON CONFLICT (anio_lectivo_id, estudiante_id) DO NOTHING;

-- 9. CRONOGRAMA DE OBLIGACIONES DE PAGO (Matrícula de Mateo Rodríguez)
-- Se inicializan en total_pagado = 0.00 y PENDIENTE para que los pagos registrados apliquen mediante trigger
INSERT INTO public.obligaciones_pago (
    matricula_id, concepto_id, tipo_concepto, numero_cuota, descripcion,
    fecha_vencimiento, monto_base, monto_mora, monto_descuento, total_pagado, estado
)
SELECT m.id, c.id, c.tipo_concepto, o.cuota, o.descrip, o.vencimiento, o.monto, 0.00, 0.00, 0.00, 'PENDIENTE'
FROM public.matriculas m
JOIN public.estudiantes e ON e.id = m.estudiante_id
JOIN public.anios_lectivos al ON al.id = m.anio_lectivo_id
CROSS JOIN (VALUES
    ('MATRICULA', 0::SMALLINT, 'Derecho de Matrícula y Admisión 2026', DATE '2026-02-28', 350.00),
    ('PENSION',   1::SMALLINT, 'Pensión Escolar - Cuota 1 (Marzo 2026)', DATE '2026-03-31', 380.00),
    ('PENSION',   2::SMALLINT, 'Pensión Escolar - Cuota 2 (Abril 2026)', DATE '2026-04-30', 380.00),
    ('PENSION',   3::SMALLINT, 'Pensión Escolar - Cuota 3 (Mayo 2026)',  DATE '2026-05-31', 380.00)
) AS o(tipo, cuota, descrip, vencimiento, monto)
JOIN public.conceptos_cobro c ON c.tipo_concepto = o.tipo
WHERE e.numero_documento = '71234567' AND al.anio = 2026
ON CONFLICT (matricula_id, concepto_id, numero_cuota) DO NOTHING;

-- 10. TRANSACCIONES DE PAGO Y COMPROBANTES EMITIDOS
-- Al insertar con estado_pago = 'APROBADO', el trigger fn_aplicar_pago_obligacion actualiza automáticamente obligaciones_pago
INSERT INTO public.pagos_transacciones (
    obligacion_pago_id, pasarela_transaccion_id, pasarela_proveedor, metodo_pago,
    monto_pagado, estado_pago, payload_webhook
)
SELECT op.id, 'DEMO-MP-' || op.tipo_concepto || '-' || op.numero_cuota || '-71234567',
       'MERCADO_PAGO', 'TARJETA_DEBITO', op.monto_base, 'APROBADO',
       '{"status":"approved","payment_method_id":"visa","authorization_code":"AUTH2026"}'::jsonb
FROM public.obligaciones_pago op
JOIN public.matriculas m ON m.id = op.matricula_id
JOIN public.estudiantes e ON e.id = m.estudiante_id
WHERE e.numero_documento = '71234567'
  AND op.numero_cuota IN (0, 1)
ON CONFLICT (pasarela_transaccion_id) DO NOTHING;

INSERT INTO public.comprobantes_pago (
    pago_transaccion_id, tipo_comprobante, serie, correlativo, fecha_emision, monto_total, estado_comprobante
)
SELECT pt.id, 'BOLETA', 'B001',
       (COALESCE((SELECT MAX(cp2.correlativo) FROM public.comprobantes_pago cp2 WHERE cp2.serie = 'B001'), 0)
        + ROW_NUMBER() OVER (ORDER BY pt.id))::INTEGER,
       CURRENT_TIMESTAMP, pt.monto_pagado, 'EMITIDO'
FROM public.pagos_transacciones pt
WHERE pt.pasarela_transaccion_id LIKE 'DEMO-MP-%-71234567'
  AND NOT EXISTS (
      SELECT 1 FROM public.comprobantes_pago cp WHERE cp.pago_transaccion_id = pt.id
  );

UPDATE public.series_comprobante
SET ultimo_correlativo = COALESCE((SELECT MAX(cp.correlativo) FROM public.comprobantes_pago cp WHERE cp.serie = 'B001'), 0)
WHERE serie = 'B001';

-- 11. CALIFICACIONES CNEB I BIMESTRE
INSERT INTO public.calificaciones_cneb (
    matricula_id, anio_lectivo_id, seccion_id, periodo_academico_id,
    asignacion_docente_id, area_curricular_id, docente_usuario_id,
    competencia_id, calificacion_cualitativa, conclusion_descriptiva, sugerencia_ia_utilizada
)
SELECT m.id, ad.anio_lectivo_id, ad.seccion_id, pa.id, ad.id, ad.area_curricular_id,
       ad.docente_usuario_id, comp.id,
       CASE comp.numero_orden WHEN 1 THEN 'AD' ELSE 'A' END,
       'El estudiante demuestra excelente autonomía en la resolución de problemas y participa activamente en clase.',
       FALSE
FROM public.matriculas m
JOIN public.estudiantes e ON e.id = m.estudiante_id
JOIN public.asignaciones_docentes ad ON ad.seccion_id = m.seccion_id AND ad.anio_lectivo_id = m.anio_lectivo_id
JOIN public.competencias comp ON comp.area_id = ad.area_curricular_id
JOIN public.periodos_academicos pa ON pa.anio_lectivo_id = m.anio_lectivo_id AND pa.numero_periodo = 1
WHERE e.numero_documento = '71234567'
ON CONFLICT (matricula_id, competencia_id, periodo_academico_id) DO NOTHING;

-- 12. ASISTENCIA DE AULA
INSERT INTO public.asistencias_aula (
    matricula_id, fecha_sesion, hora_registro, estado, auxiliar_usuario_id, justificada
)
SELECT m.id, sesion.fecha, '08:05:00'::TIME, sesion.estado, u.id, FALSE
FROM public.matriculas m
JOIN public.estudiantes e ON e.id = m.estudiante_id
CROSS JOIN (VALUES
    (DATE '2026-03-02', 'PRESENTE'),
    (DATE '2026-03-03', 'PRESENTE'),
    (DATE '2026-03-04', 'TARDANZA'),
    (DATE '2026-03-05', 'PRESENTE'),
    (DATE '2026-03-06', 'PRESENTE')
) AS sesion(fecha, estado)
LEFT JOIN public.usuarios u ON u.correo_electronico = 'auxiliar@shuji.edu.pe'
WHERE e.numero_documento = '71234567'
ON CONFLICT (matricula_id, fecha_sesion) DO NOTHING;

-- 13. COMUNICADOS OFICIALES
INSERT INTO public.comunicados_oficiales (
    titulo_comunicado, contenido, remitente_usuario_id, fecha_publicacion, requiere_acuse
)
SELECT 'Bienvenida al Año Escolar 2026',
       'Estimada comunidad educativa Shuji Kitamura: Les damos la más cordial bienvenida al periodo lectivo 2026. Reiteramos nuestro compromiso con la excelencia y la formación integral de nuestros estudiantes.',
       u.id, CURRENT_TIMESTAMP, TRUE
FROM public.usuarios u
WHERE u.correo_electronico = 'admin@shuji.edu.pe'
  AND NOT EXISTS (
      SELECT 1 FROM public.comunicados_oficiales WHERE titulo_comunicado = 'Bienvenida al Año Escolar 2026'
  );

INSERT INTO public.comunicado_destinatarios (
    comunicado_id, apoderado_id, leido, fecha_lectura, acuse_confirmado, fecha_acuse
)
SELECT co.id, a.id, TRUE, CURRENT_TIMESTAMP, TRUE, CURRENT_TIMESTAMP
FROM public.comunicados_oficiales co
JOIN public.apoderados a ON a.numero_documento = '70123456'
WHERE co.titulo_comunicado = 'Bienvenida al Año Escolar 2026'
  AND NOT EXISTS (
      SELECT 1 FROM public.comunicado_destinatarios cd
      WHERE cd.comunicado_id = co.id AND cd.apoderado_id = a.id
  );

-- 14. INCIDENCIA FORMATIVA / CONVIVENCIA
INSERT INTO public.incidencias_conductuales (
    matricula_id, fecha_incidencia, tipo_falta, descripcion,
    reportado_por_usuario_id, requiere_citacion, estado
)
SELECT m.id, DATE '2026-03-04', 'LEVE',
       'El estudiante olvidó su texto de trabajo de Comunicación. Se coordinó con el apoderado mediante agenda escolar.',
       u.id, FALSE, 'ATENDIDA'
FROM public.matriculas m
JOIN public.estudiantes e ON e.id = m.estudiante_id
LEFT JOIN public.usuarios u ON u.correo_electronico = 'auxiliar@shuji.edu.pe'
WHERE e.numero_documento = '71234567'
  AND NOT EXISTS (
      SELECT 1 FROM public.incidencias_conductuales ic
      WHERE ic.matricula_id = m.id AND ic.fecha_incidencia = DATE '2026-03-04'
  );

-- 15. SOLICITUD DE MATRÍCULA PÚBLICA DE MUESTRA PARA REVISIÓN
INSERT INTO public.solicitudes_matricula_publica (
    id, huella_token, estado, anio_lectivo_id, seccion_id,
    numero_documento_estudiante, nombres_estudiante, apellido_paterno_estudiante, apellido_materno_estudiante,
    fecha_nacimiento_estudiante, genero_estudiante, numero_documento_apoderado, nombres_apoderado,
    apellido_paterno_apoderado, apellido_materno_apoderado, celular_apoderado, correo_apoderado,
    direccion_apoderado, ubigeo_apoderado, parentesco, consentimiento_gemini,
    requiere_autorizacion_edad, estado_partida, estado_dni_c4, estado_recibo, pago_monto
)
SELECT 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'::UUID,
       '842d74c97fcdb52d2ef74d11ea61c92cc04d05479364fa69a9408f2a145fdf28',
       'DOCUMENTOS_EN_VALIDACION', al.id, s.id,
       '74567890', 'Joaquín André', 'Vargas', 'Paredes',
       DATE '2019-06-25', 'M', '75678901', 'Patricia Rosa',
       'Paredes', 'Salas', '981234567', 'patricia.paredes@example.com',
       'Calle Los Cipreses 450, Jesús María', '150113', 'MADRE', TRUE,
       FALSE, 'EN_VALIDACION', 'VALIDADO', 'PENDIENTE', 350.00
FROM public.anios_lectivos al
JOIN public.secciones s ON s.anio_lectivo_id = al.id AND s.letra = 'A'
JOIN public.grados g ON g.id = s.grado_id AND g.numero_grado = 1
JOIN public.niveles n ON n.id = s.nivel_id AND n.codigo_nivel = 'PRIMARIA'
WHERE al.anio = 2026
ON CONFLICT (id) DO NOTHING;
