import assert from 'node:assert/strict';
import puppeteer from 'puppeteer-core';

const apiBase = process.env.E2E_API_BASE;
const username = process.env.E2E_USERNAME;
const password = process.env.E2E_PASSWORD;
assert.ok(apiBase && username && password, 'E2E_API_BASE, E2E_USERNAME and E2E_PASSWORD are required');
const target = new URL(apiBase);
assert.ok(['localhost', '127.0.0.1'].includes(target.hostname) && target.port === '18080', 'This write test only targets the isolated backend on port 18080');

const runId = Math.random().toString(36).slice(2, 10);
const date = '2026-09-20';
let adminToken;
let guardianToken;
let checks = 0;

async function request(method, path, body, token = adminToken, expected = [200, 201]) {
  const response = await fetch(`${apiBase}${path}`, {
    method,
    headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}), ...(body === undefined ? {} : { 'Content-Type': 'application/json' }) },
    ...(body === undefined ? {} : { body: JSON.stringify(body) })
  });
  const raw = await response.text();
  const result = raw ? JSON.parse(raw) : null;
  assert.ok(expected.includes(response.status), `${method} ${path} returned ${response.status}: ${raw.slice(0, 500)}`);
  checks++;
  return result?.data ?? result;
}

function one(items, predicate, label) {
  const item = items.find(predicate);
  assert.ok(item, `${label} not found`);
  return item;
}

const login = await request('POST', '/auth/login', { username, password }, null);
adminToken = login.accessToken;
assert.ok(adminToken, 'Admin login did not return an access token');

const guardianName = `e2e_guardian_${runId}`;
const guardianPassword = `LocalGuard-${runId}-2026!`;
const guardian = await request('POST', '/auth/register', {
  username: guardianName, email: `${guardianName}@local.test`, password: guardianPassword, roles: ['DIRECCION']
}, null);
assert.deepEqual(guardian.roles.map(role => typeof role === 'string' ? role : role.codigo), ['APODERADO'], 'Public registration must not grant the requested admin role');
guardianToken = (await request('POST', '/auth/login', { username: guardianName, password: guardianPassword }, null)).accessToken;
await request('GET', '/dashboard/resumen', undefined, guardianToken, [403]);
console.log('OK authentication, public role restriction, admin authorization');

const levels = await request('GET', '/academico/niveles');
const primary = one(levels, level => level.codigo === 'PRIMARIA', 'PRIMARIA');
let year = (await request('GET', '/academico/anios')).find(item => item.anio === 2026);
year ??= await request('POST', '/academico/anios', { anio: 2026, fechaInicio: '2026-03-01', fechaFin: '2026-12-31' });
let period = (await request('GET', `/academico/periodos?anioId=${year.id}`)).find(item => item.numeroPeriodo === 3);
period ??= await request('POST', '/academico/periodos', { anioLectivoId: year.id, numeroPeriodo: 3, nombre: 'III Bimestre', fechaInicio: '2026-08-01', fechaFin: '2026-10-31' });
let grade = (await request('GET', `/academico/grados?nivelId=${primary.id}`)).find(item => item.numeroGrado === 1);
grade ??= await request('POST', '/academico/grados', { nivelId: primary.id, numeroGrado: 1, nombre: 'Primer Grado' });
let section = (await request('GET', `/academico/secciones?anioId=${year.id}`)).find(item => item.gradoId === grade.id && item.letra === 'A');
section ??= await request('POST', '/academico/secciones', { anioLectivoId: year.id, gradoId: grade.id, nivelId: primary.id, letra: 'A', cupoMaximo: 30, aulaFisica: 'Aula E2E' });
let area = (await request('GET', `/curriculo/areas?nivelId=${primary.id}`)).find(item => item.codigo === 'MAT');
area ??= await request('POST', '/curriculo/areas', { nivelId: primary.id, codigo: 'MAT', nombre: 'Matemática' });
let competency = (await request('GET', `/curriculo/competencias?areaId=${area.id}`))[0];
competency ??= await request('POST', '/curriculo/competencias', { areaId: area.id, numeroOrden: 1, nombre: 'Resuelve problemas de cantidad', descripcion: 'Competencia E2E' });
let assignment = (await request('GET', '/curriculo/asignaciones')).find(item => item.seccionId === section.id && item.areaCurricularId === area.id && item.anioLectivoId === year.id);
if (!assignment) {
  const teacher = await request('POST', '/usuarios', { username: `e2e_teacher_${runId}`, email: `e2e_teacher_${runId}@local.test`, password: `LocalTeacher-${runId}-2026!`, roles: ['DOCENTE'] });
  assignment = await request('POST', '/curriculo/asignaciones', { docenteUsuarioId: teacher.id, seccionId: section.id, anioLectivoId: year.id, nivelId: primary.id, areaCurricularId: area.id });
}
console.log('OK academic year, period, grade, section, curriculum, teacher assignment');

