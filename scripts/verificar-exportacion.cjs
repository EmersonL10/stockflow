// QA únicamente. Requiere la instancia aislada en localhost:18080 y Playwright.
const { chromium } = require('playwright');
const fs = require('node:fs');
const assert = require('node:assert/strict');
(async () => {
  assert(process.env.APP_ADMIN_PASSWORD, 'Falta APP_ADMIN_PASSWORD');
  const output = 'target/evidencias-librerias';
  fs.mkdirSync(output, { recursive: true });
  const browser = await chromium.launch({ headless: true });
  try {
    const page = await browser.newPage({ acceptDownloads: true });
    await page.goto('http://localhost:18080/login');
    await page.locator('#username').fill(process.env.APP_ADMIN_USERNAME || 'admin');
    await page.locator('#password').fill(process.env.APP_ADMIN_PASSWORD);
    await Promise.all([page.waitForURL(url => url.pathname === '/'), page.getByRole('button', { name: 'Iniciar sesión' }).click()]);
    await page.goto('http://localhost:18080/salidas?todasFechas=true');
    await page.setViewportSize({ width: 1366, height: 1000 });
    await page.screenshot({ path: `${output}/salidas-exportacion.png`, fullPage: true });
    const downloadEvent = page.waitForEvent('download');
    await page.getByRole('button', { name: 'Exportar a Excel' }).click();
    const download = await downloadEvent;
    assert.match(download.suggestedFilename(), /^salidas-\d{4}-\d{2}-\d{2}\.xlsx$/);
    await download.saveAs(`${output}/salidas-qa.xlsx`);
    assert(fs.statSync(`${output}/salidas-qa.xlsx`).size > 1000);
    for (const width of [1366, 1024, 768, 390, 320]) {
      await page.setViewportSize({ width, height: 900 });
      const size = await page.evaluate(() => ({ content: document.documentElement.scrollWidth, width: innerWidth }));
      assert(size.content <= size.width + 1, `Desborde a ${width}px`);
      if (width === 390) await page.screenshot({ path: `${output}/salidas-exportacion-movil.png`, fullPage: true });
    }
    console.log('Descarga autenticada y cinco tamaños responsive verificados en QA.');
  } finally { await browser.close(); }
})().catch(error => { console.error(error.message); process.exitCode = 1; });
