"""Produce DDL definitivo desde pg_dump del esquema verificado, sin concatenar versiones."""
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]

def clean_dump(name):
    text = (root/'target'/name).read_text(encoding='utf-8')
    # Comandos de psql y ajustes globales del dump no corresponden al SQL portable.
    text = re.sub(r'^\\(?:un)?restrict[^\n]*\n', '', text, flags=re.M)
    text = re.sub(r'^SET [^\n]*;\n', '', text, flags=re.M)
    text = re.sub(r"^SELECT pg_catalog.set_config\('search_path', '', false\);\n", '', text, flags=re.M)
    text = re.sub(r'^--\n-- (?:Name:[^\n]*|PostgreSQL database dump(?: complete)?)\n--\n', '', text, flags=re.M)
    text = re.sub(r'^-- Dumped (?:from|by)[^\n]*\n', '', text, flags=re.M)
    text = text.replace('CREATE SCHEMA public;', 'CREATE SCHEMA IF NOT EXISTS public;')
    text = re.sub(r'[ \t]+\n', '\n', text)
    return re.sub(r'\n{3,}', '\n\n', text).strip()

schema = clean_dump('esquema-final-v18-original.sql')
# Expresa catálogos enumerados con IN, evitando los casts internos que pg_dump
# imprime y PostgreSQL reinterpreta al restaurar (aunque sean equivalentes).
def enum_in(match):
    values = re.sub(r'::character varying', '', match.group(2))
    return '(' + match.group(1) + ' IN (' + values + '))'
schema = re.sub(r'\(\((\w+)\)::text = ANY \(\(ARRAY\[(.*?)\]\)::text\[\]\)\)', enum_in, schema)
schema = re.sub(r'\(\((\w+)\)::text <> ALL \(\(ARRAY\[(.*?)\]\)::text\[\]\)\)',
                lambda match: enum_in(match).replace(' IN (', ' NOT IN ('), schema)
catalogs = clean_dump('catalogos-final-v18-original.sql')
roles = re.search(r'CREATE TABLE public\.roles \((.*?)\n\);', schema, re.S)
assert roles and 'descripcion' not in roles.group(1)
assert len(re.findall(r'^CREATE TABLE public\.', schema, re.M)) == 37
assert 'flyway_schema_history' not in schema
assert not re.search(r'INSERT INTO public\.(usuarios|estudiantes|apoderados)\b', catalogs)

header = '''-- COLEGIO SHUJI KITAMURA — ESTRUCTURA DEFINITIVA V18 (2026-10-04)
-- PostgreSQL 17 / Supabase. Define directamente el estado final de las 37 tablas.
-- No concatena versiones históricas ni incluye atributos eliminados.
-- roles: id, codigo_rol VARCHAR(20), nombre_rol VARCHAR(30).
-- Solo para crear una base VACÍA o consultar/importar su modelo.
-- No ejecutar sobre la base existente, que ya se encuentra en V18.
-- Sin usuarios de práctica, credenciales, datos personales ni historial Flyway.
-- Si se inicializa una base con este archivo, registrar el baseline V18 en Flyway
-- antes de iniciar el backend; para bases existentes usar las migraciones pendientes.

CREATE SCHEMA IF NOT EXISTS extensions;
CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA extensions;
CREATE EXTENSION IF NOT EXISTS unaccent WITH SCHEMA extensions;
SET search_path TO public, extensions;

-- 1. ESTRUCTURA: funciones, tablas, secuencias, restricciones, índices, triggers y RLS.
'''
# Restituye las revocaciones que --no-privileges omitió, sin depender de roles Supabase.
security = '''
-- 3. PRIVILEGIOS: acceso a través del backend; sin acceso directo de clientes Supabase.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
        REVOKE ALL ON ALL TABLES IN SCHEMA public FROM anon;
        REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM anon;
        REVOKE ALL ON ALL ROUTINES IN SCHEMA public FROM anon;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON TABLES FROM anon;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON SEQUENCES FROM anon;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON ROUTINES FROM anon;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
        REVOKE ALL ON ALL TABLES IN SCHEMA public FROM authenticated;
        REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM authenticated;
        REVOKE ALL ON ALL ROUTINES IN SCHEMA public FROM authenticated;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON TABLES FROM authenticated;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON SEQUENCES FROM authenticated;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON ROUTINES FROM authenticated;
    END IF;
END
$$;
'''
sql = header + schema + '\n\n-- 2. CATÁLOGOS: datos académicos e institucionales; sin cuentas ni expedientes.\n' + catalogs + '\n' + security
for name in ['schema_completo_colegio_shuji.sql', 'schema_completo_importador_postgresql.sql']:
    (root/'src/main/resources/db'/name).write_text(sql, encoding='utf-8')
print('Consolidado definitivo generado: 37 tablas; roles sin descripción; dos archivos idénticos.')