const dni = String(70000000 + Math.floor(Math.random() * 20000000));
const student = await request('POST', '/estudiantes', { tipoDocumento: 'DNI', numeroDocumento: dni, nombres: 'Integracion', apellidoPaterno: 'Modulos', apellidoMaterno: 'Local', fechaNacimiento: '2015-05-15', genero: 'M' });
const guardianDni = String(70000000 + Math.floor(Math.random() * 20000000));
const guardianRecord = await request('POST', '/apoderados', { tipoDocumento: 'DNI', numeroDocumento: guardianDni, nombres: 'Apoderado', apellidoPaterno: 'Prueba', apellidoMaterno: 'Local', celular: '987654321', email: `${guardianName}@local.test`, direccion: 'Avenida Prueba 123', ubigeoInei: '150101', usuarioId: guardian.id });
await request('POST', '/estudiantes/apoderados', { estudianteId: student.id, apoderadoId: guardianRecord.id, parentesco: 'PADRE', esResponsableEconomico: true, tieneCustodia: true, permiteRecojo: true });
const enrollment = await request('POST', '/matriculas', { anioLectivoId: year.id, estudianteId: student.id, seccionId: section.id, observaciones: 'E2E módulos' });
await request('POST', '/matriculas/confirmacion', { matriculaId: enrollment.id });
console.log('OK student, guardian, link, confirmed enrollment');

const attendance = await request('POST', '/asistencia/aula', { matriculaId: enrollment.id, fechaSesion: date, horaRegistro: '08:00:00', estado: 'FALTA_INJUSTIFICADA' });
const daily = await request('GET', `/asistencia/aula?fecha=${date}`);
assert.ok(daily.asistencias.some(item => item.id === attendance.id), 'Attendance was not persisted');
await request('POST', '/asistencia/aula/justificacion', { asistenciaId: attendance.id, motivo: 'Cita médica', documentoSustentoUrl: null });
const justified = await request('GET', `/asistencia/aula?fecha=${date}`);
assert.ok(justified.asistencias.some(item => item.id === attendance.id && item.estado === 'FALTA_JUSTIFICADA'), 'Justification did not update attendance');
await request('POST', '/asistencia/biometrico/importar', { nombreArchivo: `biometrico-${runId}.csv`, marcas: [{ dniLeido: dni, fechaHora: `${date}T08:00:00-05:00`, dispositivoCodigo: 'PUERTA-1' }] });
await request('POST', `/asistencia/conciliacion?anioId=${year.id}&fecha=${date}&turnoCerrado=true`);
await request('GET', `/asistencia/conciliacion/alertas?fecha=${date}`);
console.log('OK attendance, justification, biometric import, reconciliation');

const matrixBefore = await request('GET', `/evaluacion/cneb/matriz?asignacionDocenteId=${assignment.id}&periodoAcademicoId=${period.id}&competenciaId=${competency.id}`);
assert.ok(matrixBefore.some(item => item.matriculaId === enrollment.id), 'Enrolled student is missing from grading matrix');
await request('POST', '/evaluacion/cneb', { periodoAcademicoId: period.id, asignacionDocenteId: assignment.id, calificaciones: [{ matriculaId: enrollment.id, competenciaId: competency.id, calificacionCualitativa: 'C', conclusionDescriptiva: 'Requiere refuerzo en problemas de cantidad', sugerenciaIaUtilizada: false }] });
const grades = await request('GET', `/evaluacion/cneb?asignacionDocenteId=${assignment.id}&periodoAcademicoId=${period.id}`);
assert.ok(grades.some(item => item.matriculaId === enrollment.id && item.calificacionCualitativa === 'C'), 'Grade was not persisted');
const matrixAfter = await request('GET', `/evaluacion/cneb/matriz?asignacionDocenteId=${assignment.id}&periodoAcademicoId=${period.id}&competenciaId=${competency.id}`);
assert.ok(matrixAfter.some(item => item.matriculaId === enrollment.id && item.calificacionCualitativa === 'C'), 'Grading matrix did not refresh');
const reportCard = await request('GET', `/evaluacion/libreta/${enrollment.id}`);
assert.ok(reportCard.calificaciones.some(item => item.calificacionCualitativa === 'C'), 'Report card omitted the grade');
const atRisk = await request('GET', `/evaluacion/riesgo?periodoId=${period.id}`);
assert.ok(atRisk.calificaciones.some(item => item.matriculaId === enrollment.id), 'At-risk list omitted a C grade');
console.log('OK grading matrix, grade persistence, report card, risk detection');

