-- Catálogo de prueba basado en las áreas de Educación Básica Regular del CNEB.
-- Se puede ejecutar más de una vez: conserva claves de área y no duplica competencias ni bloques.
BEGIN;

-- El conjunto anterior para Inicial provenía del ciclo I (0–2). El padrón usa ciclo II (3–5).
UPDATE public.areas_curriculares anterior
SET codigo_area = 'MAT', nombre_area = 'Matemática'
WHERE anterior.nivel_id = 1
  AND anterior.codigo_area = 'DSM'
  AND NOT EXISTS (
    SELECT 1 FROM public.areas_curriculares actual
    WHERE actual.nivel_id = 1 AND actual.codigo_area = 'MAT'
  );

UPDATE public.competencias c
SET nombre_competencia = CASE c.numero_orden
      WHEN 1 THEN 'Resuelve problemas de cantidad'
      WHEN 2 THEN 'Resuelve problemas de forma y localización'
    END,
    descripcion = CASE c.numero_orden
      WHEN 1 THEN 'Resuelve situaciones de cantidad adecuadas al ciclo inicial.'
      WHEN 2 THEN 'Resuelve situaciones de forma, movimiento y localización adecuadas al ciclo inicial.'
    END
FROM public.areas_curriculares a
WHERE c.area_id = a.id AND a.nivel_id = 1 AND a.codigo_area = 'MAT'
  AND c.numero_orden IN (1, 2);

WITH areas(nivel_id, codigo_area, nombre_area) AS (
  VALUES
    (1, 'PS', 'Personal Social'),
    (1, 'PSI', 'Psicomotriz'),
    (1, 'COM', 'Comunicación'),
    (1, 'CASTL2', 'Castellano como segunda lengua'),
    (1, 'MAT', 'Matemática'),
    (1, 'CYT', 'Ciencia y Tecnología'),
    (2, 'PS', 'Personal Social'),
    (2, 'EDREL', 'Educación Religiosa'),
    (2, 'EDFIS', 'Educación Física'),
    (2, 'COM', 'Comunicación'),
    (2, 'ARTCUL', 'Arte y Cultura'),
    (2, 'CASTL2', 'Castellano como segunda lengua'),
    (2, 'ING', 'Inglés'),
    (2, 'MAT', 'Matemática'),
    (2, 'CYT', 'Ciencia y Tecnología'),
    (3, 'DPCC', 'Desarrollo Personal, Ciudadanía y Cívica'),
    (3, 'CCSS', 'Ciencias Sociales'),
    (3, 'EDREL', 'Educación Religiosa'),
    (3, 'EPT', 'Educación para el Trabajo'),
    (3, 'EDFIS', 'Educación Física'),
    (3, 'COM', 'Comunicación'),
    (3, 'ARTCUL', 'Arte y Cultura'),
    (3, 'CASTL2', 'Castellano como segunda lengua'),
    (3, 'ING', 'Inglés'),
    (3, 'MAT', 'Matemática'),
    (3, 'CYT', 'Ciencia y Tecnología')
)
INSERT INTO public.areas_curriculares (nivel_id, codigo_area, nombre_area)
SELECT nivel_id, codigo_area, nombre_area FROM areas
ON CONFLICT (nivel_id, codigo_area) DO UPDATE SET nombre_area = EXCLUDED.nombre_area;

