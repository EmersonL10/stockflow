// Solo entorno QA aislado en localhost:18080.
const {chromium}=require('playwright');const assert=require('node:assert/strict');const fs=require('node:fs');
(async()=>{const b=await chromium.launch({headless:true});try{
 const p=await b.newPage({viewport:{width:1366,height:950}});const base='http://localhost:18080';const dir='target/entrega-final/evidencias-registros';fs.mkdirSync(dir,{recursive:true});
 await p.goto(base+'/login');await p.locator('#username').fill(process.env.APP_ADMIN_USERNAME||'admin');await p.locator('#password').fill(process.env.APP_ADMIN_PASSWORD);
 await Promise.all([p.waitForURL(u=>u.pathname==='/'),p.getByRole('button',{name:'Iniciar sesión'}).click()]);
 const id=String(Date.now()).slice(-8);const placa='Q'+id;const resultados=[];
 for(const [mod,key,data,editField,editValue,button] of [
 ['conductores',id,{dni:id,nombres:'Registro QA',apellidos:'Verificacion academica',licencia:'QA-LIC-'+id,telefono:'900000001'},'nombres','Registro QA editado','Guardar conductor'],
 ['vehiculos',placa,{placa,marca:'PRUEBA QA',modelo:'REGISTRO',anio:'2022',color:'BLANCO',capacidadPasajeros:'5',numeroTuc:'QA-TUC-'+id},'color','AZUL','Guardar vehículo']]){
  await p.goto(base+'/'+mod+'/nuevo');for(const [field,value] of Object.entries(data))await p.locator('#'+field).fill(value);
  await Promise.all([p.waitForNavigation(),p.getByRole('button',{name:button}).click()]);assert.equal(new URL(p.url()).pathname,'/'+mod);
  let row=p.locator('tbody tr').filter({hasText:key});assert.equal(await row.count(),1);assert.match(await row.innerText(),/Activo/);
  resultados.push({modulo:mod,caso:'registro',resultado:'aprobado'});
  await row.getByRole('link',{name:'Editar',exact:true}).click();await p.locator('#'+editField).fill(editValue);
  await Promise.all([p.waitForNavigation(),p.getByRole('button',{name:button}).click()]);row=p.locator('tbody tr').filter({hasText:key});assert.match(await row.innerText(),new RegExp(editValue));
  resultados.push({modulo:mod,caso:'edicion',resultado:'aprobado'});
  await p.goto(base+'/'+mod+'/nuevo');for(const [field,value] of Object.entries(data))await p.locator('#'+field).fill(value);
  await Promise.all([p.waitForNavigation(),p.getByRole('button',{name:button}).click()]);assert.match(await p.getByRole('alert').innerText(),/Ya existe/);
  await p.screenshot({path:dir+'/'+mod+'-duplicado.png',fullPage:true});resultados.push({modulo:mod,caso:'duplicado_rechazado',resultado:'aprobado'});
  await p.goto(base+'/'+mod);row=p.locator('tbody tr').filter({hasText:key});assert.equal(await row.count(),1);
  p.once('dialog',d=>d.accept());await Promise.all([p.waitForNavigation(),row.getByRole('button',{name:'Inactivar',exact:true}).click()]);
  row=p.locator('tbody tr').filter({hasText:key});assert.match(await row.innerText(),/Inactivo/);await row.screenshot({path:dir+'/'+mod+'-inactivo.png'});
  resultados.push({modulo:mod,caso:'inactivacion',resultado:'aprobado'});
 }
 fs.writeFileSync(dir+'/resultados.json',JSON.stringify({fecha:new Date().toISOString(),entorno:'stockflow_qa_codex',resultados},null,2));
 console.log('8 casos QA aprobados: registro, edición, duplicados e inactivación de conductores y vehículos.');
}finally{await b.close();}})().catch(e=>{console.error(e.message);process.exitCode=1;});
