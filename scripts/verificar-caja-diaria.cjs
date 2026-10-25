// Solo QA aislada en localhost:18080, sin caja abierta al comenzar.
const {chromium}=require('playwright');
const assert=require('node:assert/strict');
const fs=require('node:fs');
(async()=>{
 const browser=await chromium.launch({headless:true});
 try {
  const page=await browser.newPage(); const base='http://localhost:18080'; const errores=[];
  page.on('pageerror', e=>errores.push(e.message));
  fs.mkdirSync('target/evidencias-caja-simple',{recursive:true});
  await page.goto(base+'/login');await page.locator('#username').fill(process.env.APP_ADMIN_USERNAME||'admin');
  await page.locator('#password').fill(process.env.APP_ADMIN_PASSWORD);
  await Promise.all([page.waitForURL(u=>u.pathname==='/'),page.getByRole('button',{name:'Iniciar sesión'}).click()]);
  await page.goto(base+'/caja');assert.equal(await page.locator('#saldoInicial').count(),1,'QA requiere caja cerrada');
  await page.locator('#saldoInicial').fill('10.00');
  await Promise.all([page.waitForURL(/\/caja\/\d+$/),page.getByRole('button',{name:'Abrir caja',exact:true}).click()]);
  const url=page.url();
  for(const [tipo,medio,importe] of [['INGRESO','EFECTIVO','30.00'],['EGRESO','EFECTIVO','30.00'],['INGRESO','YAPE','70.00'],['EGRESO','PLIN','10.00']]){
   await page.locator('[data-movimientos-diarios] > summary').click();
   await page.locator('#tipo').selectOption(tipo);await page.locator('#medio').selectOption(medio);
   await page.locator('#importe').fill(importe);await page.locator('#concepto').fill('Verificación de total QA');
   await Promise.all([page.waitForNavigation(),page.getByRole('button',{name:'Registrar movimiento'}).click()]);
  }
  assert.equal(await page.locator('.caja-total').innerText(),'S/ 70.00');
  assert.match(await page.locator('.caja-saldo').innerText(),/S\/ 10.00 en efectivo \+ S\/ 60.00/);
  for(const width of [1366,390]){
   await page.setViewportSize({width,height:900});
   await page.evaluate(()=>new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r))));
   assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1));
   await page.screenshot({path:`target/evidencias-caja-simple/resumen-${width}.png`,fullPage:true});
  }
  await page.getByRole('link',{name:'Contar efectivo y cerrar caja'}).click();
  assert(await page.locator('#cierre').evaluate(e=>e.open));
  await page.locator('#contado').fill('10.00');assert.match(await page.locator('#resultadoConteo').innerText(),/coincide/);
  assert.equal(await page.locator('#observacion').isVisible(),false);
  await page.locator('#contado').fill('9.00');assert.match(await page.locator('#resultadoConteo').innerText(),/Faltan S\/ 1.00/);
  assert(await page.locator('#observacion').evaluate(e=>e.required));
  // Omitir validación cliente: comprobar respuesta segura y recuperación de valores del servidor.
  await Promise.all([page.waitForNavigation(),page.locator('[data-cierre-caja]').evaluate(f=>f.submit())]);
  assert.equal(await page.locator('#contado').inputValue(),'9.00');
  assert(await page.locator('#cierre').evaluate(e=>e.open));
  assert.match(await page.getByRole('alert').innerText(),/Faltan S\/ 1.00/);
  const dia=await page.locator('#diaOperativo').getAttribute('data-fecha');
  await page.locator('#contado').fill('9.00');
  const siguiente=new Date(dia+'T17:00:00Z');siguiente.setUTCDate(siguiente.getUTCDate()+1);
  await page.clock.install({time:siguiente});await page.evaluate(()=>window.dispatchEvent(new Event('focus')));
  assert.equal(await page.locator('[data-movimientos-diarios]').isVisible(),false);
  assert.equal(await page.locator('#contado').inputValue(),'9.00');
  await page.locator('#observacion').fill('Faltante de un sol confirmado en QA');page.once('dialog',d=>d.accept());
  await Promise.all([page.waitForNavigation(),page.getByRole('button',{name:'Cerrar caja definitivamente'}).click()]);
  assert.equal(await page.locator('.caja-total').innerText(),'S/ 69.00');
  assert.match(await page.locator('main').innerText(),/Efectivo confirmado al cerrar/);
  assert.equal(await page.locator('[data-cierre-caja]').count(),0);assert.deepEqual(errores,[]);
  await page.screenshot({path:'target/evidencias-caja-simple/cierre.png',fullPage:true});
  console.log('Caja simple: total 70, efectivo 10, digital 60; diferencia guiada, recuperación del conteo, corte visual de día y cierre real 69 verificados.');
 }finally{await browser.close();}
})().catch(e=>{console.error(e.message);process.exitCode=1;});
