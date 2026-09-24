import assert from 'node:assert/strict';
import puppeteer from 'puppeteer-core';

// Run only against disposable local services. Seed E2E_USERNAME with DIRECCION.
const frontend = process.env.E2E_FRONT_BASE;
const backend = process.env.E2E_API_BASE;
const username = process.env.E2E_USERNAME;
const password = process.env.E2E_PASSWORD;
for (const [name, value] of Object.entries({ E2E_FRONT_BASE: frontend, E2E_API_BASE: backend, E2E_USERNAME: username, E2E_PASSWORD: password })) {
  assert.ok(value, `${name} is required`);
}
for (const url of [frontend, backend]) {
  assert.ok(['localhost', '127.0.0.1'].includes(new URL(url).hostname), 'E2E targets must be local');
}

const chrome = process.env.E2E_CHROME ?? 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
const browser = await puppeteer.launch({ executablePath: chrome, headless: true, args: ['--no-sandbox', '--disable-dev-shm-usage'] });
const page = await browser.newPage();
page.setDefaultTimeout(30000);
const browserApiFailures = [];
const pageErrors = [];
page.on('pageerror', error => pageErrors.push(error.message));
page.on('response', response => {
  if (response.url().startsWith(backend) && response.status() >= 400) {
    browserApiFailures.push(`${response.status()} ${response.request().method()} ${response.url()}`);
  }
});
page.on('dialog', dialog => dialog.accept());
await page.setRequestInterception(true);
page.on('request', request => {
  const url = request.url();
  if (url.startsWith('http://localhost:8080/api/v1')) {
    request.continue({ url: backend + url.slice('http://localhost:8080/api/v1'.length) });
  } else {
    request.continue();
  }
});

let token;
async function api(method, path, body) {
  const response = await fetch(`${backend}${path}`, {
    method,
    headers: { Authorization: `Bearer ${token}`, ...(body ? { 'Content-Type': 'application/json' } : {}) },
    ...(body ? { body: JSON.stringify(body) } : {})
  });
  const raw = await response.text();
  assert.ok(response.ok, `${method} ${path}: ${response.status} ${raw}`);
  return raw ? JSON.parse(raw) : null;
}
async function waitForText(text) {
  await page.waitForFunction(text => document.body.innerText.includes(text), {}, text);
}
async function setInput(selector, value) {
  await page.$eval(selector, (input, value) => {
    input.value = value;
    input.dispatchEvent(new Event('input', { bubbles: true }));
    input.dispatchEvent(new Event('change', { bubbles: true }));
  }, value);
}

