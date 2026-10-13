// Ejecutar únicamente con la instancia y los datos aislados de QA.
const { chromium } = require('playwright');
const fs = require('node:fs');
const assert = require('node:assert/strict');
(async () => {
  const base = 'http://localhost:18080';
  assert(process.env.APP_ADMIN_PASSWORD, 'Falta APP_ADMIN_PASSWORD');
  fs.mkdirSync('target/qa-results', { recursive: true });
  const browser = await chromium.launch({ headless: true });
  try {
    const page = await browser.newPage();
    await page.goto(base + '/login');
    await page.setViewportSize({ width: 390, height: 844 });
    await page.screenshot({ path: 'target/qa-results/login-mobile.png', fullPage: true });
    await page.locator('#username').fill(process.env.APP_ADMIN_USERNAME || 'admin');
    await page.locator('#password').fill(process.env.APP_ADMIN_PASSWORD);
    await Promise.all([page.waitForURL(url => !url.pathname.includes('login')), page.getByRole('button', { name: 'Iniciar sesión' }).click()]);
    for (const [plate, paid] of [['QA-001', 'true'], ['QA-002', 'false']]) {
      await page.goto(base + '/salidas/nuevo');
      const option = page.locator('#turno option').filter({ hasText: plate });
      if (await option.count()) {
        await page.locator('#turno').selectOption(await option.getAttribute('value'));
        await page.locator('#cantidadPasajeros').fill('5');
        await page.locator('#pagoAlSalir').selectOption(paid);
        await Promise.all([page.waitForURL(url => url.pathname === '/salidas'), page.getByRole('button', { name: 'Registrar salida', exact: true }).click()]);
      }
    }
    await page.goto(base + '/salidas');
    const pending = page.locator('form[action$="/pago"] button');
    if (await pending.count()) {
      page.once('dialog', dialog => dialog.accept());
      await Promise.all([page.waitForURL(url => url.pathname === '/salidas'), pending.first().click()]);
    }
    assert.equal(await page.locator('form[action$="/pago"]').count(), 0);
    assert.match(await page.locator('.cards-grid, .cards').allTextContents().then(a => a.join(' ')), /20/);
    let checks = 0;
    for (const route of ['/', '/salidas', '/salidas/nuevo', '/turnos', '/turnos/nuevo', '/conductores', '/conductores/nuevo', '/vehiculos', '/vehiculos/nuevo']) {
      for (const width of [1920, 1366, 1024, 768, 390, 320]) {
        await page.setViewportSize({ width, height: 900 });
        const response = await page.goto(base + route);
        assert(response.status() < 400, `${route}: HTTP ${response.status()}`);
        const dimensions = await page.evaluate(() => ({ page: document.documentElement.scrollWidth, viewport: innerWidth }));
        assert(dimensions.page <= dimensions.viewport + 1, `${route} ${width}px: desborde ${dimensions.page}`);
        checks++;
        if (route === '/salidas' && [1366, 390].includes(width)) await page.screenshot({ path: `target/qa-results/salidas-${width}.png`, fullPage: true });
      }
    }
    console.log(`Flujo de alta y cobro verificado; ${checks} comprobaciones responsive correctas.`);
  } finally { await browser.close(); }
})().catch(error => { console.error(error.message); process.exitCode = 1; });
