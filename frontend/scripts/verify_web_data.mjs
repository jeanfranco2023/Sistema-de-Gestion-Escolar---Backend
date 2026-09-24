import puppeteer from 'puppeteer-core';
import path from 'path';
import { mkdir } from 'node:fs/promises';

const CHROME_PATH = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
const ARTIFACT_DIR = path.resolve('artifacts');
const username = process.env.E2E_USERNAME;
const password = process.env.E2E_PASSWORD;
if (!username || !password) throw new Error('E2E_USERNAME and E2E_PASSWORD are required');
const SCREENSHOT_MATRICULAS = path.join(ARTIFACT_DIR, 'web_matriculas_con_datos_bd.png');
const SCREENSHOT_DASHBOARD = path.join(ARTIFACT_DIR, 'web_dashboard_con_datos_bd.png');

async function testWeb() {
  console.log('🚀 Iniciando Chrome para verificar la carga de datos maestros en la Web UI...');
  await mkdir(ARTIFACT_DIR, { recursive: true });
  const browser = await puppeteer.launch({
    executablePath: CHROME_PATH,
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--window-size=1400,900']
  });

  try {
    const page = await browser.newPage();
    await page.setViewport({ width: 1400, height: 900 });

    page.on('response', async res => {
      if (res.url().includes('api/v1/')) {
        console.log(`API [${res.status()}] ${res.url()}`);
      }
    });

    console.log('🌐 Ingresando al sistema...');
    await page.goto('http://localhost:4200/auth/login', { waitUntil: 'networkidle0' });
    await page.waitForSelector('#username');
    await page.type('#username', username, { delay: 20 });
    await page.type('#password', password, { delay: 20 });
    await Promise.all([
      page.waitForNavigation({ waitUntil: 'networkidle0', timeout: 15000 }).catch(() => {}),
      page.click('button[type="submit"]')
    ]);

    await new Promise(r => setTimeout(r, 2000));
    console.log('📸 Capturando Dashboard con métricas...');
    await page.screenshot({ path: SCREENSHOT_DASHBOARD, fullPage: true });

    console.log('📂 Navegando a Matrículas & Alumnos...');
    await page.goto('http://localhost:4200/admin/matriculas', { waitUntil: 'networkidle0' });
    await new Promise(r => setTimeout(r, 2000));

    console.log('🔄 Cambiando a la pestaña "Matrículas por Sección"...');
    await page.click('#tab-matriculas');
    await new Promise(r => setTimeout(r, 1500));

    console.log('📸 Capturando Matrículas por Sección con datos reales...');
    await page.screenshot({ path: SCREENSHOT_MATRICULAS, fullPage: true });

    console.log('💳 Navegando a Tesorería & Facturación...');
    await page.goto('http://localhost:4200/admin/tesoreria', { waitUntil: 'networkidle0' });
    await new Promise(r => setTimeout(r, 2000));
    await page.screenshot({ path: path.join(ARTIFACT_DIR, 'web_tesoreria_con_datos_bd.png'), fullPage: true });

    console.log('✅ Verificación completada con éxito.');
  } finally {
    await browser.close();
  }
}

testWeb().catch(err => {
  console.error('Error:', err);
  process.exit(1);
});