try {
  await page.goto(`${frontend}/auth/login`, { waitUntil: 'domcontentloaded' });
  await page.waitForSelector('#username');
  await page.type('#username', username);
  await page.type('#password', password);
  await page.click('button[type="submit"]');
  await page.waitForFunction(() => location.pathname === '/admin/dashboard');
  await waitForText('Bienvenido');
  token = await page.evaluate(() => localStorage.getItem('accessToken'));
  assert.ok(token, 'The login did not persist the access token');
  console.log('OK login -> dashboard');

  const levels = await api('GET', '/academico/niveles');
  const primary = levels.find(level => level.codigo === 'PRIMARIA');
  assert.ok(primary, 'Missing PRIMARIA catalog entry');
  let year = (await api('GET', '/academico/anios')).find(item => item.anio === 2026);
  year ??= await api('POST', '/academico/anios', { anio: 2026, fechaInicio: '2026-03-01', fechaFin: '2026-12-31' });
  let grade = (await api('GET', `/academico/grados?nivelId=${primary.id}`)).find(item => item.numeroGrado === 1);
  grade ??= await api('POST', '/academico/grados', { nivelId: primary.id, numeroGrado: 1, nombre: 'Primer Grado' });
  let section = (await api('GET', `/academico/secciones?anioId=${year.id}`)).find(item => item.gradoId === grade.id && item.letra === 'A');
  section ??= await api('POST', '/academico/secciones', { anioLectivoId: year.id, gradoId: grade.id, nivelId: primary.id, letra: 'A', cupoMaximo: 30, aulaFisica: 'Aula E2E' });
  console.log('OK academic catalog -> year/grade/section');

  await page.goto(`${frontend}/admin/matriculas`, { waitUntil: 'domcontentloaded' });
  await page.waitForSelector('#btn-abrir-registro-alumno');
  await page.click('#tab-matriculas');
  await page.waitForFunction(() => [...document.querySelectorAll('select option')].some(option => option.textContent.includes('Sección A')));
  await page.click('#tab-estudiantes');
  await page.click('#btn-abrir-registro-alumno');
  await page.waitForSelector('#nuevo-alumno-numero-doc');
  const dni = String(70000000 + Math.floor(Math.random() * 20000000));
  await page.type('#nuevo-alumno-numero-doc', dni);
  await page.type('#nuevo-alumno-nombres', 'Integracion');
  await page.type('#nuevo-alumno-ape-paterno', 'Prueba');
  await page.type('#nuevo-alumno-ape-materno', 'Local');
  await page.click('#btn-guardar-alumno');
  await waitForText('registrado exitosamente');
  await page.waitForFunction(dni => document.querySelector('#tabla-estudiantes')?.innerText.includes(dni), {}, dni);
  const students = await api('GET', '/estudiantes');
  const student = students.find(item => item.numeroDocumento === dni);
  assert.ok(student, 'The student was not persisted');
  const secondDni = String(70000000 + Math.floor(Math.random() * 20000000));
  await page.click('#btn-abrir-registro-alumno');
  await page.waitForSelector('#nuevo-alumno-numero-doc');
  await page.type('#nuevo-alumno-numero-doc', secondDni);
  await page.type('#nuevo-alumno-nombres', 'Segunda');
  await page.type('#nuevo-alumno-ape-paterno', 'Prueba');
  await page.type('#nuevo-alumno-ape-materno', 'Local');
  await page.click('#btn-guardar-alumno');
  await page.waitForFunction(dni => document.querySelector('#tabla-estudiantes')?.innerText.includes(dni), {}, secondDni);
  assert.ok((await api('GET', '/estudiantes')).find(item => item.numeroDocumento === secondDni), 'The second student without SIAGIE was not persisted');
  console.log('OK two student forms without SIAGIE -> API -> PostgreSQL');

  await page.evaluate(dni => {
    const row = [...document.querySelectorAll('#tabla-estudiantes tbody tr')].find(item => item.innerText.includes(dni));
    row?.querySelector('button')?.click();
  }, dni);
  await waitForText('Asignar Solicitud de Matrícula');
  await page.waitForFunction(() => [...document.querySelectorAll('select[name="seccion"] option')].some(option => option.textContent.includes('Primer Grado - Sección A')));
  await page.evaluate(() => {
    const select = document.querySelector('select[name="seccion"]');
    const option = [...select.options].find(item => item.textContent.includes('Primer Grado - Sección A'));
    select.value = option.value;
    select.dispatchEvent(new Event('change', { bubbles: true }));
  });
  await page.evaluate(() => {
    const form = [...document.querySelectorAll('form')].find(item => item.innerText.includes('Asignar Matrícula'));
    form?.querySelector('button[type="submit"]')?.click();
  });
  await waitForText('solicitada exitosamente');
  const matriculas = await api('GET', `/matriculas/seccion/${section.id}`);
  const matricula = matriculas.find(item => item.estudianteId === student.id);
  assert.ok(matricula, 'The enrollment was not persisted');
  await page.click('#tab-matriculas');
  await page.waitForFunction(id => document.body.innerText.includes(`MAT-2026-${String(id).padStart(3, '0')}`), {}, matricula.id);
  console.log('OK enrollment form -> API -> PostgreSQL');

  await page.goto(`${frontend}/admin/tesoreria`, { waitUntil: 'domcontentloaded' });
  await waitForText('Tesorería & Pagos');
  await page.waitForFunction(() => document.body.innerText.includes('Generar Cronograma en BD'));
  await page.evaluate(() => [...document.querySelectorAll('button')].find(button => button.innerText.includes('Generar Cronograma en BD'))?.click());
  await page.waitForSelector('input[name="matricula"]');
  await page.waitForFunction(() => document.querySelectorAll('select[name="conceptoMatricula"] option').length > 0);
  await setInput('input[name="matricula"]', String(matricula.id));
  await setInput('input[name="primerVencimiento"]', '2026-03-01');
  const scheduleResponse = page.waitForResponse(response => response.url().endsWith('/tesoreria/obligaciones') && response.request().method() === 'POST');
  await page.click('form button[type="submit"]');
  assert.equal((await scheduleResponse).status(), 200, 'The payment schedule request failed');
  await page.waitForFunction(id => [...document.querySelectorAll('table tbody tr')].filter(row => row.innerText.includes(`Matrícula #${id}`)).length >= 11, {}, matricula.id);
  let obligations = await api('GET', `/tesoreria/obligaciones?matriculaId=${matricula.id}`);
  assert.equal(obligations.length, 11, 'Expected one enrollment fee and ten tuition installments');
  console.log('OK payment schedule -> 11 persisted obligations');

  const paymentResponse = page.waitForResponse(response => response.url().endsWith('/tesoreria/pagos/caja') && response.request().method() === 'POST');
  await page.evaluate(() => [...document.querySelectorAll('button')].find(button => button.innerText.includes('Cobrar en Caja'))?.click());
  assert.equal((await paymentResponse).status(), 200, 'The cash payment request failed');
  await page.waitForFunction(() => [...document.querySelectorAll('table tbody tr')].some(row => row.innerText.includes('PAGADO')));
  obligations = await api('GET', `/tesoreria/obligaciones?matriculaId=${matricula.id}`);
  assert.ok(obligations.some(item => item.estado === 'PAGADO_TOTAL' && Number(item.saldoPendiente) === 0), 'Cash payment was not applied');
  await page.reload({ waitUntil: 'domcontentloaded' });
  await page.waitForSelector('input[type="number"]');
  await setInput('input[type="number"]', String(matricula.id));
  await page.waitForFunction(() => [...document.querySelectorAll('table tbody tr')].some(row => row.innerText.includes('PAGADO')));
  console.log('OK cash payment -> trigger -> persisted after reload');

  assert.deepEqual(browserApiFailures, [], 'Browser API failures');
  assert.deepEqual(pageErrors, [], 'Browser exceptions');
  console.log('PASS frontend/backend integration');
} catch (error) {
  console.error('Browser API failures:', browserApiFailures);
  console.error('Browser exceptions:', pageErrors);
  console.error('Visible page:', (await page.evaluate(() => document.body.innerText)).slice(0, 1500));
  throw error;
} finally {
  await browser.close();
}
