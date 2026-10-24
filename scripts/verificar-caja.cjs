// Ejecutar únicamente sobre localhost:18080 y stockflow_qa_codex.
// Requiere dos turnos pendientes con vehículos QA-001 y QA-002, y ninguna caja abierta.
const { chromium } = require('playwright');
const fs = require('node:fs');
const assert = require('node:assert/strict');
(async () => {
  const output = 'target/evidencias-caja';
  fs.mkdirSync(output, { recursive: true });
  assert(process.env.APP_ADMIN_PASSWORD, 'Falta APP_ADMIN_PASSWORD');
  const browser = await chromium.launch({ headless: true });
  try {
    const page = await browser.newPage();
    const base = 'http://localhost:18080';
    await page.goto(base + '/login');
    await page.locator('#username').fill(process.env.APP_ADMIN_USERNAME || 'admin');
    await page.locator('#password').fill(process.env.APP_ADMIN_PASSWORD);
    await Promise.all([page.waitForURL(u => u.pathname === '/'), page.getByRole('button', { name: 'Iniciar sesión' }).click()]);
    await page.goto(base + '/caja');
    assert.equal(await page.locator('#saldoInicial').count(), 1, 'QA debe iniciar sin caja abierta');
    await page.locator('#saldoInicial').fill('50.00');
    await Promise.all([page.waitForURL(/\/caja\/\d+$/), page.getByRole('button', { name: 'Abrir caja', exact: true }).click()]);
    const urlCaja = page.url();
    async function movimiento(tipo, medio, importe, concepto) {
      await page.locator('#tipo').selectOption(tipo); await page.locator('#medio').selectOption(medio);
      await page.locator('#importe').fill(importe); await page.locator('#concepto').fill(concepto);
      await Promise.all([page.waitForNavigation(), page.getByRole('button', { name: 'Registrar movimiento', exact: true }).click()]);
      assert.match(await page.locator('.success-message').innerText(), /registrado/);
    }
    await movimiento('INGRESO', 'EFECTIVO', '5.00', 'Ingreso de prueba QA');
    await movimiento('EGRESO', 'EFECTIVO', '3.00', 'Gasto de prueba QA');
    await movimiento('INGRESO', 'YAPE', '15.00', 'Ingreso digital de prueba QA');
    for (const [placa, pagada, medio] of [['QA-001', 'true', 'YAPE'], ['QA-002', 'false', 'EFECTIVO']]) {
      await page.goto(base + '/salidas/nuevo');
      const option = page.locator('#turno option').filter({ hasText: placa });
      assert.equal(await option.count(), 1, `Se requiere un turno pendiente de ${placa}`);
      await page.locator('#turno').selectOption(await option.getAttribute('value'));
      await page.locator('#cantidadPasajeros').fill('4');
      await page.locator('#pagoAlSalir').selectOption(pagada); await page.locator('#medioPago').selectOption(medio);
      await Promise.all([page.waitForURL(u => u.pathname === '/salidas'), page.getByRole('button', { name: 'Registrar salida', exact: true }).click()]);
      assert.equal(await page.locator('.error-message').count(), 0);
    }
    const form = page.locator('tr').filter({ has: page.locator('[data-label="Vehículo"]').filter({ hasText: 'QA-002' }) }).locator('form[action$="/pago"]');
    assert.equal(await form.count(), 1);
    await form.locator('[name="medioPago"]').selectOption('EFECTIVO');
    page.once('dialog', d => d.accept());
    await Promise.all([page.waitForNavigation(), form.getByRole('button').click()]);
    await page.goto(urlCaja);
    const card = name => page.locator('.card').filter({ has: page.locator('span').filter({ hasText: new RegExp(`^${name}$`) }) }).locator('strong');
    assert.equal(await card('Efectivo esperado').innerText(), 'S/ 62.00');
    assert.equal(await card('Ingresos digitales').innerText(), 'S/ 25.00');
    let comprobaciones = 0;
    for (const route of [base + '/caja', urlCaja, base + '/salidas/nuevo']) {
      await page.goto(route);
      for (const width of [1920, 1366, 1024, 768, 390, 320]) {
        await page.setViewportSize({ width, height: 1000 });
        const size = await page.evaluate(() => ({ content: document.documentElement.scrollWidth, width: innerWidth }));
        assert(size.content <= size.width + 1, `Desborde a ${width}px en ${route}`);
        comprobaciones++;
        if (route === urlCaja && [1366, 390].includes(width)) await page.screenshot({ path: `${output}/abierta-${width}.png`, fullPage: true });
      }
    }
    await page.goto(urlCaja);
    await page.locator('#contado').fill('61.00'); await page.locator('#observacion').fill('Faltante de S/ 1.00 simulado en QA');
    page.once('dialog', d => d.accept());
    await Promise.all([page.waitForNavigation(), page.getByRole('button', { name: 'Cerrar caja definitivamente' }).click()]);
    assert.match(await page.locator('main').innerText(), /S\/ -1\.00/);
    assert.equal(await page.locator('form[data-cierre-caja]').count(), 0);
    await page.setViewportSize({ width: 1366, height: 1000 });
    await page.screenshot({ path: `${output}/cerrada.png`, fullPage: true });
    console.log(`Caja: apertura, tres movimientos, dos cobros, cierre y ${comprobaciones} verificaciones responsive correctas.`);
  } finally { await browser.close(); }
})().catch(error => { console.error(error.message); process.exitCode = 1; });
