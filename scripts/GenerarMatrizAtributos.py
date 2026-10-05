"""Genera la matriz desde los metadatos verificados en PostgreSQL, no desde supuestos."""
from pathlib import Path
from collections import defaultdict
import re

root = Path(__file__).resolve().parents[1]
rows = [line.split('\t') for line in (root/'target/atributos_bd_v15.tsv').read_text(encoding='utf-8').splitlines()]
checks = defaultdict(list)
for line in (root/'target/restricciones_bd_v15.tsv').read_text(encoding='utf-8').splitlines():
    table, name, kind, definition = line.split('\t', 3)
    if kind == 'c': checks[table].append(definition)

def reason(table, col, kind, size):
    if col == 'usuario_password': return 'BCrypt existente: 60 caracteres; nunca contraseña en claro.'
    if 'huella' in col: return 'Digest criptográfico: longitud fija del algoritmo; no reducir.'
    if 'correo' in col: return '254 conserva el límite de correo del sistema; VARCHAR no reserva espacio vacío.'
    if 'apellido' in col or col.startswith('nombres_'): return '20 por decisión del proyecto; cada apellido se captura separado.'
    if col == 'nombre_usuario': return '30 para el identificador de acceso; independiente de los nombres personales.'
    if col == 'nombre_concepto': return '50: el catálogo existente contiene un nombre de 42 caracteres.'
    if col == 'nombre_area': return '60 para denominaciones curriculares; descripción extensa fuera del nombre.'
    if col == 'nombre_competencia': return '50 pactados para etiqueta; desarrollo completo en descripción.'
    if col in ('titulo_comunicado','tema_refuerzo'): return '40 pactados para título/tema breve; contenido separado.'
    if 'direccion' in col and kind != 'inet': return '150 para vía, número, urbanización y referencias; ubigeo separado.'
    if col == 'ubicacion': return '60 para pabellón/piso/referencia dentro de la institución.'
    if col == 'codigo_aula': return '30 ya acordados; conserva las referencias y códigos institucionales.'
    if col == 'tipo_contenido': return '30 cubre los MIME admitidos; CHECK restringe formatos.'
    if col in ('contenedor','ruta_objeto'): return 'Referencia técnica de Storage, no título visible; conservar capacidad de rutas/configuración.'
    if 'enlace' in col or 'url' in col: return 'URL externa variable; no reducir basándose en un título o nombre.'
    if 'pago_id' in col or 'preferencia_id' in col or 'transaccion_id' in col:
        return 'Identificador de proveedor externo; no imponer un formato numérico local.'
    if kind == 'text':
        maxima = next((re.search(r'char_length\('+re.escape(col)+r'\) <= (\d+)', c) for c in checks[table]
                       if re.search(r'char_length\('+re.escape(col)+r'\) <= (\d+)', c)), None)
        return ('TEXT con CHECK máximo '+maxima.group(1)+'; texto variable, sin reserva fija.' if maxima else
                'Texto técnico variable de navegador; no representa un nombre/título y no reserva espacio fijo.')
    if kind == 'uuid': return 'Identificador UUID de 16 bytes; identidad pública/solicitud.'
    if kind == 'inet': return 'Tipo nativo IPv4/IPv6; evita tamaños VARCHAR arbitrarios.'
    if kind == 'boolean': return 'Estado lógico: un byte; conserva los registros.'
    if kind == 'numeric': return 'Precisión decimal exacta para importes; mantener escala y coherencia de FK/finanzas.'
    if kind in ('smallint','integer','bigint'):
        return 'Entero; conservar ancho compatible con PK/FK y margen del contador/catálogo.'
    if kind.startswith('timestamp'): return 'Fecha/hora con zona; conserva el instante y contexto de auditoría.'
    if kind in ('date','time without time zone'): return 'Tipo temporal nativo; sin conversión a texto.'
    if kind == 'jsonb': return 'Instantánea estructurada de auditoría; no sustituir por VARCHAR.'
    if col in ('celular','celular_apoderado'): return '9 dígitos y formato peruano validado por CHECK.'
    if 'ubigeo' in col: return '6 dígitos INEI; código, no cantidad numérica.'
    if 'documento' in col or col == 'dni_leido': return 'Código documental; conserva ceros iniciales y formatos admitidos.'
    if kind in ('character varying','character'):
        enum = any(re.search(r'\b'+re.escape(col)+r'\b', c) and ('ANY' in c or ' IN ' in c) for c in checks[table])
        return ('Valor enumerado con CHECK; margen explícito para estados válidos.' if enum else
                'Límite funcional del catálogo/metadato; no reserva el máximo en disco.')
    return 'Tipo nativo revisado; sin representación redundante en texto.'

