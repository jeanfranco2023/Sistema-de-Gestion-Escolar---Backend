"""DBML del esquema final verificado, con grupos funcionales y relaciones reales."""
from pathlib import Path
from collections import defaultdict
import re

root = Path(__file__).resolve().parents[1]
sql = (root/'src/main/resources/db/schema_completo_colegio_shuji.sql').read_text(encoding='utf-8')
columns = defaultdict(list)
constraints = defaultdict(list)
for line in (root/'target/atributos_bd_v18.tsv').read_text(encoding='utf-8').splitlines():
    table, *column = line.split('\t')
    columns[table].append(column)
for line in (root/'target/restricciones_bd_v18.tsv').read_text(encoding='utf-8').splitlines():
    table, *constraint = line.split('\t', 3)
    constraints[table].append(constraint)

groups = {
    'UsuariosYSeguridad': ['roles', 'usuarios', 'usuario_roles', 'sesiones', 'auditoria_cambios'],
    'OrganizacionAcademica': ['anios_lectivos', 'niveles', 'grados', 'aulas', 'periodos_academicos', 'secciones', 'bloques_horarios', 'horarios_seccion'],
    'CurriculoYDocencia': ['areas_curriculares', 'competencias', 'asignaciones_docentes'],
    'MatriculaYFamilia': ['estudiantes', 'apoderados', 'estudiante_apoderados', 'matriculas', 'solicitudes_matricula_publica', 'solicitudes_matricula_documentos'],
    'EvaluacionYRefuerzo': ['calificaciones_cneb', 'sesiones_refuerzo', 'inscripciones_refuerzo'],
    'Asistencia': ['lotes_biometrico', 'marcas_biometrico_porteria', 'asistencias_aula', 'conciliaciones_asistencia'],
    'Comunicacion': ['comunicados_oficiales', 'comunicado_destinatarios'],
    'Convivencia': ['incidencias_conductuales'],
    'Tesoreria': ['conceptos_cobro', 'obligaciones_pago', 'pagos_transacciones', 'series_comprobante', 'comprobantes_pago'],
}
assert set(columns) == {table for tables in groups.values() for table in tables}

def quoted(text):
    return "'" + text.replace('\\', '\\\\').replace("'", "\\'") + "'"

def list_columns(definition):
    return tuple(x.strip() for x in re.search(r'\(([^)]*)\)', definition).group(1).split(','))

def endpoint(table, names):
    return table + '.' + (names[0] if len(names) == 1 else '(' + ', '.join(names) + ')')

def index_fields(names):
    return names[0] if len(names) == 1 else '(' + ', '.join(names) + ')'

indexes = defaultdict(list)
for unique, name, table, method, remainder in re.findall(
        r'^CREATE (UNIQUE )?INDEX (\w+) ON public\.(\w+) USING (\w+) \((.*);$', sql, re.M):
    depth = 1
    for pos, char in enumerate(remainder):
        if char == '(': depth += 1
        elif char == ')': depth -= 1
        if depth == 0: break
    assert depth == 0
    fields, tail = remainder[:pos], remainder[pos+1:]
    attrs = ['name: ' + quoted(name)]
    if method in ('btree', 'hash'): attrs.append('type: ' + method)
    if unique: attrs.append('unique')
    if tail.strip(): attrs.append('note: ' + quoted('Índice parcial: ' + tail.strip()))
    field_list = [x.strip() for x in fields.split(',')]
    if not all(re.fullmatch(r'\w+', x) for x in field_list):
        # Índices con expresión se conservan como expresión DBML, no como nombres de campos.
        field_list = ['`' + fields + '`']
    indexes[table].append(index_fields(field_list) + ' [' + ', '.join(attrs) + ']')

out = ['// COLEGIO SHUJI KITAMURA — DBML definitivo V18',
       '// Pegar TODO este archivo en el editor DBML de dbdiagram.io.',
       '// 37 tablas; 307 atributos; sin roles.descripcion ni UUID redundantes.',
       '// El SQL definitivo conserva funciones, triggers, RLS y catálogos; este archivo representa el modelo.', '',
       'Project ColegioShujiKitamura {', "  database_type: 'PostgreSQL'",
       "  Note: 'Modelo final V18. Tablas ordenadas por módulos, del contexto institucional a los procesos.'", '}', '']
