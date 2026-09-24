import assert from 'node:assert/strict';

const apiBase = process.env.E2E_API_BASE ?? 'http://localhost:8080/api/v1';
const username = process.env.E2E_USERNAME;
const password = process.env.E2E_PASSWORD;
assert.ok(username && password, 'E2E_USERNAME and E2E_PASSWORD are required');
assert.ok(['localhost', '127.0.0.1'].includes(new URL(apiBase).hostname), 'Only local backend is permitted');

const login = await fetch(`${apiBase}/auth/login`, {
  method: 'POST', headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username, password })
});
assert.equal(login.status, 200, 'Authentication failed');
const token = (await login.json()).data.accessToken;
assert.ok(token, 'Missing access token');
const checked = [];
const skipped = [];
async function get(path) {
  const response = await fetch(`${apiBase}${path}`, { headers: { Authorization: `Bearer ${token}` } });
  const text = await response.text();
  assert.equal(response.status, 200, `${path}: ${response.status} ${text.slice(0, 400)}`);
  const parsed = text ? JSON.parse(text) : null;
  checked.push(path);
  return parsed && Object.hasOwn(parsed, 'success') && Object.hasOwn(parsed, 'data') ? parsed.data : parsed;
}

await get('/usuarios/me');
await get('/usuarios');
await get('/usuarios?rol=DIRECCION');
await get('/dashboard/resumen');
const years = await get('/academico/anios');
const levels = await get('/academico/niveles');
await get('/academico/aulas');
const blocks = await get('/curriculo/bloques');
const assignments = await get('/curriculo/asignaciones');
const students = await get('/estudiantes');
const concepts = await get('/tesoreria/conceptos');
const incidents = await get('/convivencia/incidencias');
await get('/convivencia/incidencias/citaciones');
const communications = await get('/comunicados');
const today = new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Lima', year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date());
await get(`/asistencia/aula?fecha=${today}`);
await get(`/asistencia/conciliacion/alertas?fecha=${today}`);

let year;
let period;
let section;
let allSections = [];
if (years.length) {
  year = years.find(item => item.abierto) ?? years[0];
  const periods = await get(`/academico/periodos?anioId=${year.id}`);
  period = periods[0];
  allSections = await get(`/academico/secciones?anioId=${year.id}`);
  section = allSections[0];
  if (section) {
    await get(`/academico/secciones/${section.id}/vacantes`);
    await get(`/matriculas/seccion/${section.id}?page=0&size=20`);
    await get(`/horarios/seccion/${section.id}?anioId=${year.id}`);
  } else skipped.push('section-dependent reads: no section');
  if (period) {
    await get(`/evaluacion/riesgo?periodoId=${period.id}`);
    await get(`/evaluacion/refuerzo?periodoId=${period.id}`);
  } else skipped.push('period-dependent reads: no period');
} else skipped.push('year-dependent reads: no academic year');

let grade;
let area;
for (const level of levels) {
  const grades = await get(`/academico/grados?nivelId=${level.id}`);
  grade ??= grades[0];
  const areas = await get(`/curriculo/areas?nivelId=${level.id}`);
  area ??= areas[0];
}
let competencies = [];
if (area) competencies = await get(`/curriculo/competencias?areaId=${area.id}`);
else skipped.push('competencies: no curricular area');

let enrollment;
if (students.length) {
  const student = students[0];
  await get(`/estudiantes/${student.id}`);
  await get(`/tesoreria/estudiantes/${student.id}/estado-cuenta`);
} else skipped.push('student-dependent reads: no student');
if (allSections.length) {
  for (const candidate of allSections) {
    const enrollments = await get(`/matriculas/seccion/${candidate.id}?page=0&size=100`);
    if (enrollments.length) {
      enrollment = enrollments[0];
      break;
    }
  }
  if (enrollment) {
    const ficha = await get(`/matriculas/${enrollment.id}`);
    await get(`/evaluacion/libreta/${enrollment.id}`);
    await get(`/convivencia/matricula/${enrollment.id}`);
    await get(`/tesoreria/obligaciones?matriculaId=${enrollment.id}`);
    if (ficha.apoderados?.length) await get(`/apoderados/${ficha.apoderados[0].id}`);
    else skipped.push('guardian read: enrollment has no linked guardian');
  } else skipped.push('enrollment-dependent reads: no enrollment');
}

if (assignments.length && year) {
  const assignment = assignments[0];
  await get(`/horarios/docente/${assignment.docenteUsuarioId}?anioId=${year.id}`);
  if (period) {
    await get(`/evaluacion/cneb?asignacionDocenteId=${assignment.id}&periodoAcademicoId=${period.id}`);
    const matchedCompetencies = await get(`/curriculo/competencias?areaId=${assignment.areaCurricularId}`);
    if (matchedCompetencies.length) {
      await get(`/evaluacion/cneb/matriz?asignacionDocenteId=${assignment.id}&periodoAcademicoId=${period.id}&competenciaId=${matchedCompetencies[0].id}`);
    } else skipped.push('grade matrix: no matching competency');
  }
} else skipped.push('assignment-dependent reads: no teaching assignment');

if (communications.length) await get(`/comunicados/${communications[0].id}/metricas`);
else skipped.push('communication metrics: no published communication');

const forbidden = await fetch(`${apiBase}/comunicados/apoderado/bandeja`, { headers: { Authorization: `Bearer ${token}` } });
assert.equal(forbidden.status, 403, 'Admin should not access guardian-only inbox');
const anonymous = await fetch(`${apiBase}/dashboard/resumen`);
assert.ok([401, 403].includes(anonymous.status), 'Anonymous dashboard must be blocked');

console.log(`PASS ${checked.length} real-database GET requests; role restriction 403; anonymous blocked`);
if (skipped.length) console.log(`UNAVAILABLE DATA: ${skipped.join('; ')}`);
