# Caja operativa de StockFlow

## Alcance y acceso

Caja administra una única sesión abierta para la agencia, en soles. Administrador y Caja pueden consultar, abrir, registrar movimientos y cerrar. Operativo continúa registrando salidas pendientes, pero no accede a Caja ni cobra cuotas. Los permisos se aplican tanto en HTTP como en el servicio.

La configuración de desarrollo crea dos tablas adicionales al iniciar: `caja_sesiones` y `caja_movimientos`. No se reemplaza `stockflow_db` ni se eliminan los pagos antiguos. El desarrollo usa `ddl-auto=update`; antes de un despliegue productivo corresponde establecer migraciones y respaldos.

## Secuencia de uso

1. Ingresar a Caja y abrir con el efectivo físico inicial. No incluir saldos de billeteras o bancos.
2. Registrar las salidas como pendientes o pagadas. Para cobrar se requiere una caja abierta de la fecha actual en Lima.
3. Seleccionar el medio: Efectivo, Yape, Plin o Transferencia. La aplicación registra el medio declarado; no verifica operaciones en esos proveedores.
4. Los pagos de Salidas generan automáticamente un ingreso vinculado a la salida y a la caja. No volver a ingresarlos manualmente.
   Los cobros completos de Encomiendas también generan un ingreso automático con su propio origen, código y medio de pago; no volver a ingresarlos manualmente.
5. Usar movimientos manuales para otros ingresos o egresos y especificar concepto e importe.
6. Revisar el resumen, contar el efectivo y registrar el cierre. Si hay diferencia, el motivo es obligatorio.
7. Consultar el cierre en el historial. No se reabre ni se modifican movimientos confirmados mediante la aplicación. Después puede abrirse otra sesión, incluso el mismo día.

Si una caja quedó abierta en una fecha anterior, debe cerrarse antes de registrar nuevos movimientos. No se cambia automáticamente su fecha ni se mueve dinero entre sesiones. El cierre guarda fecha, responsable, efectivo esperado, contado y diferencia.

## Importes

`Efectivo esperado = saldo inicial + ingresos en efectivo - egresos en efectivo`.

`Diferencia = efectivo contado - efectivo esperado`.

Una diferencia positiva indica sobrante; una negativa, faltante. Los ingresos y egresos digitales aparecen por separado y no alteran el arqueo físico. No se valida el saldo disponible de cuentas bancarias o billeteras, porque su saldo inicial no se administra aquí. Un egreso en efectivo no puede superar el efectivo disponible de esa sesión.

Los importes se calculan con BigDecimal sin redondear entradas silenciosamente. Apertura y movimientos aceptan hasta 8 enteros y 2 decimales; el contado admite hasta 12 enteros para contemplar acumulación de varios movimientos.

## Integración transaccional con Salidas

- Si no existe una caja abierta de hoy, el cobro se rechaza. La transacción revierte también el cambio a PAGADA y, en un alta pagada, la nueva salida y el cambio de turno.
- Cada movimiento de cuota referencia una salida, protegida por una restricción única. La edición general de la salida no genera otro movimiento.
- Cobrar una salida anterior afecta la caja en la que se recibe el pago, no la fecha en que viajó el vehículo.
- Los pagos preexistentes sin movimiento se cuentan en una advertencia de Caja. No se importan como ingreso de una caja nueva ni se vuelven pendientes automáticamente.
- La cancelación de una salida pagada sigue bloqueada: no hay devolución ni regularización automática implementada.

## Concurrencia y formularios

Una clave única nullable permite una sola sesión activa. Las operaciones bloquean la sesión con PESSIMISTIC_WRITE antes de modificarla. El cierre y los movimientos comparten ese bloqueo; un pago que llega después del cierre revierte y queda pendiente.

Los movimientos manuales incluyen un UUID con restricción única para rechazar reenvíos del mismo formulario. No evita dos registros introducidos manualmente como operaciones diferentes: el operador debe verificar el concepto y el origen.

Los formularios usan DTOs, no entidades financieras. El usuario no decide fecha, responsable, efectivo esperado ni diferencia. CSRF se mantiene habilitado. No se registran en el log los conceptos libres ni los datos personales de los movimientos.

## Verificación

`CajaIntegrationTests` comprueba apertura, movimientos de efectivo y digitales, cierre, diferencias, permisos, CSRF, importes, reenvíos, pagos históricos, rollback, cierre de caja anterior y concurrencia de apertura/cobro/cierre. `SalidasIntegrationTests` ahora prepara una caja de prueba antes de los cobros.

```powershell
.\mvnw.cmd test
```

