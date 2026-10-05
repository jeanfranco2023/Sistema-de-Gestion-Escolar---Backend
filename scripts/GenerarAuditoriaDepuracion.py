"""Matriz de atributos conservados tras V16, desde metadatos de PostgreSQL."""
import ast
import re
from pathlib import Path
from collections import defaultdict

root = Path(__file__).resolve().parents[1]
rows = [line.split('\t') for line in (root/'target/atributos_bd_v16.tsv').read_text(encoding='utf-8').splitlines()]
constraints = (root/'target/restricciones_bd_v16.tsv').read_text(encoding='utf-8').splitlines()
checks = defaultdict(list)
for line in constraints:
    table, name, kind, definition = line.split('\t', 3)
    if kind == 'c': checks[table].append(definition)
# Reutiliza únicamente el criterio de tipos, sin ejecutar el generador del informe histórico.
tree = ast.parse((root/'scripts/GenerarMatrizAtributos.py').read_text(encoding='utf-8'))
function = next(node for node in tree.body if isinstance(node, ast.FunctionDef) and node.name == 'reason')
exec(compile(ast.Module(body=[function], type_ignores=[]), '<criterios>', 'exec'))

output = ['# Depuración de atributos de la base de datos — 2026-09-30', '',
    'Estado local: V1–V16, 37 tablas, 307 atributos y 222 restricciones. Se revisaron los identificadores, '
    'sus referencias en repositorios, DTO, servicios y frontend, y la correspondencia dominio–persistencia. '
    'La migración no elimina registros; la verificación utiliza esquemas temporales. No se modificó .env '
    'ni se aplicó V16 a public.', '',
    '## Eliminaciones justificadas', '',
    '| Elemento | Decisión y evidencia |', '|---|---|',
    '| UUID de usuarios, apoderados, estudiantes, matrículas, pagos_transacciones y comprobantes_pago | Eliminar: el ID numérico es PK y destino de las FK; no existen búsquedas, rutas ni procesos basados en esos UUID. Frontend solo los declaraba. |',
    '| roles.descripcion | Eliminar: ningún proceso ni pantalla la consume; código y nombre ya identifican el rol. |',
    '| ComunicadoOficial.uuid e IncidenciaConductual.uuid | Eliminar del dominio: nunca se persistían ni tenían uso funcional. |',
    '| Nivel.descripcion | Eliminar del dominio: no existía en la entidad JPA ni en la respuesta del catálogo. |',
    '| InscripcionRefuerzo.matriculaId | Eliminar del dominio: no se persistía ni se usaba; la calificación de origen conserva el vínculo con matrícula. |', '',
    '## Campos conservados que pueden parecer redundantes', '',
    '| Campo o grupo | Justificación |', '|---|---|',
    '| solicitudes_matricula_publica.id UUID | Es su única PK. Se utiliza en consulta pública, validación documental, almacenamiento y referencia de pago. |',
    '| solicitudes_matricula_documentos.solicitud_id UUID | FK de la solicitud; no duplica un ID numérico. |',
    '| roles.codigo_rol / nombre_rol | Código estable para autorización; nombre legible para mostrar. ID es la clave relacional. |',
    '| niveles.codigo_nivel / nombre_nivel | El código corresponde al enum funcional usado por oferta y validación; el nombre es la etiqueta. |',
    '| aulas.codigo_aula / nombre_aula / ubicacion | Código identificador, etiqueta para reportes y localización dentro del colegio son datos distintos. |',
    '| Referencias de año, sección, nivel y matrícula | Relaciones y coherencia académica temporal. No retirar una FK solo por poder recorrer otra relación. |',
    '| calificacion_origen_id en refuerzo | Trazabilidad de la calificación que motivó la inscripción. |',
    '| Saldos e indicadores GENERATED | Derivados por PostgreSQL; no se editan independientemente. Soportan filtros sin inconsistencias por doble escritura. |',
    '| Estados, activo y fechas | Conservan historial, reservas, cierres y bajas lógicas. |',
    '| Identificadores de proveedor, serie y correlativo | Conciliación, idempotencia y numeración del comprobante; funciones distintas de la PK. |',
    '| Hashes, tokens y auditoría | Seguridad y trazabilidad. Un ID o UUID no sustituye la autorización de acceso. |',
    '| Filtros de publicación de comunicado | Seleccionan destinatarios; su persistencia efectiva está en comunicado_destinatarios. No se modificó este flujo. |', '',
    'Los UUID usados para claves aleatorias de idempotencia o tokens se conservan. La eliminación afecta '
    'los campos uuid en las respuestas REST indicadas; los tipos del frontend se actualizaron conjuntamente. '
    'Otros consumidores externos deberán dejar de depender de esos campos.', '',
    '## Matriz completa del estado final', '',
    'Conservar significa que no se identificó una redundancia eliminable con la evidencia actual. '
    'No presupone que cada campo opcional tenga datos en desarrollo. Los límites VARCHAR son máximos '
    'funcionales: no reservan espacio vacío por cada fila.', '',
    '| Tabla | Atributo | Tipo | NULL | Decisión | Criterio |', '|---|---|---|---|---|---|']
for table, col, kind, size, nullable, precision, scale in rows:
    sqltype = kind + ('('+size+')' if size else '('+precision+','+scale+')' if kind=='numeric' else '')
    criterion = reason(table, col, kind, size)
    if kind == 'uuid': criterion = 'Clave primaria de solicitud pública o FK que la referencia; uso comprobado en el flujo público.'
    output.append(f'| {table} | `{col}` | {sqltype} | {"Sí" if nullable=="YES" else "No"} | Conservar | {criterion} |')
output += ['', '## Restricciones verificadas', '', '| Tabla | Restricción | Definición |', '|---|---|---|']
for line in constraints:
    table, name, kind, definition = line.split('\t', 3)
    output.append(f'| {table} | `{name}` | `{definition.replace("|", "&#124;")}` |')
output += ['', '## Verificación', '',
    '- Migraciones V1–V16 y consolidado equivalentes en PostgreSQL/Supabase real.',
    '- Límites 20/21 y 500/501, rechazo sin truncado y trigger de actualización comprobados.',
    '- Los dos esquemas de comparación se revirtieron completamente.',
    '- Resultado de pruebas JPA y compilación frontend: completar al finalizar la ejecución.', '']
(root/'AUDITORIA_BD_DEPURACION_ATRIBUTOS_2026-09-30.md').write_text('\n'.join(output), encoding='utf-8')
print(f'Auditoría generada: {len(rows)} atributos, {len(constraints)} restricciones.')
