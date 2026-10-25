# Gestión de Encomiendas

## Alcance

Registro de envíos con código interno, remitente y destinatario, documentos, teléfonos, ruta, descripción, bultos, peso opcional e importe del servicio. Permite consulta paginada, edición antes del cobro/asignación, seguimiento, cobro completo, entrega y cancelación previa al despacho.

El código `ENC-00000001` se genera en el servidor a partir del identificador persistido. Es interno y no es numeración de comprobante fiscal. El importe se registra como ingreso de la agencia por el servicio de encomienda; no representa el valor del contenido ni incluye un cálculo de reparto con el transportista.

## Usuarios y permisos

| Operación | Administrador | Operativo | Caja |
| --- | --- | --- | --- |
| Consultar listado, detalle y constancia interna | Sí | Sí | Sí |
| Registrar y editar antes del cobro/despacho | Sí | Sí | No |
| Asignar transporte, confirmar carga y llegada | Sí | Sí | No |
| Entregar o cancelar según reglas | Sí | Sí | No |
| Cobrar el servicio | Sí | No | Sí |

Los permisos se aplican en las rutas HTTP y en los métodos del servicio. El seguimiento y los datos personales no se publican mediante una consulta anónima. Las páginas responden `Cache-Control: no-store`.

## Flujo operativo

1. Registrar remitente, destinatario y datos del envío. El sistema crea estado RECIBIDA y pago PENDIENTE; no requiere caja abierta para recibir el paquete.
2. Verificar datos antes de cobrar o despachar. La edición queda bloqueada después de cualquiera de esas acciones.
3. Cobrar en origen o destino desde el detalle, por Administrador o Caja. Debe existir una caja abierta de hoy en Lima. Se declara Efectivo, Yape, Plin o Transferencia y se registra el importe completo.
4. Elegir vehículo y conductor desde un turno en espera de hoy y de la misma ruta. El paquete pasa a ASIGNADA y muestra placa, conductor, fecha y orden. Se puede retirar la asignación antes de salir, incluso si el servicio está pagado; no se revierte su pago.
5. Abrir Registrar salida, revisar el manifiesto y confirmar la carga. Se valida la lista de identificadores revisada: si cambia mientras el formulario está abierto, hay que volver a revisarla. La salida y todos sus paquetes se confirman en una misma transacción; pasan a EN_TRANSITO. No se agregan paquetes a un viaje que ya salió.
6. Si la asignación queda pendiente de un día anterior, retirar el transporte y registrar/asignar una llegada de hoy. No se cambia la fecha histórica ni se traslada el paquete automáticamente.
7. Confirmar llegada únicamente cuando se recibe físicamente en destino. El estado pasa a DISPONIBLE.
8. Para entregar, comprobar el pago completo y el documento del destinatario. El operador ingresa nombre y documento de quien recibe. El documento debe coincidir con el registrado. Se guardan esos datos, la fecha y el evento de entrega; el estado pasa a ENTREGADA.

La coincidencia de documento es una validación de datos, no una verificación con un registro de identidad externo. El operador sigue siendo responsable de verificar la identidad física. No se implementa entrega a terceros autorizados.

## Estados independientes

- Envío: RECIBIDA → ASIGNADA → EN_TRANSITO → DISPONIBLE → ENTREGADA.
- Cancelación: únicamente RECIBIDA y con pago PENDIENTE; exige motivo y cambia a CANCELADA y pago ANULADA.
- Pago: PENDIENTE → PAGADA mediante cobro completo. No cambia automáticamente al despachar o confirmar llegada.

No se puede cobrar dos veces, entregar sin pago, reentregar, saltar estados, reasignar una encomienda despachada ni cancelar un envío ya cobrado. No hay eliminación de registros ni reapertura desde la aplicación. La salida con encomiendas asociadas tampoco se puede cancelar desde Salidas.

## Caja y concurrencia

El pago, su fecha/usuario, el evento de seguimiento y el movimiento de ingreso se confirman en la misma transacción. Si Caja no está abierta o quedó en otra fecha, todo el cobro revierte y el envío sigue pendiente. El cobro se registra en la sesión que recibe el dinero, no en la fecha del registro o del viaje.

`caja_movimientos.encomienda_id` es único. El movimiento tiene origen Encomienda, separado de Salida y de un movimiento manual. No volver a ingresar ese importe manualmente. Los medios digitales no incrementan el efectivo esperado del arqueo.

