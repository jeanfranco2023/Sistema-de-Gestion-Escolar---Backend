-- V62: Permitir reasignar docente_usuario_id en asignaciones_docentes actualizando en cascada
-- las FKs compuestas en horarios_seccion y calificaciones_cneb.

ALTER TABLE public.horarios_seccion
  DROP CONSTRAINT IF EXISTS horarios_seccion_asignacion_docente_id_anio_lectivo_id_sec_fkey;

ALTER TABLE public.horarios_seccion
  ADD CONSTRAINT horarios_seccion_asignacion_docente_id_anio_lectivo_id_sec_fkey
  FOREIGN KEY (asignacion_docente_id, anio_lectivo_id, seccion_id, docente_usuario_id)
  REFERENCES public.asignaciones_docentes(id, anio_lectivo_id, seccion_id, docente_usuario_id)
  ON UPDATE CASCADE
  ON DELETE RESTRICT;

ALTER TABLE public.calificaciones_cneb
  DROP CONSTRAINT IF EXISTS calificaciones_cneb_asignacion_docente_id_seccion_id_area__fkey;

ALTER TABLE public.calificaciones_cneb
  ADD CONSTRAINT calificaciones_cneb_asignacion_docente_id_seccion_id_area__fkey
  FOREIGN KEY (asignacion_docente_id, seccion_id, area_curricular_id, anio_lectivo_id, docente_usuario_id)
  REFERENCES public.asignaciones_docentes(id, seccion_id, area_curricular_id, anio_lectivo_id, docente_usuario_id)
  ON UPDATE CASCADE
  ON DELETE RESTRICT;
