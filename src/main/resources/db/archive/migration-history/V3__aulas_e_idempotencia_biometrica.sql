-- Catálogo institucional de ambientes físicos sin cambiar el contrato actual de secciones.
CREATE TABLE aulas (
    id SERIAL PRIMARY KEY,
    codigo VARCHAR(30) UNIQUE NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    ubicacion VARCHAR(150),
    capacidad SMALLINT NOT NULL CHECK (capacidad > 0),
    activa BOOLEAN DEFAULT TRUE NOT NULL
);

UPDATE secciones
SET aula_fisica = 'SIN-ASIGNAR-' || id
WHERE aula_fisica IS NULL OR btrim(aula_fisica) = '';

INSERT INTO aulas (codigo, nombre, capacidad)
SELECT upper(btrim(s.aula_fisica)),
       'Aula ' || upper(btrim(s.aula_fisica)),
       max(s.cupo_maximo)
FROM secciones s
GROUP BY upper(btrim(s.aula_fisica))
ON CONFLICT (codigo) DO NOTHING;

ALTER TABLE secciones ADD COLUMN aula_id INT;

UPDATE secciones s
SET aula_id = a.id,
    aula_fisica = a.codigo
FROM aulas a
WHERE a.codigo = upper(btrim(s.aula_fisica));

ALTER TABLE secciones ALTER COLUMN aula_id SET NOT NULL;
ALTER TABLE secciones
    ADD CONSTRAINT fk_secciones_aula
    FOREIGN KEY (aula_id) REFERENCES aulas(id) ON DELETE RESTRICT;
CREATE UNIQUE INDEX uq_secciones_anio_aula_id ON secciones (anio_lectivo_id, aula_id);

-- Identidad estable del contenido para impedir reprocesar el mismo lote.
ALTER TABLE lotes_biometrico ADD COLUMN hash_contenido VARCHAR(64);
UPDATE lotes_biometrico
SET hash_contenido = encode(
    digest(id::text || '|' || nombre_archivo || '|' || created_at::text, 'sha256'),
    'hex');
ALTER TABLE lotes_biometrico ALTER COLUMN hash_contenido SET NOT NULL;
ALTER TABLE lotes_biometrico
    ADD CONSTRAINT uq_lotes_biometrico_hash_contenido UNIQUE (hash_contenido);

UPDATE marcas_biometrico_porteria
SET dispositivo_codigo = 'PORTERIA_01'
WHERE dispositivo_codigo IS NULL OR btrim(dispositivo_codigo) = '';
ALTER TABLE marcas_biometrico_porteria ALTER COLUMN dispositivo_codigo SET NOT NULL;

WITH repetidas AS (
    SELECT id,
           row_number() OVER (
               PARTITION BY dni_leido, fecha_hora, dispositivo_codigo
               ORDER BY id) AS numero
    FROM marcas_biometrico_porteria
    WHERE estado_procesamiento <> 'DUPLICADO'
)
UPDATE marcas_biometrico_porteria m
SET estado_procesamiento = 'DUPLICADO'
FROM repetidas r
WHERE m.id = r.id AND r.numero > 1;

CREATE UNIQUE INDEX uq_marcas_porteria_evento_valido
ON marcas_biometrico_porteria (dni_leido, fecha_hora, dispositivo_codigo)
WHERE estado_procesamiento <> 'DUPLICADO';

-- La Data API no debe exponer el nuevo catálogo; el acceso continúa por el backend.
ALTER TABLE aulas ENABLE ROW LEVEL SECURITY;
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
        REVOKE ALL ON aulas FROM anon;
        REVOKE ALL ON SEQUENCE aulas_id_seq FROM anon;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
        REVOKE ALL ON aulas FROM authenticated;
        REVOKE ALL ON SEQUENCE aulas_id_seq FROM authenticated;
    END IF;
END
$$;
