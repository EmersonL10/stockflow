# Cuota de agencia

Cada salida nueva genera S/ 10.00 por defecto, configurable con app.agencia.cuota. Puede pagarse al salir o quedar pendiente hasta el regreso. El cobro registra fecha/hora de Lima y usuario autenticado; un segundo cobro se rechaza.

La tarifa se copia a la salida y no cambia retroactivamente. Cancelar una salida pendiente anula su cuota. Las pagadas no pueden cancelarse sin regularización.

monto_recaudado conserva importes históricos del transporte y admite NULL para salidas nuevas. No se suma a la cuota ni al ingreso de agencia. Las salidas antiguas sin cuota quedan para revisión, sin crear deuda automática.

El registro de cobro todavía no crea movimiento de Caja, comprobante ni cierre. La futura integración debe referenciar el pago existente para evitar duplicar ingresos.

Consultar [operación, permisos y pruebas](salidas.md). El script manual scripts/separar-cuota-agencia.sql permite NULL en recaudación histórica; no constituye un sistema completo de migraciones.
