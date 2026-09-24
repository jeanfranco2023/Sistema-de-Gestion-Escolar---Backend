-- V12: ajustar límites de texto, controlar períodos y vincular refuerzos con aulas.
-- No elimina ni trunca filas: aborta si datos actuales superan los nuevos límites.

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM apoderados WHERE char_length(apellido_paterno) > 20 OR char_length(apellido_materno) > 20)
       OR EXISTS (SELECT 1 FROM estudiantes WHERE char_length(apellido_paterno) > 20 OR char_length(apellido_materno) > 20)
       OR EXISTS (SELECT 1 FROM solicitudes_matricula_publica
                  WHERE char_length(apellido_paterno_estudiante) > 20
                     OR char_length(apellido_materno_estudiante) > 20
                     OR char_length(apellido_paterno_apoderado) > 20
                     OR char_length(apellido_materno_apoderado) > 20) THEN
        RAISE EXCEPTION 'V12 cancelada: existen apellidos de más de 20 caracteres. Revise longitudes antes de migrar; no se truncó ningún dato.';
    END IF;

    IF EXISTS (SELECT 1 FROM solicitudes_matricula_publica
               WHERE char_length(nombres_estudiante) > 20 OR char_length(nombres_apoderado) > 20) THEN
        RAISE EXCEPTION 'V12 cancelada: existen nombres de solicitud pública de más de 20 caracteres. Revise los datos antes de migrar; no se truncó ningún dato.';
    END IF;

    IF EXISTS (SELECT 1 FROM competencias WHERE char_length(nombre) > 50)
       OR EXISTS (SELECT 1 FROM comunicados_oficiales WHERE char_length(titulo) > 40)
       OR EXISTS (SELECT 1 FROM sesiones_refuerzo WHERE char_length(tema) > 40)
       OR EXISTS (SELECT 1 FROM roles WHERE char_length(nombre) > 30)
       OR EXISTS (SELECT 1 FROM niveles WHERE char_length(nombre) > 30)
       OR EXISTS (SELECT 1 FROM grados WHERE char_length(nombre) > 30)
       OR EXISTS (SELECT 1 FROM aulas WHERE char_length(nombre) > 40) THEN
        RAISE EXCEPTION 'V12 cancelada: hay textos que exceden los nuevos límites. Revise los valores antes de migrar; no se truncó ningún dato.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM sesiones_refuerzo s
        WHERE s.aula_asignada IS NOT NULL
          AND NOT EXISTS (
              SELECT 1 FROM aulas a
              WHERE lower(btrim(a.codigo)) = lower(btrim(s.aula_asignada))
          )
    ) THEN
        RAISE EXCEPTION 'V12 cancelada: aula_asignada contiene valores sin aula correspondiente. Cree o corrija el aula antes de migrar; no se eliminó ningún dato.';
    END IF;

    IF EXISTS (
        SELECT s.id
        FROM sesiones_refuerzo s
        JOIN aulas a ON lower(btrim(a.codigo)) = lower(btrim(s.aula_asignada))
        WHERE s.aula_asignada IS NOT NULL
        GROUP BY s.id
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'V12 cancelada: un aula_asignada coincide con más de un código de aula al normalizar mayúsculas y espacios. Resuelva la ambigüedad antes de migrar.';
    END IF;
END
$$;

-- Normaliza únicamente espacios/mayúsculas en claves de aula que ya existen.
UPDATE sesiones_refuerzo s
SET aula_asignada = a.codigo
FROM aulas a
WHERE s.aula_asignada IS NOT NULL
  AND lower(btrim(a.codigo)) = lower(btrim(s.aula_asignada))
  AND s.aula_asignada <> a.codigo;

ALTER TABLE roles
    ALTER COLUMN nombre TYPE VARCHAR(30);
ALTER TABLE competencias
    ALTER COLUMN nombre TYPE VARCHAR(50);
ALTER TABLE comunicados_oficiales
    ALTER COLUMN titulo TYPE VARCHAR(40);
ALTER TABLE sesiones_refuerzo
    ALTER COLUMN tema TYPE VARCHAR(40);
ALTER TABLE apoderados
    ALTER COLUMN apellido_paterno TYPE VARCHAR(20),
    ALTER COLUMN apellido_materno TYPE VARCHAR(20),
    ALTER COLUMN email TYPE VARCHAR(254);
ALTER TABLE estudiantes
    ALTER COLUMN apellido_paterno TYPE VARCHAR(20),
    ALTER COLUMN apellido_materno TYPE VARCHAR(20);
ALTER TABLE solicitudes_matricula_publica
    ALTER COLUMN nombres_estudiante TYPE VARCHAR(20),
    ALTER COLUMN apellido_paterno_estudiante TYPE VARCHAR(20),
    ALTER COLUMN apellido_materno_estudiante TYPE VARCHAR(20),
    ALTER COLUMN nombres_apoderado TYPE VARCHAR(20),
    ALTER COLUMN apellido_paterno_apoderado TYPE VARCHAR(20),
    ALTER COLUMN apellido_materno_apoderado TYPE VARCHAR(20),
    ALTER COLUMN email_apoderado TYPE VARCHAR(254);
ALTER TABLE usuarios
    ALTER COLUMN email TYPE VARCHAR(254);
ALTER TABLE niveles
    ALTER COLUMN nombre TYPE VARCHAR(30);
ALTER TABLE grados
    ALTER COLUMN nombre TYPE VARCHAR(30);
ALTER TABLE aulas
    ALTER COLUMN nombre TYPE VARCHAR(40);

-- El período puede estar inactivo sin borrar su fila ni su historial académico.
-- cerrado conserva su significado independiente: bloqueo de edición de notas.
ALTER TABLE periodos_academicos
    ADD COLUMN activo BOOLEAN NOT NULL DEFAULT TRUE;

-- aula_asignada conserva el contrato actual (código de aula) y ahora referencia
-- la clave única aulas.codigo. RESTRICT protege referencias históricas.
ALTER TABLE sesiones_refuerzo
    ADD CONSTRAINT fk_sesiones_refuerzo_aula_codigo
    FOREIGN KEY (aula_asignada) REFERENCES aulas(codigo)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

CREATE INDEX idx_sesiones_refuerzo_aula_asignada
    ON sesiones_refuerzo (aula_asignada);