refs = []
for group, tables in groups.items():
    out += ['// ============================================================', '// ' + group, '// ============================================================', '']
    for table in tables:
        primary = next(list_columns(d) for _, kind, d in constraints[table] if kind == 'p')
        unique_sets = {list_columns(d) for _, kind, d in constraints[table] if kind == 'u'}
        block = re.search(r'CREATE TABLE public\.' + table + r' \((.*?)\n\);', sql, re.S).group(1)
        out.append('Table ' + table + ' {')
        for col, kind, size, nullable, precision, scale in columns[table]:
            datatype = {'character varying': 'varchar', 'character': 'char',
                        'timestamp with time zone': 'timestamptz', 'timestamp without time zone': 'timestamp',
                        'time without time zone': 'time'}.get(kind, kind)
            if size: datatype += '(' + size + ')'
            elif kind == 'numeric': datatype += '(' + precision + ',' + scale + ')'
            attrs = ['not null' if nullable == 'NO' else 'null']
            if primary == (col,): attrs.insert(0, 'pk')
            if re.search(r'ALTER TABLE ONLY public\.' + table + r' ALTER COLUMN ' + col + r' SET DEFAULT nextval', sql):
                attrs.append('increment')
            line = next(x.strip().rstrip(',') for x in block.splitlines() if re.match(r'\s*' + col + r'\s', x))
            if 'GENERATED ALWAYS AS' in line:
                attrs.append('note: ' + quoted('Calculado por PostgreSQL; no se modifica manualmente.'))
            elif ' DEFAULT ' in line:
                default = line.split(' DEFAULT ', 1)[1].removesuffix(' NOT NULL')
                attrs.append('default: ' + (default if default in ('true', 'false') or re.fullmatch(r'-?\d+(\.\d+)?', default) else '`' + default + '`'))
            out.append('  ' + col + ' ' + datatype + ' [' + ', '.join(attrs) + ']')
        definitions = []
        for name, kind, definition in constraints[table]:
            if kind == 'u' or (kind == 'p' and len(primary) > 1):
                definitions.append(index_fields(list_columns(definition)) + ' [' + ('pk' if kind == 'p' else 'unique') + ', name: ' + quoted(name) + ']')
            elif kind == 'f':
                match = re.match(r'FOREIGN KEY \(([^)]*)\) REFERENCES (\w+)\(([^)]*)\)(.*)', definition)
                assert match, definition
                local = tuple(x.strip() for x in match[1].split(','))
                target = tuple(x.strip() for x in match[3].split(','))
                for field in local: assert field in {x[0] for x in columns[table]}
                for field in target: assert field in {x[0] for x in columns[match[2]]}
                actions = []
                for action in ('UPDATE', 'DELETE'):
                    found = re.search(r'ON ' + action + r' (CASCADE|RESTRICT|SET NULL|SET DEFAULT|NO ACTION)', match[4])
                    actions.append(action.lower() + ': ' + (found[1].lower() if found else 'no action'))
                # En relación uno a uno, DBML define la FK en el extremo derecho.
                relation = (endpoint(match[2], target) + ' - ' + endpoint(table, local)
                            if local in unique_sets else endpoint(table, local) + ' > ' + endpoint(match[2], target))
                refs.append('Ref ' + name + ': ' + relation + ' [' + ', '.join(actions) + ']')
        definitions += indexes[table]
        if definitions:
            out += ['', '  indexes {'] + ['    ' + item for item in definitions] + ['  }']
        check_defs = [(name, definition) for name, kind, definition in constraints[table] if kind == 'c']
        if check_defs:
            out += ['', '  checks {']
            for name, definition in check_defs:
                out.append('    `' + definition.removeprefix('CHECK (').removesuffix(')') + '` [name: ' + quoted(name) + ']')
            out += ['  }']
        out += ['}', '']

out += ['// RELACIONES: claves foráneas simples y compuestas del esquema real.', ''] + refs + ['', '// AGRUPACIÓN VISUAL POR MÓDULO.', '']
for group, tables in groups.items():
    out += ['TableGroup ' + group + ' {'] + ['  ' + table for table in tables] + ['}', '']
output = root/'src/main/resources/db/colegio_shuji_v18.dbml'
output.write_text('\n'.join(out), encoding='utf-8')
print(f'DBML generado: {len(columns)} tablas, {sum(map(len, columns.values()))} atributos y {len(refs)} relaciones.')
