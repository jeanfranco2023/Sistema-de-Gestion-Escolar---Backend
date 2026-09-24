import assert from 'node:assert/strict';
import puppeteer from 'puppeteer-core';

const frontend = process.env.E2E_FRONT_BASE ?? 'http://localhost:4201';
assert.ok(['localhost', '127.0.0.1'].includes(new URL(frontend).hostname));

const browser = await puppeteer.launch({
  executablePath: process.env.E2E_CHROME ?? 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
  headless: true,
  args: ['--no-sandbox', '--disable-dev-shm-usage']
});

try {
  for (const role of ['DIRECCION', 'APODERADO']) {
    const page = await browser.newPage();
    let postedBody;
    const obligation = {
      id: 42, matriculaId: 8, conceptoId: 2, tipoConcepto: 'PENSION', numeroCuota: 1,
      descripcion: 'Pensión de prueba', fechaVencimiento: '2026-10-01', montoBase: 100,
      montoMora: 0, montoDescuento: 0, totalPagado: 0, saldoPendiente: 100,
      estado: 'PENDIENTE'
    };
    await page.setRequestInterception(true);
    page.on('request', request => {
      const url = new URL(request.url());
      if (!url.pathname.startsWith('/api/v1/')) return request.continue();
      const json = body => request.respond({ status: 200, contentType: 'application/json', body: JSON.stringify(body) });
      if (url.pathname.endsWith('/tesoreria/conceptos')) return json([]);
      if (url.pathname.endsWith('/tesoreria/obligaciones') || url.pathname.endsWith('/tesoreria/mis-obligaciones')) return json([obligation]);
      if (url.pathname.endsWith('/tesoreria/pagos/mercadopago/preferencia')) {
        postedBody = JSON.parse(request.postData());
        return json({
          preferenceId: 'test-preference',
          sandboxInitPoint: 'https://sandbox.mercadopago.com.pe/checkout/v1/redirect?pref_id=test-preference',
          initPoint: '', publicKey: 'test-public-key'
        });
      }
      return request.respond({ status: 404, body: '' });
    });

    await page.goto(`${frontend}/auth/login`, { waitUntil: 'domcontentloaded' });
    await page.evaluate(currentRole => {
      localStorage.setItem('accessToken', 'ui-test-token');
      localStorage.setItem('user', JSON.stringify({ id: 1, username: 'ui-test', roles: [{ codigo: currentRole }] }));
      localStorage.removeItem('refreshToken');
    }, role);
    await page.goto(`${frontend}/admin/tesoreria`, { waitUntil: 'domcontentloaded' });
    await page.waitForFunction(() => [...document.querySelectorAll('button')].some(button => button.textContent.includes('Pagar con Mercado Pago')));
    await page.evaluate(() => [...document.querySelectorAll('button')].find(button => button.textContent.includes('Pagar con Mercado Pago')).click());
    await page.waitForFunction(() => !!document.querySelector('a[href*="sandbox.mercadopago.com.pe"]'));
    assert.deepEqual(postedBody, { obligacionPagoId: 42 });
    assert.ok((await page.$eval('a[href*="sandbox.mercadopago.com.pe"]', anchor => anchor.href)).startsWith('https://sandbox.mercadopago.com.pe/'));
    await page.close();
    console.log(`PASS checkout UI ${role}`);
  }
} finally {
  await browser.close();
}
