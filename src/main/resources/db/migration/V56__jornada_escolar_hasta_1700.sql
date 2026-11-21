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