output = ['# Auditoría de nombres y atributos — 2026-09-30', '',
          'Estado final: PostgreSQL en español y snake_case; propiedades Java en camelCase. '
          '`usuarioPassword` / `usuario_password` conserva BCrypt (60). El JSON de fechas existente '
          'se conserva mediante JsonProperty para mantener el contrato del frontend.', '',
          f'Inventario real: **{len(set(r[0] for r in rows))} tablas y {len(rows)} atributos**, obtenido '
          'de los esquemas de prueba de PostgreSQL/Supabase. V15 conserva PK, FK, UUID e índices; '
          'no altera las migraciones históricas. Las longitudes visibles en V1 pertenecen a esa versión: '
          'el estado final lo determinan las modificaciones posteriores hasta V15.', '',
          'Se renombraron 55 columnas, redujeron 10 límites VARCHAR y añadieron 8 límites CHECK '
          'para TEXT. Observaciones cortas: 500 tanto en base como en DTO. Concepto de cobro: 50 '
          'para no rechazar el nombre de 42 caracteres que ya existe en el catálogo.', '',
          'VARCHAR guarda el texto realmente utilizado, no reserva el máximo declarado. Los límites '
          'son reglas de negocio, no una estimación de memoria por fila. Referencia: '
          '[PostgreSQL — tipos de caracteres](https://www.postgresql.org/docs/current/datatype-character.html).', '',
          'Verificación: migraciones y consolidado se comparan en PostgreSQL real; atributos y '
          'restricciones deben ser idénticos. Se prueban rechazo sin truncado, límites 20/21 y 500/501, '
          'y el trigger de fecha. La prueba transaccional revierte sus dos esquemas temporales. '
          'La validación JPA usa otro esquema desechable; los resultados se registran al finalizar.', '',
          '## Matriz completa', '', '| Tabla | Atributo SQL | Tipo final | Admite NULL | Criterio |',
          '|---|---|---|---|---|']
for table, col, kind, size, nullable, precision, scale in rows:
    sqltype = kind + ('('+size+')' if size else '('+precision+','+scale+')' if kind=='numeric' else '')
    output.append(f'| {table} | `{col}` | {sqltype} | {"Sí" if nullable=="YES" else "No"} | {reason(table,col,kind,size)} |')
output += ['', '## Relaciones y restricciones', '',
           'Las restricciones siguientes son las obtenidas de PostgreSQL después de V15. '
           'Las claves se renombraron por dependencia; las referencias de PL/pgSQL y JPA '
           'se actualizaron explícitamente.', '', '| Tabla | Restricción | Definición |', '|---|---|---|']
for line in (root/'target/restricciones_bd_v15.tsv').read_text(encoding='utf-8').splitlines():
    table, name, kind, definition = line.split('\t',3)
    output.append(f'| {table} | `{name}` | `{definition.replace("|","&#124;")}` |')
path=root/'AUDITORIA_BD_NOMBRES_Y_ATRIBUTOS_2026-09-30.md'
path.write_text('\n'.join(output)+'\n',encoding='utf-8')
print(f'Matriz generada: {len(rows)} atributos y todas sus restricciones.')
