// Verificación de interfaz únicamente en la instancia aislada localhost:18080.
const { chromium } = require('playwright');
const fs = require('node:fs');
const assert = require('node:assert/strict');
(async () => {
  assert(process.env.APP_ADMIN_PASSWORD, 'Falta APP_ADMIN_PASSWORD');
  const output = 'target/evidencias-interfaz'; fs.mkdirSync(output, { recursive: true });
  const browser = await chromium.launch({ headless: true });
  try {
    const page = await browser.newPage(); const base = 'http://localhost:18080'; const errores = [];
    page.on('pageerror', error => errores.push(error.message));
    await page.goto(base + '/login');
    await page.locator('#username').fill(process.env.APP_ADMIN_USERNAME || 'admin');
    await page.locator('#password').fill(process.env.APP_ADMIN_PASSWORD);
    await Promise.all([page.waitForURL(u => u.pathname === '/'), page.getByRole('button', { name: 'Iniciar sesión' }).click()]);
    assert.equal(await page.getByRole('heading', { name: 'Inicio', exact: true }).count(), 1);
    assert.equal(await page.locator('text=cuando implementemos').count(), 0);
    const rutas = ['/', '/turnos', '/turnos/nuevo', '/salidas', '/salidas/nuevo', '/conductores', '/conductores/nuevo',
      '/vehiculos', '/vehiculos/nuevo', '/encomiendas', '/encomiendas/nuevo', '/caja'];
    await page.goto(base + '/encomiendas');
    const envio = page.getByRole('link', { name: 'Ver detalle', exact: true }).first();
    if (await envio.count()) rutas.push(await envio.getAttribute('href'));
    await page.goto(base + '/caja');
    const caja = page.locator('a[href^="/caja/"]').first();
    if (await caja.count()) rutas.push(await caja.getAttribute('href'));
    let checks = 0;
    for (const ruta of rutas) {
      const response = await page.goto(base + ruta); assert.equal(response.status(), 200, `HTTP en ${ruta}`);
      for (const width of [1920, 1366, 1024, 768, 390, 320]) {
        await page.setViewportSize({ width, height: 950 });
        await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
        const medidas = await page.evaluate(() => ({ ancho: innerWidth, contenido: document.documentElement.scrollWidth,
          fuera: [...document.querySelectorAll('input:not([type="hidden"]), select, textarea')]
            .filter(e => e.getClientRects().length && (e.getBoundingClientRect().left < -1 || e.getBoundingClientRect().right > innerWidth + 1)).length }));
        assert(medidas.contenido <= medidas.ancho + 1, `Desborde en ${ruta} a ${width}px`);
        assert.equal(medidas.fuera, 0, `Campo fuera de pantalla en ${ruta}`); checks++;
        if ([1366,390].includes(width) && ['/', '/salidas', '/salidas/nuevo', '/encomiendas/nuevo'].includes(ruta)) {
          const nombre = ruta === '/' ? 'inicio' : ruta.slice(1).replaceAll('/', '-');
          await page.screenshot({ path: `${output}/${nombre}-${width}.png`, fullPage: true });
        }
      }
    }
    await page.setViewportSize({ width: 390, height: 844 }); await page.goto(base + '/');
    const toggle = page.getByRole('button', { name: 'Menú', exact: true });
    assert.equal(await toggle.getAttribute('aria-expanded'), 'false');
    await toggle.click(); assert.equal(await toggle.getAttribute('aria-expanded'), 'true');
    await page.getByRole('link', { name: 'Salidas', exact: true }).click();
    assert.equal(new URL(page.url()).pathname, '/salidas');
    await page.locator('details.filter-more > summary').click();
    assert(await page.locator('#placa').isVisible());
    await page.goto(base + '/salidas/nuevo');
    assert.equal(await page.locator('#campoMedioPago').isVisible(), false);
    await page.locator('#pagoAlSalir').selectOption('true'); assert(await page.locator('#medioPago').isVisible());
    await page.locator('#pagoAlSalir').selectOption('false'); assert.equal(await page.locator('#campoMedioPago').isVisible(), false);
    await page.goto(base + '/turnos/nuevo');
    const dia = await page.locator('#diaOperativo').getAttribute('data-fecha');
    assert.equal(await page.locator('#fecha').inputValue(), dia);
    assert.equal(await page.locator('#fecha').evaluate(input => input.readOnly), true);
    await page.locator('#numeroTurno').fill('999');
    const siguiente = new Date(dia + 'T17:00:00Z'); siguiente.setUTCDate(siguiente.getUTCDate() + 1);
    await page.clock.install({ time: siguiente });
    await page.evaluate(() => window.dispatchEvent(new Event('focus')));
    await page.getByRole('alert').filter({ hasText: 'Cambió el día en Pucallpa' }).waitFor();
    assert.equal(await page.locator('#numeroTurno').inputValue(), '999', 'No perder formulario al cambiar el día');
    assert.deepEqual(errores, [], 'Errores JavaScript');
    console.log(`${checks} comprobaciones responsive; menú móvil, filtros, medio de pago y cambio de día sin perder datos verificados. Sin errores JavaScript.`);
  } finally { await browser.close(); }
})().catch(error => { console.error(error.message); process.exitCode = 1; });
