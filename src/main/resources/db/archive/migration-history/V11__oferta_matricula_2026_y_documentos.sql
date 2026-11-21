-- Oferta 2026 necesaria para el flujo público de admisión. Inserciones idempotentes.
INSERT INTO anios_lectivos (anio, fecha_inicio, fecha_fin, abierto)
VALUES (2026, DATE '2026-03-01', DATE '2026-12-20', TRUE)
ON CONFLICT (anio) DO NOTHING;

INSERT INTO periodos_academicos (anio_lectivo_id, numero_periodo, nombre, fecha_inicio, fecha_fin, cerrado)
SELECT a.id, p.numero, p.nombre, p.inicio, p.fin, FALSE
FROM anios_lectivos a
CROSS JOIN (VALUES
  (1::SMALLINT, 'I Bimestre', DATE '2026-03-01', DATE '2026-05-08'),
  (2::SMALLINT, 'II Bimestre', DATE '2026-05-18', DATE '2026-07-24'),
  (3::SMALLINT, 'III Bimestre', DATE '2026-08-10', DATE '2026-10-09'),
  (4::SMALLINT, 'IV Bimestre', DATE '2026-10-19', DATE '2026-12-18')
) AS p(numero, nombre, inicio, fin)
WHERE a.anio = 2026
ON CONFLICT (anio_lectivo_id, numero_periodo) DO NOTHING;

INSERT INTO grados (nivel_id, numero_grado, nombre)
SELECT n.id, g.numero, g.nombre
FROM (VALUES
  ('INICIAL', 1::SMALLINT, 'Inicial 3 años'),
  ('INICIAL', 2::SMALLINT, 'Inicial 4 años'),
  ('INICIAL', 3::SMALLINT, 'Inicial 5 años'),
  ('PRIMARIA', 1::SMALLINT, '1° de Primaria'),
  ('PRIMARIA', 2::SMALLINT, '2° de Primaria'),
  ('PRIMARIA', 3::SMALLINT, '3° de Primaria'),
  ('PRIMARIA', 4::SMALLINT, '4° de Primaria'),
  ('PRIMARIA', 5::SMALLINT, '5° de Primaria'),
  ('PRIMARIA', 6::SMALLINT, '6° de Primaria'),
  ('SECUNDARIA', 1::SMALLINT, '1° de Secundaria'),
  ('SECUNDARIA', 2::SMALLINT, '2° de Secundaria'),
  ('SECUNDARIA', 3::SMALLINT, '3° de Secundaria'),
  ('SECUNDARIA', 4::SMALLINT, '4° de Secundaria'),
  ('SECUNDARIA', 5::SMALLINT, '5° de Secundaria')
) AS g(codigo, numero, nombre)
JOIN niveles n ON n.codigo = g.codigo
ON CONFLICT (nivel_id, numero_grado) DO NOTHING;

INSERT INTO aulas (codigo, nombre, ubicacion, capacidad, activa)
VALUES
  ('AULA_I_01', 'Pabellón Inicial - Aula 1', 'Pabellón A - Piso 1', 30, TRUE),
  ('AULA_I_02', 'Pabellón Inicial - Aula 2', 'Pabellón A - Piso 1', 30, TRUE),
  ('AULA_I_03', 'Pabellón Inicial - Aula 3', 'Pabellón A - Piso 1', 30, TRUE),
  ('AULA_P_101', 'Pabellón Primaria - Aula 101', 'Pabellón B - Piso 1', 35, TRUE),
  ('AULA_P_102', 'Pabellón Primaria - Aula 102', 'Pabellón B - Piso 1', 35, TRUE),
  ('AULA_P_103', 'Pabellón Primaria - Aula 103', 'Pabellón B - Piso 2', 35, TRUE),
  ('AULA_P_104', 'Pabellón Primaria - Aula 104', 'Pabellón B - Piso 2', 35, TRUE),
  ('AULA_P_105', 'Pabellón Primaria - Aula 105', 'Pabellón B - Piso 3', 35, TRUE),
  ('AULA_P_106', 'Pabellón Primaria - Aula 106', 'Pabellón B - Piso 3', 35, TRUE),
  ('AULA_S_201', 'Pabellón Secundaria - Aula 201', 'Pabellón C - Piso 1', 35, TRUE),
  ('AULA_S_202', 'Pabellón Secundaria - Aula 202', 'Pabellón C - Piso 1', 35, TRUE),
  ('AULA_S_203', 'Pabellón Secundaria - Aula 203', 'Pabellón C - Piso 2', 35, TRUE),
  ('AULA_S_204', 'Pabellón Secundaria - Aula 204', 'Pabellón C - Piso 2', 35, TRUE),
  ('AULA_S_205', 'Pabellón Secundaria - Aula 205', 'Pabellón C - Piso 3', 35, TRUE)
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO secciones (anio_lectivo_id, grado_id, nivel_id, letra, cupo_maximo,
                       vacantes_ocupadas, aula_fisica, aula_id)