const notice = await request('POST', '/comunicados', { titulo: `Aviso E2E ${runId}`, contenido: 'Reunión de prueba institucional', requiereAcuse: true, anioLectivoId: year.id, nivelId: primary.id, seccionId: section.id });
const notices = await request('GET', '/comunicados');
assert.ok(notices.some(item => item.id === notice.id), 'Published notice was not listed');
const metricsBefore = await request('GET', `/comunicados/${notice.id}/metricas`);
assert.ok(metricsBefore.destinatarios >= 1, 'Published notice did not reach the enrolled guardian');
assert.equal(metricsBefore.acuses, 0);
const inbox = await request('GET', '/comunicados/apoderado/bandeja', undefined, guardianToken);
assert.ok(inbox.comunicados.some(item => item.id === notice.id), 'Guardian inbox omitted the notice');
await request('POST', `/comunicados/${notice.id}/acuse`, { confirmarAcuse: true }, guardianToken);
const metricsAfter = await request('GET', `/comunicados/${notice.id}/metricas`);
assert.equal(metricsAfter.leidos, 1);
assert.equal(metricsAfter.acuses, 1);
console.log('OK notice publication, guardian inbox, acknowledgment, metrics');

const incident = await request('POST', '/convivencia/incidencias', { matriculaId: enrollment.id, fechaIncidencia: date, tipoFalta: 'GRAVE', descripcion: 'Incidencia de integración aislada', requiereCitacion: true });
const incidents = await request('GET', '/convivencia/incidencias');
assert.ok(incidents.some(item => item.id === incident.id), 'Incident was not listed');
const history = await request('GET', `/convivencia/matricula/${enrollment.id}`);
assert.ok(history.incidencias.some(item => item.id === incident.id), 'Student history omitted the incident');
const citations = await request('GET', '/convivencia/incidencias/citaciones?page=0&size=20');
assert.ok(citations.some(item => item.id === incident.id), 'Citation list omitted the incident');
const updatedIncident = await request('PUT', `/convivencia/incidencias/${incident.id}/estado`, { estado: 'ATENDIDA' });
assert.equal(updatedIncident.estado, 'ATENDIDA');
console.log('OK incident, student history, citation, state transition');

let block = (await request('GET', '/curriculo/bloques')).find(item => item.numeroBloque === 1);
block ??= await request('POST', '/curriculo/bloques', { numeroBloque: 1, horaInicio: '08:00:00', horaFin: '08:45:00', esRecreo: false });
let sectionTimetable = await request('GET', `/horarios/seccion/${section.id}?anioId=${year.id}`);
let timetableEntry = sectionTimetable.horarios.find(item => item.asignacionDocenteId === assignment.id && item.bloqueHorarioId === block.id && item.diaSemana === 1);
timetableEntry ??= await request('POST', '/horarios', { asignacionDocenteId: assignment.id, diaSemana: 'LUNES', bloqueHorarioId: block.id });
sectionTimetable = await request('GET', `/horarios/seccion/${section.id}?anioId=${year.id}`);
assert.ok(sectionTimetable.horarios.some(item => item.id === timetableEntry.id), 'Section timetable omitted the lesson');
const teacherTimetable = await request('GET', `/horarios/docente/${assignment.docenteUsuarioId}?anioId=${year.id}`);
assert.ok(teacherTimetable.horarios.some(item => item.id === timetableEntry.id), 'Teacher timetable omitted the lesson');
console.log('OK time block, lesson scheduling, section and teacher timetables');

