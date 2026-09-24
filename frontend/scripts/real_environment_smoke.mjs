import assert from 'node:assert/strict';
import puppeteer from 'puppeteer-core';

const username = process.env.E2E_USERNAME;
const password = process.env.E2E_PASSWORD;
const frontend = process.env.E2E_FRONT_BASE ?? 'http://localhost:4200';
const backend = process.env.E2E_API_BASE ?? 'http://localhost:8080/api/v1';
assert.ok(username && password, 'E2E_USERNAME and E2E_PASSWORD are required');
for (const url of [frontend, backend]) {
  assert.ok(['localhost', '127.0.0.1'].includes(new URL(url).hostname), 'Only local services are permitted');
}

const cases = [
  ['dashboard', 'Bienvenido'],
  ['matriculas', 'Padrón de Estudiantes & Matrículas'],
  ['calificaciones', 'Matriz de Calificaciones CNEB'],
  ['asistencia', 'Control de Asistencia & Biometría'],
  ['tesoreria', 'Tesorería & Pagos'],
  ['academico', 'Estructura Académica & Aulas'],
  ['convivencia', 'Convivencia Escolar & Tutoría'],
  ['comunicados', 'Comunicados Institucionales'],
  ['usuarios', 'Usuarios & Permisos (RBAC)'],
  ['perfil', 'Mi Perfil Institucional']
];

const browser = await puppeteer.launch({
  executablePath: process.env.E2E_CHROME ?? 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
  headless: true,
  args: ['--no-sandbox', '--disable-dev-shm-usage']
});
const page = await browser.newPage();
page.setDefaultTimeout(20000);
let phase = 'login';
const failures = [];
const traffic = new Map();
page.on('pageerror', error => failures.push(`${phase}: JavaScript ${error.message}`));
page.on('requestfailed', request => {
  if (request.url().startsWith(backend)) failures.push(`${phase}: NETWORK ${request.method()} ${request.url()}`);
});
page.on('response', response => {
  if (!response.url().startsWith(backend)) return;
  const key = phase;
  traffic.set(key, [...(traffic.get(key) ?? []), `${response.status()} ${response.request().method()} ${new URL(response.url()).pathname}`]);
  if (response.status() >= 400) failures.push(`${phase}: HTTP ${response.status()} ${response.request().method()} ${response.url()}`);
});

try {
  await page.goto(`${frontend}/auth/login`, { waitUntil: 'domcontentloaded' });
  await page.waitForSelector('#username');
  await page.type('#username', username);
  await page.type('#password', password);
  const loginResponse = page.waitForResponse(response => response.url().endsWith('/auth/login') && response.request().method() === 'POST');
  await page.click('button[type="submit"]');
  assert.equal((await loginResponse).status(), 200, 'Login failed');
  await page.waitForFunction(() => location.pathname === '/admin/dashboard');
  assert.ok(await page.evaluate(() => localStorage.getItem('accessToken')), 'Missing access token');
  console.log('OK login');

  for (const [route, heading] of cases) {
    phase = route;
    await page.goto(`${frontend}/admin/${route}`, { waitUntil: 'domcontentloaded' });
    await page.waitForFunction(heading => [...document.querySelectorAll('h2')].some(node => node.textContent.includes(heading)), {}, heading);
    await page.waitForNetworkIdle({ idleTime: 500, timeout: 10000 }).catch(() => {});
    assert.equal(new URL(page.url()).pathname, `/admin/${route}`, `${route} redirected unexpectedly`);
    console.log(`${failures.some(item => item.startsWith(`${route}:`)) ? 'FAIL' : 'OK'} ${route}: ${(traffic.get(route) ?? []).join(', ') || 'no API calls'}`);
  }

  assert.deepEqual(failures, [], `Integration failures: ${failures.join('; ')}`);
  console.log('PASS real-environment read-only smoke');
} finally {
  await browser.close();
}
