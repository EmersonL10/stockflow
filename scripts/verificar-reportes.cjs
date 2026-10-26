// Ejecutar solo contra la instancia local de pruebas en stockflow_qa, puerto 18081.
const { chromium } = require('playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');

(async () => {
    assert(process.env.APP_ADMIN_PASSWORD, 'Configure APP_ADMIN_PASSWORD por entorno');
    const base = 'http://localhost:18081';
    const dir = 'target/evidencias-reportes';
    fs.mkdirSync(dir, { recursive: true });
    const browser = await chromium.launch({ headless: true, channel: 'chrome' });
    try {
        const page = await browser.newPage({ viewport: { width: 1366, height: 950 } });
        const errores = [];
        page.on('pageerror', error => errores.push(error.message));
        await page.goto(base + '/login');
        await page.locator('#username').fill(process.env.APP_ADMIN_USERNAME || 'admin');
        await page.locator('#password').fill(process.env.APP_ADMIN_PASSWORD);
        await Promise.all([page.waitForURL(u => u.pathname === '/'), page.getByRole('button', { name: 'Iniciar sesión' }).click()]);

        const id = String(Date.now()).slice(-8), placa = 'R' + id;
        async function formulario(ruta, valores, boton, destino) {
            await page.goto(base + ruta);
            for (const [campo, valor] of Object.entries(valores)) await page.locator('#' + campo).fill(valor);
            await Promise.all([page.waitForURL(u => u.pathname === destino), page.getByRole('button', { name: boton, exact: true }).click()]);
        }
        await formulario('/vehiculos/nuevo', { placa, marca: 'PRUEBA', modelo: 'REPORTE', anio: '2020',
            color: 'BLANCO', capacidadPasajeros: '5', numeroTuc: 'R-' + id, propietario: 'Propietario de prueba' }, 'Guardar vehículo', '/vehiculos');
        const fila = page.locator('tbody tr').filter({ hasText: placa });
        assert.match(await fila.innerText(), /Propietario de prueba/);
        const historial = await fila.getByRole('link', { name: 'Historial', exact: true }).getAttribute('href');
        await formulario('/conductores/nuevo', { dni: id, nombres: 'Prueba', apellidos: 'Reportes ' + id,
            licencia: 'R-' + id }, 'Guardar conductor', '/conductores');
        await page.goto(base + '/turnos/nuevo');
        const vehiculo = await page.locator('#vehiculo option').filter({ hasText: placa }).getAttribute('value');
        const conductor = await page.locator('#conductor option').filter({ hasText: 'Reportes ' + id }).getAttribute('value');
        await page.locator('#vehiculo').selectOption(vehiculo); await page.locator('#conductor').selectOption(conductor);
        await page.locator('#rutaOperativa').fill('Pucallpa - Honoria');
        await page.locator('#numeroTurno').fill(id); await page.locator('#horaProgramada').fill('08:00');
        const hoy = await page.locator('#fecha').inputValue();
        await Promise.all([page.waitForURL(u => u.pathname === '/turnos'), page.getByRole('button', { name: 'Guardar llegada', exact: true }).click()]);
        await page.goto(base + '/caja');
        if (await page.locator('#saldoInicial').count()) {
            await page.locator('#saldoInicial').fill('0.00');
            await Promise.all([page.waitForNavigation(), page.getByRole('button', { name: 'Abrir caja', exact: true }).click()]);
        }
        await page.goto(base + '/salidas/nuevo');
        const turno = await page.locator('#turno option').filter({ hasText: placa }).getAttribute('value');
        await page.locator('#turno').selectOption(turno);
        await page.locator('#cantidadPasajeros').fill('4');
        await page.locator('#pagoAlSalir').selectOption('true');
        await Promise.all([page.waitForURL(u => u.pathname === '/salidas'), page.getByRole('button', { name: 'Registrar salida', exact: true }).click()]);
        await page.goto(base + historial); assert.match(await page.locator('main').innerText(), new RegExp('Reportes ' + id));
        await page.screenshot({ path: dir + '/historial-vehiculo.png', fullPage: true });
        let comprobaciones = 0;
        const rutas = ['/', '/vehiculos', '/vehiculos/nuevo', historial,
            ...['SALIDAS', 'ENCOMIENDAS', 'CAJA'].map(m => `/reportes?modulo=${m}&desde=${hoy}&hasta=${hoy}`)];
        for (const ruta of rutas) {
            const response = await page.goto(base + ruta); assert.equal(response.status(), 200, ruta);
            for (const width of [1366, 768, 390, 320]) {
                await page.setViewportSize({ width, height: 950 });
                await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
                const ancho = await page.evaluate(() => ({ contenido: document.documentElement.scrollWidth, pantalla: innerWidth }));
                assert(ancho.contenido <= ancho.pantalla + 1, `Desborde en ${ruta} a ${width}px`);
                if (width === 390 && ruta.includes('modulo=CAJA')) await page.screenshot({ path: dir + '/reporte-movil.png', fullPage: true });
                comprobaciones++;
            }
        }
        await page.setViewportSize({ width: 1366, height: 950 });
        for (const modulo of ['SALIDAS', 'ENCOMIENDAS', 'CAJA']) {
            await page.goto(base + `/reportes?modulo=${modulo}&desde=${hoy}&hasta=${hoy}`);
            await page.screenshot({ path: dir + '/reporte-' + modulo.toLowerCase() + '.png', fullPage: true });
            const descarga = page.waitForEvent('download');
            await page.getByRole('button', { name: 'Descargar Excel' }).click();
            assert.equal(await (await descarga).failure(), null);
        }
        await page.goto(base + '/'); await page.screenshot({ path: dir + '/inicio.png', fullPage: true });
        assert.equal(await page.getByText('Ingresos de hoy', { exact: true }).count(), 1);
        assert.equal(await page.getByText('Vehículos activos', { exact: true }).count(), 1);
        assert.deepEqual(errores, []);
        const resultado = { fecha: new Date().toISOString(), entorno: 'stockflow_qa', puerto: 18081,
            comprobacionesResponsive: comprobaciones, descargasExcel: 3, erroresJavascript: errores, placa };
        fs.writeFileSync(dir + '/resultados.json', JSON.stringify(resultado, null, 2));
        console.log(JSON.stringify(resultado));
    } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