Se bloquea la encomienda antes de cobrar o cambiar su estado y se usa el bloqueo de Caja para coordinar el cobro con el cierre. La asignación y su retiro bloquean primero Turno y después Encomienda. El registro de salida bloquea la cola y sus encomiendas asignadas; confirma manifiesto, ruta y fecha. Si falla el cobro de la cuota de salida, también revierte el despacho. Se impide modificar un turno que tenga paquetes asignados y cambiar la ruta de una salida que los transporta. El registro inicial tiene un UUID de solicitud único para rechazar reenvíos del mismo formulario.

## Datos y arquitectura

- `EncomiendaFormulario` y `EntregaEncomiendaFormulario`: DTOs de entrada. No reciben código, estados, fecha de pago, usuario ni movimiento de Caja desde el formulario.
- `EncomiendaController`: solicitudes, formularios, mensajes y redirecciones.
- `EncomiendaServiceImpl`: validaciones, estados y transacciones.
- `EncomiendaRepository`: bloqueos y filtros mediante Specifications y paginación en MySQL, 20 registros por página.
- `EncomiendaEventoRepository`: historial de acciones, fecha, usuario y detalle. La asignación y el retiro también generan eventos. Los eventos de edición registran que se editó; no guardan una copia completa anterior de cada campo.
- `encomiendas.turno_asignado_id`: referencia opcional al turno previo a la salida. Los paquetes históricos con `salida_id` conservan su vínculo y se muestran desde ese viaje. Se agrega ASIGNADA al estado. `turnos.ruta_operativa` permite rutas escritas además de los dos sentidos habituales. El desarrollo actualiza estas columnas al reiniciar con `ddl-auto=update`; no se recargan datos ni se alteran sus fechas.
- `encomiendas` y `encomienda_eventos`: tablas de envíos y eventos. Se agrega una referencia nullable a Encomienda en Caja; los movimientos previos se conservan.

Todos los textos se presentan con escape HTML mediante Thymeleaf. Los filtros de código escapan comodines de LIKE; los datos personales no se envían a logs. Los documentos admiten entre 6 y 20 letras/números/guiones; no se aplica validación de dígito de control de documentos. Los teléfonos deben contener entre 7 y 15 dígitos. Importe positivo con hasta 8 enteros y 2 decimales; peso opcional positivo con hasta 5 enteros y 3 decimales; entre 1 y 1000 bultos.

## Pruebas y evidencias

`EncomiendasIntegrationTests` ejecuta validaciones, flujo completo, rollback del cobro, permisos y CSRF, protección de campos, paginación, cancelación y concurrencia de registros, pagos, cierre y despacho/cancelación de Salida. Las pruebas existentes de Caja y Salidas se mantienen y limpian solo la base exclusiva de pruebas.

```powershell
.\mvnw.cmd test
```

El perfil test recrea las tablas en `stockflow_test_codex`; nunca debe dirigirse a una base operativa. `scripts/verificar-encomiendas.cjs` usa solo localhost:18080 y requiere `stockflow_qa_codex`, ninguna caja abierta y una salida EN_RUTA de hoy con ruta Pucallpa - Honoria.

Se verificó en QA el registro, edición, cobro Plin, vínculo en Caja, despacho, llegada, rechazo de documento incorrecto, entrega, cancelación sin cobro y estilo de impresión. Se comprobaron cuatro pantallas en seis anchos: 1920, 1366, 1024, 768, 390 y 320 píxeles. Las capturas están en `target/evidencias-encomiendas` y contienen únicamente datos sintéticos.

Suite final del 24/10/2026: 64 pruebas aprobadas, sin fallos, errores ni omisiones. Incluye las 49 pruebas anteriores y 15 de Encomiendas. La comprobación final de navegador verificó 24 combinaciones de pantalla y ancho con el módulo actualizado.

## Funciones no incluidas

No hay seguimiento GPS, prueba de entrega con firma/foto, consulta pública, notificaciones, devolución, pago parcial, cambio de destinatario tras el cobro, gestión de pérdidas/daños, reparto de ingresos, comprobantes fiscales, validación de identidad externa ni cajas independientes por sede. Una instalación con varias sedes y cajeros requerirá definir esas reglas antes de operar de esa forma.