WITH competencias(nivel_id, codigo_area, numero_orden, nombre, descripcion) AS (
  VALUES
    (1, 'PS', 1, 'Construye su identidad', 'Reconoce y valora sus características personales y desarrolla autonomía.'),
    (1, 'PS', 2, 'Convive y participa democráticamente', 'Se relaciona con otras personas y participa en acuerdos sencillos.'),
    (1, 'PS', 3, 'Construye identidad como persona humana', 'Desarrolla su identidad espiritual y respeta las creencias de su entorno.'),
    (1, 'PSI', 1, 'Se desenvuelve a través de su motricidad', 'Explora movimientos, equilibrio y coordinación de acuerdo con su edad.'),
    (1, 'COM', 1, 'Se comunica oralmente en su lengua materna', 'Expresa ideas, necesidades y emociones en situaciones cotidianas.'),
    (1, 'COM', 2, 'Lee diversos tipos de textos escritos', 'Interpreta textos de su entorno con acompañamiento.'),
    (1, 'COM', 3, 'Escribe diversos tipos de textos', 'Produce escrituras iniciales con propósito comunicativo.'),
    (1, 'COM', 4, 'Crea proyectos desde los lenguajes del arte', 'Explora y comunica ideas mediante lenguajes artísticos.'),
    (1, 'CASTL2', 1, 'Se comunica oralmente en castellano', 'Se comunica oralmente en castellano como segunda lengua.'),
    (1, 'MAT', 1, 'Resuelve problemas de cantidad', 'Resuelve situaciones de cantidad adecuadas al ciclo inicial.'),
    (1, 'MAT', 2, 'Resuelve problemas de forma y localización', 'Resuelve situaciones de forma, movimiento y localización adecuadas al ciclo inicial.'),
    (1, 'CYT', 1, 'Indaga mediante métodos científicos', 'Explora, pregunta y obtiene información de su entorno para construir conocimientos.'),
    (2, 'PS', 1, 'Construye su identidad', 'Reconoce sus características, emociones y derechos.'),
    (2, 'PS', 2, 'Convive y participa democráticamente', 'Convive, dialoga y participa en acuerdos para el bien común.'),
    (2, 'PS', 3, 'Construye interpretaciones históricas', 'Interpreta cambios y permanencias a partir de fuentes.'),
    (2, 'PS', 4, 'Gestiona el ambiente y el espacio', 'Usa información para cuidar el ambiente y comprender el espacio.'),
    (2, 'PS', 5, 'Gestiona responsablemente los recursos económicos', 'Toma decisiones responsables sobre recursos y consumo.'),
    (2, 'EDREL', 1, 'Construye su identidad como persona humana', 'Reconoce su dignidad, libertad y dimensión espiritual.'),
    (2, 'EDREL', 2, 'Asume experiencias de encuentro con Dios', 'Integra experiencias religiosas a su vida personal y comunitaria.'),
    (2, 'EDFIS', 1, 'Se desenvuelve a través de su motricidad', 'Comprende y desarrolla sus posibilidades motrices.'),
    (2, 'EDFIS', 2, 'Asume una vida saludable', 'Practica hábitos que favorecen el bienestar y la salud.'),
    (2, 'EDFIS', 3, 'Interactúa mediante habilidades sociomotrices', 'Participa en actividades físicas y deportivas con respeto.'),
    (2, 'COM', 1, 'Se comunica oralmente en su lengua materna', 'Comprende y expresa ideas en situaciones comunicativas.'),
    (2, 'COM', 2, 'Lee diversos tipos de textos escritos', 'Comprende textos y reflexiona sobre su contenido.'),
    (2, 'COM', 3, 'Escribe diversos tipos de textos', 'Produce textos adecuados a su propósito y destinatario.'),
    (2, 'ARTCUL', 1, 'Aprecia manifestaciones artístico-culturales', 'Interpreta manifestaciones artísticas de distintos contextos.'),
    (2, 'ARTCUL', 2, 'Crea proyectos desde los lenguajes artísticos', 'Explora y comunica ideas mediante lenguajes artísticos.'),
    (2, 'CASTL2', 1, 'Se comunica oralmente en castellano', 'Interactúa oralmente en castellano como segunda lengua.'),
    (2, 'CASTL2', 2, 'Lee textos en castellano como segunda lengua', 'Comprende textos escritos en castellano como segunda lengua.'),
    (2, 'CASTL2', 3, 'Escribe textos en castellano como segunda lengua', 'Produce textos escritos en castellano como segunda lengua.'),
    (2, 'ING', 1, 'Se comunica oralmente en inglés', 'Comprende y produce mensajes orales en inglés.'),
    (2, 'ING', 2, 'Lee diversos tipos de textos en inglés', 'Comprende textos escritos en inglés.'),
    (2, 'ING', 3, 'Escribe diversos tipos de textos en inglés', 'Produce textos escritos en inglés.'),
    (2, 'MAT', 1, 'Resuelve problemas de cantidad', 'Modela y resuelve situaciones que involucran cantidades.'),
    (2, 'MAT', 2, 'Resuelve problemas de regularidad y cambio', 'Reconoce patrones, equivalencias y relaciones de cambio.'),
    (2, 'MAT', 3, 'Resuelve problemas de forma y localización', 'Modela formas, movimientos y relaciones espaciales.'),
    (2, 'MAT', 4, 'Resuelve problemas de gestión de datos', 'Organiza datos y razona sobre incertidumbre.'),
    (2, 'CYT', 1, 'Indaga mediante métodos científicos', 'Investiga situaciones y construye explicaciones con evidencias.'),
    (2, 'CYT', 2, 'Explica el mundo natural y artificial', 'Explica fenómenos con conocimientos sobre seres vivos y materia.'),
    (2, 'CYT', 3, 'Diseña y construye soluciones tecnológicas', 'Propone y evalúa soluciones a necesidades de su entorno.'),
    (3, 'DPCC', 1, 'Construye su identidad', 'Reflexiona sobre su identidad, emociones y proyecto personal.'),
    (3, 'DPCC', 2, 'Convive y participa democráticamente', 'Delibera y participa en asuntos públicos con enfoque de derechos.'),
    (3, 'CCSS', 1, 'Construye interpretaciones históricas', 'Interpreta procesos históricos usando fuentes diversas.'),
    (3, 'CCSS', 2, 'Gestiona el ambiente y el espacio', 'Explica relaciones entre sociedad, ambiente y territorio.'),
    (3, 'CCSS', 3, 'Gestiona responsablemente los recursos económicos', 'Comprende decisiones económicas y administra recursos.'),
    (3, 'EDREL', 1, 'Construye su identidad como persona humana', 'Reconoce su dignidad, libertad y dimensión trascendente.'),
    (3, 'EDREL', 2, 'Asume experiencias de encuentro con Dios', 'Integra experiencias religiosas a su vida personal y comunitaria.'),
    (3, 'EPT', 1, 'Gestiona proyectos de emprendimiento', 'Crea propuestas de valor y desarrolla proyectos sostenibles.'),
    (3, 'EDFIS', 1, 'Se desenvuelve a través de su motricidad', 'Comprende y desarrolla sus posibilidades motrices.'),
    (3, 'EDFIS', 2, 'Asume una vida saludable', 'Practica hábitos que favorecen el bienestar y la salud.'),
    (3, 'EDFIS', 3, 'Interactúa mediante habilidades sociomotrices', 'Participa en actividades físicas y deportivas con respeto.'),
    (3, 'COM', 1, 'Se comunica oralmente en su lengua materna', 'Comprende y produce textos orales con diversos propósitos.'),
    (3, 'COM', 2, 'Lee diversos tipos de textos escritos', 'Interpreta textos y evalúa sus recursos y perspectivas.'),
    (3, 'COM', 3, 'Escribe diversos tipos de textos', 'Produce textos adecuados a su propósito y destinatario.'),
    (3, 'ARTCUL', 1, 'Aprecia manifestaciones artístico-culturales', 'Analiza manifestaciones artísticas de distintos contextos.'),
    (3, 'ARTCUL', 2, 'Crea proyectos desde los lenguajes artísticos', 'Explora y comunica ideas mediante lenguajes artísticos.'),
    (3, 'CASTL2', 1, 'Se comunica oralmente en castellano', 'Interactúa oralmente en castellano como segunda lengua.'),
    (3, 'CASTL2', 2, 'Lee textos en castellano como segunda lengua', 'Comprende textos escritos en castellano como segunda lengua.'),
    (3, 'CASTL2', 3, 'Escribe textos en castellano como segunda lengua', 'Produce textos escritos en castellano como segunda lengua.'),
    (3, 'ING', 1, 'Se comunica oralmente en inglés', 'Comprende y produce mensajes orales en inglés.'),
    (3, 'ING', 2, 'Lee diversos tipos de textos en inglés', 'Comprende textos escritos en inglés.'),
    (3, 'ING', 3, 'Escribe diversos tipos de textos en inglés', 'Produce textos escritos en inglés.'),
    (3, 'MAT', 1, 'Resuelve problemas de cantidad', 'Modela y resuelve situaciones que involucran cantidades.'),
    (3, 'MAT', 2, 'Resuelve problemas de regularidad y cambio', 'Reconoce patrones, equivalencias y relaciones de cambio.'),
    (3, 'MAT', 3, 'Resuelve problemas de forma y localización', 'Modela formas, movimientos y relaciones espaciales.'),
    (3, 'MAT', 4, 'Resuelve problemas de gestión de datos', 'Organiza datos y razona sobre incertidumbre.'),
    (3, 'CYT', 1, 'Indaga mediante métodos científicos', 'Investiga situaciones y construye explicaciones con evidencias.'),
    (3, 'CYT', 2, 'Explica el mundo natural y artificial', 'Explica fenómenos con conocimientos sobre seres vivos y materia.'),
    (3, 'CYT', 3, 'Diseña y construye soluciones tecnológicas', 'Propone y evalúa soluciones a necesidades de su entorno.')
)
INSERT INTO public.competencias (area_id, numero_orden, nombre_competencia, descripcion)
SELECT area.id, c.numero_orden, c.nombre, c.descripcion
FROM competencias c
JOIN public.areas_curriculares area
  ON area.nivel_id = c.nivel_id AND area.codigo_area = c.codigo_area
WHERE NOT EXISTS (
  SELECT 1 FROM public.competencias existente
  WHERE existente.area_id = area.id
    AND existente.numero_orden = c.numero_orden
);

INSERT INTO public.bloques_horarios (numero_bloque, hora_inicio, hora_fin, es_recreo)
VALUES
  (1, '08:00', '08:45', false),
  (2, '08:45', '09:30', false),
  (3, '09:30', '09:45', true),
  (4, '09:45', '10:30', false),
  (5, '10:30', '11:15', false),
  (6, '11:15', '12:00', false),
  (7, '12:00', '13:00', true),
  (8, '13:00', '13:45', false),
  (9, '13:45', '14:30', false),
  (10, '14:30', '14:45', true),
  (11, '14:45', '15:30', false),
  (12, '15:30', '16:15', false),
  (13, '16:15', '17:00', false)
ON CONFLICT (numero_bloque) DO UPDATE SET
  hora_inicio = EXCLUDED.hora_inicio,
  hora_fin = EXCLUDED.hora_fin,
  es_recreo = EXCLUDED.es_recreo;

COMMIT;