const todayParts = Object.fromEntries(new Intl.DateTimeFormat('en-US', { timeZone: 'America/Lima', year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(new Date()).map(part => [part.type, part.value]));
const today = `${todayParts.year}-${todayParts.month}-${todayParts.day}`;
let reinforcement = (await request('GET', `/evaluacion/refuerzo?periodoId=${period.id}`)).find(item => item.tema === 'Refuerzo E2E Matemática' && item.fechaProgramada === today);
reinforcement ??= await request('POST', '/evaluacion/refuerzo', { anioLectivoId: year.id, periodoAcademicoId: period.id, areaCurricularId: area.id, docenteUsuarioId: assignment.docenteUsuarioId, tema: 'Refuerzo E2E Matemática', fechaProgramada: today, horaInicio: '10:00:00', horaFin: '10:45:00', aulaAsignada: 'Aula de refuerzo' });
const enrollments = await request('POST', `/evaluacion/refuerzo/${reinforcement.id}/derivacion`);
const reinforcementEnrollment = one(enrollments, item => item.estudianteId === student.id, 'At-risk reinforcement enrollment');
const reinforcementAttendance = await request('POST', '/evaluacion/refuerzo/asistencia', { inscripcionId: reinforcementEnrollment.id, estadoAsistencia: 'ASISTIO', observaciones: 'Asistió al refuerzo de integración' });
assert.equal(reinforcementAttendance.estadoAsistencia, 'ASISTIO');
console.log('OK reinforcement scheduling, at-risk derivation, attendance');

const concepts = await request('GET', '/tesoreria/conceptos');
const enrollmentFee = one(concepts, item => item.codigo === 'MATRICULA_ANUAL', 'Enrollment fee concept');
const tuition = one(concepts, item => item.codigo === 'PENSION_MENSUAL', 'Tuition concept');
const schedule = await request('POST', '/tesoreria/obligaciones', { matriculaId: enrollment.id, conceptoMatriculaId: enrollmentFee.id, conceptoPensionId: tuition.id, primerVencimiento: '2026-03-01' });
assert.equal(schedule.length, 11, 'Annual payment schedule must have 11 obligations');
const obligation = one(schedule, item => item.tipoConcepto === 'MATRICULA', 'Enrollment obligation');
const firstPayment = await request('POST', '/tesoreria/pagos/caja', { obligacionPagoId: obligation.id, pasarelaTransaccionId: `E2E-${runId}-1`, metodoPago: 'EFECTIVO', montoPagado: obligation.saldoPendiente });
const firstReceipt = await request('POST', '/comprobantes', { pagoId: firstPayment.id, tipoComprobante: 'BOLETA', serie: 'B001' });
assert.equal(firstReceipt.estadoComprobante, 'EMITIDO');
await request('POST', '/tesoreria/pagos/reversion', { pagoId: firstPayment.id, motivo: 'Reversión de prueba aislada' });
const annulledReceipt = await request('GET', `/comprobantes/${firstReceipt.id}`);
assert.equal(annulledReceipt.estadoComprobante, 'ANULADO', 'Reversal did not annul the receipt');
let accountObligations = await request('GET', `/tesoreria/obligaciones?matriculaId=${enrollment.id}`);
assert.ok(accountObligations.some(item => item.id === obligation.id && Number(item.saldoPendiente) === Number(obligation.saldoPendiente)), 'Reversal did not restore the balance');
const secondPayment = await request('POST', '/tesoreria/pagos/caja', { obligacionPagoId: obligation.id, pasarelaTransaccionId: `E2E-${runId}-2`, metodoPago: 'EFECTIVO', montoPagado: obligation.saldoPendiente });
const secondReceipt = await request('POST', '/comprobantes', { pagoId: secondPayment.id, tipoComprobante: 'BOLETA', serie: 'B001' });
assert.equal((await request('GET', `/comprobantes/${secondReceipt.id}`)).estadoComprobante, 'EMITIDO');
accountObligations = await request('GET', `/tesoreria/obligaciones?matriculaId=${enrollment.id}`);
assert.ok(accountObligations.some(item => item.id === obligation.id && Number(item.saldoPendiente) === 0), 'Second payment did not settle the obligation');
const account = await request('GET', `/tesoreria/estudiantes/${student.id}/estado-cuenta`);
assert.ok(account.obligaciones.some(item => item.id === obligation.id), 'Student account omitted the obligation');
await request('POST', '/pagos/webhook/culqi', { transaccionId: `FAKE-${runId}`, obligacionPagoId: obligation.id }, null, [403]);
await request('POST', '/pagos/webhook/niubiz', { transaccionId: `FAKE-${runId}`, obligacionPagoId: obligation.id }, null, [403]);
await request('POST', `/pagos/webhook/mercadopago?data.id=FAKE-${runId}&type=payment`, {}, null, [401]);
console.log('OK payment schedule, payment, receipt, reversal, annulment, second settlement, account');

const frontend = process.env.E2E_FRONT_BASE || 'http://localhost:4200';
assert.ok(['localhost', '127.0.0.1'].includes(new URL(frontend).hostname), 'Frontend must be local');
const browser = await puppeteer.launch({ executablePath: process.env.E2E_CHROME || 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe', headless: true, args: ['--no-sandbox', '--disable-dev-shm-usage'] });
const uiErrors = [];
async function preparePage(page) {
  page.setDefaultTimeout(30000);
  page.on('pageerror', error => uiErrors.push(error.message));
  page.on('response', response => {
    if (response.url().startsWith(apiBase) && response.status() >= 400) uiErrors.push(`${response.status()} ${response.request().method()} ${response.url()}`);
  });
  page.on('dialog', dialog => dialog.accept());
  await page.setRequestInterception(true);
  page.on('request', request => {
    const url = request.url();
    if (url.startsWith('http://localhost:8080/api/v1')) request.continue({ url: apiBase + url.slice('http://localhost:8080/api/v1'.length) });
    else request.continue();
  });
}
async function loginPage(page, name, secret) {
  await page.goto(`${frontend}/auth/login`, { waitUntil: 'domcontentloaded' });
  await page.waitForSelector('#username');
  await page.type('#username', name);
  await page.type('#password', secret);
  await page.click('button[type=submit]');
  try {
    await page.waitForFunction(() => location.pathname.startsWith('/admin/'), { timeout: 15000 });
  } catch {
    throw new Error(`Angular login stayed on ${page.url()}: ${(await page.evaluate(() => document.body.innerText)).slice(0, 500)}; browser errors: ${uiErrors.join(' | ')}`);
  }
}
async function waitForText(page, value) {
  await page.waitForFunction(text => document.body.innerText.includes(text), {}, value);
}
try {
  const page = await browser.newPage();
  await preparePage(page);
  await loginPage(page, username, password);

  await page.goto(`${frontend}/admin/asistencia`, { waitUntil: 'domcontentloaded' });
  await page.waitForSelector('input[type=date]');
  await page.$eval('input[type=date]', (input, value) => { input.value = value; input.dispatchEvent(new Event('input', { bubbles: true })); input.dispatchEvent(new Event('change', { bubbles: true })); }, date);
  await waitForText(page, 'FALTA_JUSTIFICADA');
  console.log('OK attendance visible in Angular UI');

  await page.evaluate(() => [...document.querySelectorAll('button')].find(button => button.innerText.includes('Registrar asistencia'))?.click());
  await page.waitForSelector('input[name=matriculaId]');
  await page.type('input[name=matriculaId]', String(enrollment.id));
  await page.$eval('input[name=fechaSesion]', (input, value) => { input.value = value; input.dispatchEvent(new Event('input', { bubbles: true })); input.dispatchEvent(new Event('change', { bubbles: true })); }, today);
  await page.select('select[name=estado]', 'FALTA_INJUSTIFICADA');
  const attendanceResponse = page.waitForResponse(response => response.url().endsWith('/asistencia/aula') && response.request().method() === 'POST');
  await page.click('form button[type=submit]');
  assert.equal((await attendanceResponse).status(), 200, 'Angular attendance form failed');
  await waitForText(page, 'FALTA_INJUSTIFICADA');
  await page.evaluate(id => { const row = [...document.querySelectorAll('table tbody tr')].find(item => item.innerText.includes(`Matrícula #${id}`) && item.innerText.includes('FALTA_INJUSTIFICADA')); row?.querySelector('button')?.click(); }, enrollment.id);
  await page.waitForSelector('input[aria-label^="Motivo de justificación"]');
  await page.type('input[aria-label^="Motivo de justificación"]', 'Cita médica justificada desde Angular');
  const justificationResponse = page.waitForResponse(response => response.url().endsWith('/asistencia/aula/justificacion') && response.request().method() === 'POST');
  await page.evaluate(() => [...document.querySelectorAll('table tbody button')].find(button => button.innerText === 'Guardar')?.click());
  assert.equal((await justificationResponse).status(), 200, 'Angular justification form failed');
  const uiAttendance = await request('GET', `/asistencia/aula?fecha=${today}`);
  assert.ok(uiAttendance.asistencias.some(item => item.matriculaId === enrollment.id && item.estado === 'FALTA_JUSTIFICADA'), 'Angular justification did not persist');
  await page.evaluate(() => [...document.querySelectorAll('button')].find(button => button.innerText.includes('Importar marca'))?.click());
  await page.waitForSelector('input[name=dniBiometrico]');
  await page.type('input[name=dniBiometrico]', dni);
  await page.$eval('input[name=fechaHoraBiometrico]', (input, value) => { input.value = value; input.dispatchEvent(new Event('input', { bubbles: true })); input.dispatchEvent(new Event('change', { bubbles: true })); }, `${today}T08:02`);
  const biometricResponse = page.waitForResponse(response => response.url().endsWith('/asistencia/biometrico/importar') && response.request().method() === 'POST');
  await page.click('form button[type=submit]');
  assert.equal((await biometricResponse).status(), 200, 'Angular biometric form failed');
  console.log('OK Angular attendance, justification, biometric import -> backend -> PostgreSQL');

  await page.goto(`${frontend}/admin/calificaciones`, { waitUntil: 'domcontentloaded' });
  await page.waitForFunction(() => [...document.querySelectorAll('select option')].some(option => option.textContent.includes('III Bimestre')));
  const periodOption = await page.$eval('select', select => [...select.options].find(item => item.textContent.includes('III Bimestre'))?.value);
  assert.ok(periodOption, 'III Bimestre option missing');
  const currentPeriodLabel = await page.$eval('select', select => select.selectedOptions[0]?.textContent || '');
  if (!currentPeriodLabel.includes('III Bimestre')) {
    const matrixResponse = page.waitForResponse(response => new URL(response.url()).pathname.endsWith('/evaluacion/cneb/matriz') && new URL(response.url()).searchParams.get('periodoAcademicoId') === String(period.id) && response.status() === 200);
    await page.select('select', periodOption);
    await matrixResponse;
  }
  await page.waitForFunction(() => document.querySelector('select')?.selectedOptions[0]?.textContent.includes('III Bimestre'));
  await page.waitForFunction(id => [...document.querySelectorAll('table tbody tr')].some(row => row.innerText.includes(`Matrícula #${id}`) && row.querySelector('select')?.value === 'C'), {}, enrollment.id);
  await page.evaluate(() => { for (const select of document.querySelectorAll('table tbody select')) { select.value = 'A'; select.dispatchEvent(new Event('change', { bubbles: true })); } });
  const gradeResponse = page.waitForResponse(response => response.url().endsWith('/evaluacion/cneb') && response.request().method() === 'POST');
  await page.evaluate(() => [...document.querySelectorAll('button')].find(button => button.innerText.includes('Guardar en BD'))?.click());
  const gradeHttpResponse = await gradeResponse;
  assert.equal(gradeHttpResponse.status(), 200, 'Angular grading form failed');
  const gradePayload = JSON.parse(gradeHttpResponse.request().postData());
  assert.equal(gradePayload.periodoAcademicoId, period.id, 'Angular posted grades to the wrong period');
  assert.ok(gradePayload.calificaciones.some(item => item.matriculaId === enrollment.id && item.calificacionCualitativa === 'A'), 'Angular did not submit the edited student grade');
  const uiGrades = await request('GET', `/evaluacion/cneb?asignacionDocenteId=${assignment.id}&periodoAcademicoId=${period.id}`);
  assert.ok(uiGrades.some(item => item.matriculaId === enrollment.id && item.calificacionCualitativa === 'A'), 'Angular grade did not persist');
  console.log('OK Angular grading form -> backend -> PostgreSQL');

  await page.goto(`${frontend}/admin/comunicados`, { waitUntil: 'domcontentloaded' });
  await waitForText(page, notice.titulo);
  await page.evaluate(() => [...document.querySelectorAll('button')].find(button => button.innerText.includes('Publicar Comunicado en BD'))?.click());
  await page.waitForSelector('input[name=titulo]');
  const uiNoticeTitle = `Aviso web ${runId}`;
  await page.type('input[name=titulo]', uiNoticeTitle);
  await page.type('textarea[name=contenido]', 'Contenido generado desde el formulario Angular');
  await page.click('input[name=requiereAcuse]');
  const noticeResponse = page.waitForResponse(response => response.url().endsWith('/comunicados') && response.request().method() === 'POST');
  await page.click('form button[type=submit]');
  assert.equal((await noticeResponse).status(), 200, 'Angular notice form failed');
  await waitForText(page, uiNoticeTitle);
  const uiNotice = one(await request('GET', '/comunicados'), item => item.titulo === uiNoticeTitle, 'Angular-published notice');
  console.log('OK Angular notice publication -> backend -> PostgreSQL');

  await page.goto(`${frontend}/admin/convivencia`, { waitUntil: 'domcontentloaded' });
  await waitForText(page, incident.descripcion);
  await page.evaluate(() => [...document.querySelectorAll('button')].find(button => button.innerText.includes('Registrar Incidencia en BD'))?.click());
  await page.waitForSelector('input[name=matricula]');
  await page.type('input[name=matricula]', String(enrollment.id));
  await page.$eval('input[name=fecha]', (input, value) => { input.value = value; input.dispatchEvent(new Event('input', { bubbles: true })); input.dispatchEvent(new Event('change', { bubbles: true })); }, date);
  await page.select('select[name=tipoFalta]', 'LEVE');
  const uiIncidentDescription = `Incidencia web ${runId}`;
  await page.type('textarea[name=descripcion]', uiIncidentDescription);
  const incidentResponse = page.waitForResponse(response => response.url().endsWith('/convivencia/incidencias') && response.request().method() === 'POST');
  await page.click('form button[type=submit]');
  assert.equal((await incidentResponse).status(), 200, 'Angular incident form failed');
  await waitForText(page, uiIncidentDescription);
  console.log('OK Angular incident form -> backend -> PostgreSQL');

  await page.goto(`${frontend}/admin/academico`, { waitUntil: 'domcontentloaded' });
  await waitForText(page, 'Nueva Aula en BD');
  await page.evaluate(() => [...document.querySelectorAll('button')].find(button => button.innerText.includes('Nueva Aula en BD'))?.click());
  await page.waitForSelector('input[name=codigo]');
  const classroomCode = `AULA-${runId}`;
  await page.type('input[name=codigo]', classroomCode);
  await page.type('input[name=nombre]', `Aula Integración ${runId}`);
  await page.type('input[name=ubicacion]', 'Pabellón E2E');
  await page.$eval('input[name=capacidad]', input => { input.value = ''; input.dispatchEvent(new Event('input', { bubbles: true })); });
  await page.type('input[name=capacidad]', '25');
  const classroomResponse = page.waitForResponse(response => response.url().endsWith('/academico/aulas') && response.request().method() === 'POST');
  await page.click('form button[type=submit]');
  assert.equal((await classroomResponse).status(), 200, 'Angular classroom form failed');
  assert.ok((await request('GET', '/academico/aulas')).some(item => item.codigo.toUpperCase() === classroomCode.toUpperCase() && item.capacidad === 25), 'Angular classroom did not persist');
  console.log('OK Angular classroom form -> backend -> PostgreSQL');

  await page.goto(`${frontend}/admin/usuarios`, { waitUntil: 'domcontentloaded' });
  await waitForText(page, 'Crear Usuario en BD');
  await page.evaluate(() => [...document.querySelectorAll('button')].find(button => button.innerText.includes('Crear Usuario en BD'))?.click());
  await page.waitForSelector('.fixed input[type=text]');
  const uiUsername = `e2e_aux_${runId}`;
  const uiPassword = `LocalAux-${runId}-2026!`;
  const uiNewPassword = `ChangedAux-${runId}-2026!`;
  await page.type('.fixed input[type=text]', uiUsername);
  await page.type('.fixed input[type=email]', `${uiUsername}@local.test`);
  await page.type('.fixed input[type=password]', uiPassword);
  await page.select('.fixed select', 'AUXILIAR');
  const userResponse = page.waitForResponse(response => response.url().endsWith('/usuarios') && response.request().method() === 'POST');
  await page.evaluate(() => [...document.querySelectorAll('.fixed button')].find(button => button.innerText.includes('Crear Cuenta en BD'))?.click());
  assert.ok([200, 201].includes((await userResponse).status()), 'Angular user creation failed');
  await waitForText(page, uiUsername);
  const uiUser = one(await request('GET', '/usuarios'), item => item.username === uiUsername, 'Angular-created user');
  console.log('OK Angular auxiliary user created');

  const userContext = await browser.createBrowserContext();
  const userPage = await userContext.newPage();
  await preparePage(userPage);
  await loginPage(userPage, uiUsername, uiPassword);
  console.log('OK auxiliary user login');
  await userPage.goto(`${frontend}/admin/perfil`, { waitUntil: 'domcontentloaded' });
  await userPage.waitForSelector('input[name=passwordActual]');
  await userPage.type('input[name=passwordActual]', uiPassword);
  await userPage.type('input[name=passwordNuevo]', uiNewPassword);
  await userPage.type('input[name=confirmarPassword]', uiNewPassword);
  const passwordResponse = userPage.waitForResponse(response => response.url().endsWith('/usuarios/password') && response.request().method() === 'PUT');
  await userPage.click('form button[type=submit]');
  assert.equal((await passwordResponse).status(), 200, 'Angular password change failed');
  console.log('OK Angular password change');
  await request('POST', '/auth/login', { username: uiUsername, password: uiPassword }, null, [401]);
  const updatedUserLogin = await request('POST', '/auth/login', { username: uiUsername, password: uiNewPassword }, null);
  assert.ok(updatedUserLogin.accessToken, 'New password did not work');
  const refreshed = await request('POST', '/auth/refresh', { refreshToken: updatedUserLogin.refreshToken }, null);
  assert.ok(refreshed.accessToken, 'Refresh token did not renew the session');
  const updatedProfile = await request('PUT', '/usuarios/me', { nombres: 'Auxiliar Integración', email: `${uiUsername}-actualizado@local.test` }, updatedUserLogin.accessToken);
  assert.equal(updatedProfile.email, `${uiUsername}-actualizado@local.test`, 'Profile update did not persist');
  assert.equal((await request('GET', '/usuarios/me', undefined, updatedUserLogin.accessToken)).email, updatedProfile.email);
  console.log('OK new password login and old password rejection');
  await userContext.close();

  const statusResponse = page.waitForResponse(response => new URL(response.url()).pathname.endsWith(`/usuarios/${uiUser.id}/estado`) && response.request().method() === 'PATCH');
  const statusClicked = await page.evaluate(name => { const row = [...document.querySelectorAll('table tbody tr')].find(item => item.innerText.includes(name)); const button = row?.querySelector('button'); button?.click(); return Boolean(button); }, uiUsername);
  assert.ok(statusClicked, 'Angular user suspension button not found');
  assert.equal((await statusResponse).status(), 200, 'Angular user suspension failed');
  await request('POST', '/auth/login', { username: uiUsername, password: uiNewPassword }, null, [401, 403]);
  console.log('OK Angular user creation, profile password change, suspension and login denial');

  const guardianPage = await browser.newPage();
  await preparePage(guardianPage);
  await guardianPage.goto(`${frontend}/auth/login`, { waitUntil: 'domcontentloaded' });
  await guardianPage.evaluate(() => localStorage.clear());
  await loginPage(guardianPage, guardianName, guardianPassword);
  await guardianPage.waitForFunction(() => location.pathname === '/admin/comunicados');
  await waitForText(guardianPage, uiNoticeTitle);
  const acuseResponse = guardianPage.waitForResponse(response => response.url().endsWith(`/comunicados/${uiNotice.id}/acuse`) && response.request().method() === 'POST');
  await guardianPage.evaluate(title => { const card = [...document.querySelectorAll('div')].find(item => item.querySelector('h3')?.textContent === title && item.querySelector('button')); card?.querySelector('button')?.click(); }, uiNoticeTitle);
  assert.equal((await acuseResponse).status(), 200, 'Angular guardian acknowledgment failed');
  await waitForText(guardianPage, 'Lectura y acuse confirmados.');
  assert.equal((await request('GET', `/comunicados/${uiNotice.id}/metricas`)).acuses, 1);
  await guardianPage.goto(`${frontend}/admin/usuarios`, { waitUntil: 'domcontentloaded' });
  await guardianPage.waitForFunction(() => location.pathname === '/admin/comunicados');
  console.log('OK guardian Angular inbox and acknowledgment -> backend -> PostgreSQL');

  assert.deepEqual(uiErrors, [], 'Angular/browser errors');
} finally {
  await browser.close();
}

const releasedReservations = await request('POST', '/matriculas/reservas/liberar');
assert.ok(Number.isInteger(releasedReservations) && releasedReservations >= 0, 'Reservation cleanup response is invalid');
let closingYear = (await request('GET', '/academico/anios')).find(item => item.anio === 2027);
closingYear ??= await request('POST', '/academico/anios', { anio: 2027, fechaInicio: '2027-03-01', fechaFin: '2027-12-31' });
let closingPeriod = (await request('GET', `/academico/periodos?anioId=${closingYear.id}`)).find(item => item.numeroPeriodo === 1);
if (!closingPeriod && closingYear.abierto) closingPeriod = await request('POST', '/academico/periodos', { anioLectivoId: closingYear.id, numeroPeriodo: 1, nombre: 'I Bimestre', fechaInicio: '2027-03-01', fechaFin: '2027-05-31' });
assert.ok(closingPeriod, 'Closing-period fixture missing');
if (!closingPeriod.cerrado) await request('PUT', `/academico/periodos/${closingPeriod.id}/cierre`);
if (closingYear.abierto) await request('PUT', `/academico/anios/${closingYear.id}/cierre`);
assert.equal((await request('GET', `/academico/periodos?anioId=${closingYear.id}`)).find(item => item.id === closingPeriod.id).cerrado, true);
assert.equal((await request('GET', '/academico/anios')).find(item => item.id === closingYear.id).abierto, false);
console.log('OK reservation cleanup and academic period/year closure');

console.log(`PASS ${checks} isolated backend integration requests`);
