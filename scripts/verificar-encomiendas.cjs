// Solo QA: localhost:18080, sin caja abierta y con una salida en ruta de hoy Pucallpa - Honoria.
const { chromium } = require('playwright');
const fs = require('node:fs');
const assert = require('node:assert/strict');
(async () => {
  assert(process.env.APP_ADMIN_PASSWORD, 'Falta APP_ADMIN_PASSWORD');
  const output = 'target/evidencias-encomiendas'; fs.mkdirSync(output, { recursive: true });
  const browser = await chromium.launch({ headless: true });
  try {
    const page = await browser.newPage(); const base = 'http://localhost:18080';
    await page.goto(base + '/login');
    await page.locator('#username').fill(process.env.APP_ADMIN_USERNAME || 'admin');
    await page.locator('#password').fill(process.env.APP_ADMIN_PASSWORD);
    await Promise.all([page.waitForURL(u => u.pathname === '/'), page.getByRole('button', { name: 'Iniciar sesión' }).click()]);
    await page.goto(base + '/caja');
    assert.equal(await page.locator('#saldoInicial').count(), 1, 'Se requiere QA sin caja abierta');
    await page.locator('#saldoInicial').fill('0.00');
    await Promise.all([page.waitForURL(/\/caja\/\d+$/), page.getByRole('button', { name: 'Abrir caja', exact: true }).click()]);
    const cajaUrl = page.url();
    async function crear(ruta) {
      await page.goto(base + '/encomiendas/nuevo');
      for (const [id, valor] of Object.entries({ remitente: 'Remitente QA', documentoRemitente: '00000001',
        telefonoRemitente: '900000001', destinatario: 'Destinatario QA', documentoDestinatario: '00000002',
        telefonoDestinatario: '900000002', ruta, descripcion: 'Libros de prueba QA', bultos: '2', pesoKg: '1.250', importe: '25.00' })) {
        await page.locator('#' + id).fill(valor);
      }
      await Promise.all([page.waitForURL(/\/encomiendas\/\d+$/), page.getByRole('button', { name: 'Guardar encomienda' }).click()]);
      assert.equal(await page.locator('.error-message').count(), 0);
      return page.url();
    }
    async function confirmar(nombre) {
      page.once('dialog', d => d.accept());
      await Promise.all([page.waitForNavigation(), page.getByRole('button', { name: nombre, exact: true }).click()]);
    }
    const envioUrl = await crear('Pucallpa - Honoria');
    const codigo = await page.locator('h1').innerText();
    await page.getByRole('link', { name: 'Editar datos' }).click();
    await page.locator('#descripcion').fill('Libros revisados antes del pago QA');
    await Promise.all([page.waitForURL(envioUrl), page.getByRole('button', { name: 'Guardar encomienda' }).click()]);
    await page.locator('#medio').selectOption('PLIN'); await confirmar('Registrar cobro');
    assert.match(await page.locator('.success-message').innerText(), /Caja/);
    await page.goto(cajaUrl);
    assert.match(await page.locator('main').innerText(), /S\/ 25\.00/);
    assert.equal(await page.getByRole('link', { name: codigo, exact: true }).count(), 1);
    await page.goto(envioUrl);
    const opciones = page.locator('#salidaId option:not([value=""])');
    assert((await opciones.count()) > 0, 'Falta salida en ruta de hoy coincidente en QA');
    await page.locator('#salidaId').selectOption(await opciones.first().getAttribute('value'));
    await confirmar('Confirmar despacho'); await confirmar('Confirmar llegada a destino');
    await page.locator('#receptor').fill('Destinatario QA'); await page.locator('#documento').fill('00000999');
    await confirmar('Confirmar entrega'); assert.match(await page.locator('.error-message').innerText(), /coincidir/);
    await page.locator('#receptor').fill('Destinatario QA'); await page.locator('#documento').fill('00000002');
    await confirmar('Confirmar entrega');
    assert.match(await page.locator('main').innerText(), /Entregada/);
    assert.equal(await page.getByRole('button', { name: 'Confirmar entrega' }).count(), 0);
    await page.setViewportSize({ width: 1366, height: 1000 });
    await page.screenshot({ path: `${output}/entregada.png`, fullPage: true });
    await page.emulateMedia({ media: 'print' });
    assert.equal(await page.locator('.sidebar').isVisible(), false);
    await page.screenshot({ path: `${output}/constancia-impresion.png`, fullPage: true });
    await page.emulateMedia({ media: 'screen' });
    const canceladaUrl = await crear('Honoria - Pucallpa');
    await page.locator('#motivo').fill('Cancelación de prueba antes del cobro y despacho');
    await confirmar('Cancelar encomienda'); assert.match(await page.locator('main').innerText(), /Cancelada/);
    let comprobaciones = 0;
    for (const route of [base + '/encomiendas', base + '/encomiendas/nuevo', envioUrl, canceladaUrl]) {
      await page.goto(route);
      for (const width of [1920, 1366, 1024, 768, 390, 320]) {
        await page.setViewportSize({ width, height: 1000 });
        const size = await page.evaluate(() => ({ content: document.documentElement.scrollWidth, width: innerWidth }));
        assert(size.content <= size.width + 1, `Desborde a ${width}px en ${route}`); comprobaciones++;
        if ([1366, 390].includes(width) && route.endsWith('/nuevo')) await page.screenshot({ path: `${output}/formulario-${width}.png`, fullPage: true });
        if (width === 1366 && route.endsWith('/encomiendas')) await page.screenshot({ path: `${output}/listado.png`, fullPage: true });
      }
    }
    await page.goto(cajaUrl); await page.locator('#contado').fill('0.00');
    await page.locator('#observacion').fill('Cierre QA: solo cobro digital de encomienda');
    await confirmar('Cerrar caja definitivamente');
    assert.equal(await page.locator('.error-message').count(), 0);
    console.log(`Encomiendas: registro, edición, cobro PLIN, despacho, destino, entrega, cancelación, impresión y ${comprobaciones} verificaciones responsive correctas.`);
  } finally { await browser.close(); }
})().catch(error => { console.error(error.message); process.exitCode = 1; });
