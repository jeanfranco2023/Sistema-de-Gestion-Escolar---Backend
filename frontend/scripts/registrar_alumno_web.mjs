import puppeteer from 'puppeteer-core';
import path from 'path';
import { mkdir } from 'node:fs/promises';

const CHROME_PATH = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
const ARTIFACT_DIR = path.resolve('artifacts');
const SCREENSHOT_PATH = path.join(ARTIFACT_DIR, 'registro_estudiante_web.png');

async function run() {
  if (!process.env.E2E_USERNAME || !process.env.E2E_PASSWORD || !/^\d{8}$/.test(process.env.E2E_STUDENT_DNI || '')) {
    throw new Error('Set E2E_USERNAME, E2E_PASSWORD and an 8-digit E2E_STUDENT_DNI.');
  }
  if (process.env.E2E_ALLOW_LIVE_WRITE !== '1') {
    throw new Error('Set E2E_ALLOW_LIVE_WRITE=1 to authorize creating a student.');
  }
  await mkdir(ARTIFACT_DIR, { recursive: true });
  console.log('🚀 Iniciando Chrome para realizar registro real desde la Web UI...');
  const browser = await puppeteer.launch({
    executablePath: CHROME_PATH,
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--window-size=1400,900']
  });

  try {
    const page = await browser.newPage();
    await page.setViewport({ width: 1400, height: 900 });

    console.log('🌐 Navegando a http://localhost:4200/auth/login...');
    await page.goto('http://localhost:4200/auth/login', { waitUntil: 'networkidle0' });

    console.log('🔑 Ingresando credenciales institucionales...');
    await page.waitForSelector('#username', { timeout: 10000 });
    await page.type('#username', process.env.E2E_USERNAME, { delay: 40 });
    await page.type('#password', process.env.E2E_PASSWORD, { delay: 40 });

    console.log('🖱️ Haciendo clic en "Ingresar al Sistema"...');
    await Promise.all([
      page.waitForNavigation({ waitUntil: 'networkidle0', timeout: 15000 }).catch(() => {}),
      page.click('button[type="submit"]')
    ]);

    await new Promise(r => setTimeout(r, 2000));
    console.log('📍 URL actual tras login:', page.url());

    console.log('📂 Navegando al módulo de Matrículas & Alumnos...');
    await page.goto('http://localhost:4200/admin/matriculas', { waitUntil: 'networkidle0' });
    await new Promise(r => setTimeout(r, 2000));

    console.log('🔘 Abriendo modal "Registrar Alumno"...');
    await page.waitForSelector('#btn-abrir-registro-alumno', { timeout: 10000 });
    await page.click('#btn-abrir-registro-alumno');

    console.log('📝 Llenando formulario web de alta de estudiante...');
    await page.waitForSelector('#nuevo-alumno-numero-doc', { timeout: 10000 });

    const numDoc = process.env.E2E_STUDENT_DNI;
    await page.select('#nuevo-alumno-tipo-doc', 'DNI');
    await page.type('#nuevo-alumno-numero-doc', numDoc, { delay: 30 });
    await page.type('#nuevo-alumno-nombres', 'Mateo Alejandro', { delay: 30 });
    await page.type('#nuevo-alumno-ape-paterno', 'Quispe', { delay: 30 });
    await page.type('#nuevo-alumno-ape-materno', 'Mendoza', { delay: 30 });
    
    // Setting date input via evaluate for reliability
    await page.evaluate(() => {
      const el = document.getElementById('nuevo-alumno-fecha-nac');
      if (el) {
        el.value = '2012-05-15';
        el.dispatchEvent(new Event('input', { bubbles: true }));
        el.dispatchEvent(new Event('change', { bubbles: true }));
      }
    });

    await page.select('#nuevo-alumno-genero', 'M');
    await page.select('#nuevo-alumno-grupo-sang', 'O+');
    await page.type('#nuevo-alumno-siagie', `2026-${numDoc}`, { delay: 20 });
    await page.type('#nuevo-alumno-alergias', 'Sin alergias conocidas', { delay: 20 });

    console.log('💾 Enviando formulario web a la base de datos...');
    await page.click('#btn-guardar-alumno');

    console.log('⏳ Esperando confirmación y renderizado en tabla web...');
    await page.waitForFunction(
      (dni) => document.body.innerText.includes(dni) || document.body.innerText.includes('Mateo Alejandro'),
      { timeout: 15000 },
      numDoc
    );

    await new Promise(r => setTimeout(r, 2000));

    console.log(`📸 Capturando screenshot web en ${SCREENSHOT_PATH}...`);
    await page.screenshot({ path: SCREENSHOT_PATH, fullPage: true });

    console.log('✅ ¡Registro de estudiante desde la web UI completado con éxito!');
    console.log(`Estudiante: Mateo Alejandro Quispe Mendoza (DNI: ${numDoc}) visible en la tabla web.`);
  } catch (err) {
    console.error('❌ Error durante el registro web:', err);
    throw err;
  } finally {
    await browser.close();
  }
}

run().catch(err => {
  console.error(err);
  process.exit(1);
});