Las pruebas usan exclusivamente `stockflow_test_codex` y recrean sus tablas. La verificación del navegador usa localhost:18080 y `stockflow_qa_codex` con datos sintéticos. `scripts/verificar-caja.cjs` necesita dos turnos pendientes QA-001 y QA-002, y ninguna caja abierta. Genera capturas en `target/evidencias-caja`.

En QA se verificó: apertura S/ 50, ingreso manual efectivo S/ 5, egreso efectivo S/ 3, ingreso digital S/ 15, cobro Yape S/ 10 y cobro efectivo S/ 10. Resultado: efectivo esperado S/ 62, ingresos digitales S/ 25. El cierre con S/ 61 registra diferencia S/ -1 y motivo. Se verificaron 18 combinaciones de pantalla y ancho (1920, 1366, 1024, 768, 390 y 320 px).

Suite final del 24/10/2026: 49 pruebas aprobadas, sin fallos, errores ni omisiones. Incluye el reintento del alta pagada tras rollback y el cierre con saldo acumulado superior al máximo de un movimiento individual.

## Límites

Esta es Caja operativa por sesión, no un sistema contable completo. No implementa comprobantes fiscales, conciliación bancaria automática, devoluciones, anulación o reversión de movimientos, cuentas por sede, conciliación con proveedores ni auditoría de cambios directos en MySQL. No debe interpretarse el ingreso total como utilidad. Los movimientos de una sesión se cargan completos para consulta; el historial se pagina de 20 en 20.

La integración con Encomiendas se describe en [su documentación](encomiendas.md). El cobro de la encomienda y el de la cuota de Salida son conceptos distintos; cada uno tiene su referencia única y se registra solo al cobrarlo efectivamente.

## Desglose de movimientos

Ingresos totales y egresos totales incluyen efectivo y digital. Saldo neto de movimientos = ingresos totales − egresos totales; no incluye el efectivo inicial y no representa utilidad contable. Para el arqueo se conserva efectivo esperado = inicial + ingresos en efectivo − egresos en efectivo. El saldo digital mostrado corresponde únicamente a los movimientos de esta sesión, no al saldo real de una cuenta bancaria o billetera.

Ejemplo: inicial S/ 10, ingreso efectivo S/ 30, egreso efectivo S/ 30, ingreso digital S/ 70 y egreso digital S/ 10. Totales: ingresos S/ 100, egresos S/ 40, neto S/ 60. Efectivo esperado S/ 10; saldo digital de movimientos S/ 60.

## Caja diaria y cierre guiado

La vista principal destaca **Total registrado = efectivo esperado + resultado digital de la sesión**, incluyendo el fondo inicial. Debajo separa el efectivo físico a contar del digital registrado. Al cerrar usa el efectivo realmente contado más el resultado digital; la diferencia queda registrada por separado. Estos importes no certifican saldos bancarios ni equivalen a utilidad.

Ejemplo: fondo S/ 10, ingresos S/ 100 y egresos S/ 40: total registrado S/ 70, formado por efectivo S/ 10 y digital S/ 60 según el desglose del ejemplo anterior. Si realmente se cuentan S/ 9, el cierre registra un faltante de S/ 1 y un total al cierre de S/ 69.

El día termina a las 00:00 de America/Lima, no 24 horas después de abrir. La caja anterior pasa a mostrarse como **Pendiente de arqueo** por su fecha, sin recibir movimientos ni cobros; la validación del servidor ya protege cada escritura. No requiere un proceso programado ni que la aplicación permanezca encendida: al volver a consultarla se deriva ese estado de la fecha. La pantalla abierta revisa el cambio de día cada 30 segundos y al recuperar foco; el navegador puede suspender pestañas, pero el servidor mantiene el bloqueo.

Se permite confirmar el cierre de ayer durante el día siguiente y luego abrir la caja de hoy. El cierre financiero no es automático: contar el dinero físico requiere al responsable. El sistema no inventa un conteo ni confirma por sí solo que el efectivo coincide. Se conserva una sola sesión sin arqueo final a la vez.

El botón Contar efectivo y cerrar caja abre el formulario cercano al resumen. Se muestra al escribir si el conteo coincide o cuánto sobra/falta. El motivo es obligatorio únicamente con diferencia; el servidor conserva esa regla aunque se omita JavaScript. Si hay error, se conservan conteo y motivo y el formulario permanece abierto para corregir.

Verificación: suite Java de 77 pruebas; `scripts/verificar-caja-diaria.cjs` reproduce el ejemplo S/ 70, recuperación de cierre rechazado, cambio de día simulado sin pérdida del conteo, cierre con diferencia y revisión responsive en QA aislada.

Los formularios de otros movimientos y el historial de movimientos están en desplegables; la vista inicial prioriza total y cierre. Si se abren varias sesiones el mismo día, cada total corresponde a su propia sesión, no a la suma automática de todas las cajas de la fecha.