SELECT anio.id, grado.id, nivel.id, 'A',
       CASE WHEN nivel.codigo = 'INICIAL' THEN 30 ELSE 35 END,
       0, aulas.codigo, aulas.id
FROM (VALUES
  ('INICIAL', 1::SMALLINT, 'AULA_I_01'),
  ('INICIAL', 2::SMALLINT, 'AULA_I_02'),
  ('INICIAL', 3::SMALLINT, 'AULA_I_03'),
  ('PRIMARIA', 1::SMALLINT, 'AULA_P_101'),
  ('PRIMARIA', 2::SMALLINT, 'AULA_P_102'),
  ('PRIMARIA', 3::SMALLINT, 'AULA_P_103'),
  ('PRIMARIA', 4::SMALLINT, 'AULA_P_104'),
  ('PRIMARIA', 5::SMALLINT, 'AULA_P_105'),
  ('PRIMARIA', 6::SMALLINT, 'AULA_P_106'),
  ('SECUNDARIA', 1::SMALLINT, 'AULA_S_201'),
  ('SECUNDARIA', 2::SMALLINT, 'AULA_S_202'),
  ('SECUNDARIA', 3::SMALLINT, 'AULA_S_203'),
  ('SECUNDARIA', 4::SMALLINT, 'AULA_S_204'),
  ('SECUNDARIA', 5::SMALLINT, 'AULA_S_205')
) AS oferta(codigo_nivel, numero_grado, codigo_aula)
JOIN anios_lectivos anio ON anio.anio = 2026 AND anio.abierto IS TRUE
JOIN niveles nivel ON nivel.codigo = oferta.codigo_nivel
JOIN grados grado ON grado.nivel_id = nivel.id AND grado.numero_grado = oferta.numero_grado
JOIN aulas ON aulas.codigo = oferta.codigo_aula AND aulas.activa IS TRUE
ON CONFLICT (anio_lectivo_id, grado_id, letra) DO NOTHING;

-- Solo se persisten referencias y metadatos; los archivos viven en un bucket privado de Supabase Storage.
CREATE TABLE IF NOT EXISTS solicitudes_matricula_documentos (
    solicitud_id UUID NOT NULL REFERENCES solicitudes_matricula_publica(id) ON DELETE CASCADE,
    tipo VARCHAR(30) NOT NULL CHECK (tipo IN ('PARTIDA_NACIMIENTO', 'DNI_C4', 'RECIBO_SERVICIO')),
    bucket VARCHAR(100) NOT NULL,
    object_key VARCHAR(500) NOT NULL,
    mime_type VARCHAR(100) NOT NULL CHECK (mime_type IN ('application/pdf', 'image/jpeg', 'image/png', 'image/webp')),
    tamano_bytes BIGINT NOT NULL CHECK (tamano_bytes BETWEEN 1 AND 5242880),
    sha256 CHAR(64) NOT NULL,
    actualizado_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (solicitud_id, tipo),
    UNIQUE (bucket, object_key)
);
ALTER TABLE solicitudes_matricula_documentos ENABLE ROW LEVEL SECURITY;
