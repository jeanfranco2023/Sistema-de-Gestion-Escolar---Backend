-- Actualiza únicamente las identidades sintéticas creadas por V30.
-- Los registros reales y los datos de V29 no se modifican.
WITH catalogo AS (
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
), alumnos_demo AS (
    SELECT e.id,
           SUBSTRING(e.numero_documento, 3, 3)::INTEGER AS seccion_id,
           SUBSTRING(e.numero_documento, 6, 3)::INTEGER AS numero_demo
    FROM public.estudiantes e
    WHERE e.tipo_documento = 'DNI'
      AND e.numero_documento ~ '^98[0-9]{6}$'
      AND e.validado_reniec IS FALSE
      AND e.origen_registro = 'MANUAL_CONTINGENCIA'
      AND EXISTS (
          SELECT 1
          FROM public.matriculas m
          WHERE m.estudiante_id = e.id
            AND m.observaciones LIKE 'REGISTRO DEMO SINTETICO;%'
      )
), nombres_normalizados AS (
    SELECT d.id,
           CASE WHEN (d.seccion_id + d.numero_demo) % 2 = 0
                THEN c.nombres_masculinos[((d.seccion_id * 7 + d.numero_demo - 1)
                                           % CARDINALITY(c.nombres_masculinos) + 1)::INTEGER]
                ELSE c.nombres_femeninos[((d.seccion_id * 7 + d.numero_demo - 1)
                                          % CARDINALITY(c.nombres_femeninos) + 1)::INTEGER]
           END AS nombres,
           c.apellidos[((d.seccion_id * 11 + d.numero_demo - 1)
                        % CARDINALITY(c.apellidos) + 1)::INTEGER] AS apellido_paterno,
           c.apellidos[((d.seccion_id * 11 + d.numero_demo + 9)
                        % CARDINALITY(c.apellidos) + 1)::INTEGER] AS apellido_materno
    FROM alumnos_demo d
    CROSS JOIN catalogo c
)
UPDATE public.estudiantes e
SET nombres_estudiante = n.nombres,
    apellido_paterno_estudiante = n.apellido_paterno,
    apellido_materno_estudiante = n.apellido_materno
FROM nombres_normalizados n
WHERE e.id = n.id;
