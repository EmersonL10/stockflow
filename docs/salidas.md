# Gestión de Salidas

## Operación

1. Registrar la llegada en Turnos con fecha, sentido, vehículo y conductor.
2. Registrar salida eligiendo cualquier turno disponible. El orden de llegada es informativo.
3. Elegir Pucallpa - Honoria, Honoria - Pucallpa o escribir otra ruta.
4. Indicar hora real y pasajeros dentro de la capacidad del vehículo.
5. Registrar pendiente de pago o pagado al salir, según permisos.
6. Cobrar cuotas pendientes desde el listado cuando regrese el vehículo.

Para cobrar debe existir una caja abierta de la fecha actual. Seleccionar el medio real de cobro. El ingreso se registra automáticamente junto con el pago; no volver a registrarlo como movimiento manual. Ver [operación de Caja](caja.md).

Cada salida nueva queda EN_RUTA y genera la tarifa configurada en app.agencia.cuota (S/ 10.00 por defecto). El servidor fija el importe y lo conserva aunque cambie la tarifa.

## Resumen diario

Seleccionar fecha y pulsar Consultar en Salidas. El resumen muestra salidas realizadas, cuotas generadas, cuotas pagadas, pendientes y dinero cobrado ese día, usando hora de Lima.

Pagado de esas cuotas corresponde a salidas de la fecha elegida, aunque se paguen después. Cobrado durante este día corresponde a la fecha del pago, incluyendo salidas anteriores. No sumar ambos indicadores.

Para buscar deudas antiguas, seleccionar Todas las fechas y Pendiente de pago. Los filtros de ruta, placa y pago afectan la tabla; el resumen sigue mostrando el día completo.

## Permisos

| Acción | Administrador | Operativo | Caja |
| --- | --- | --- | --- |
| Consultar Salidas | Sí | Sí | Sí |
| Registrar o editar salida | Sí | Sí | No |
| Marcar pagado al salir | Sí | No | No |
| Cobrar cuota pendiente | Sí | No | Sí |

Los permisos se aplican en servidor y los formularios mantienen protección CSRF.

## Integridad

- Una salida por turno, con restricción única en MySQL y transacciones.
- Cobros y ediciones bloquean la misma fila para evitar pagos duplicados o perder el pago durante una edición simultánea.
- No se cambia el turno de una salida existente ni se modifica un turno que ya tenga salida.
- No se reabre una salida finalizada o cancelada ni se vuelve una ejecutada a PROGRAMADA.
- Cancelar una cuota pendiente la anula. Una pagada requiere regularización antes de cancelar; el sistema bloquea esa cancelación.
- La edición conserva cuota, estado de pago, fecha y usuario del cobro.
- Los importes históricos de transporte se conservan y no cuentan como ingreso de agencia. Las salidas históricas sin cuota no generan deuda retroactiva.

## Verificación

Ejecutar .\mvnw.cmd test. El perfil test usa exclusivamente stockflow_test_codex y RECREA SUS TABLAS. Crear previamente esa base vacía y configurar DB_PASSWORD y APP_ADMIN_PASSWORD por entorno. Nunca apuntar el perfil test a una base operativa.

10/10/2026: 32 pruebas aprobadas, incluyendo integración real con MySQL, permisos, CSRF, renderizado Thymeleaf y concurrencia de altas y cobros. El 24/10/2026 se amplió la suite a 37 pruebas aprobadas para incluir filtros, exportación a Excel y seguridad de la descarga.

Verificación con Chromium, puerto 18080 y base separada stockflow_qa_codex: alta pagada, alta pendiente, cobro posterior y 54 comprobaciones de ancho en nueve pantallas (1920, 1366, 1024, 768, 390 y 320 píxeles). Capturas locales en target/qa-results. El script scripts/verificar-navegador.cjs requiere Playwright, credenciales por entorno y vehículos QA-001 y QA-002; escribe contra localhost:18080. Reservar ese puerto para QA, nunca para una instancia operativa.

## Alcance futuro

La consulta dispone de Exportar a Excel con los mismos filtros y hasta 10000 salidas. Ver [librerías aplicadas y verificaciones](librerias-java.md). El archivo incluye detalle y resumen por ruta; el resumen por ruta no equivale a un cierre de Caja.

Existe Caja operativa con aperturas, movimientos y cierre por sesión. Aún no hay conciliación bancaria automática, comprobantes fiscales, devoluciones ni auditoría completa de cada edición. Para producción hace falta sustituir ddl-auto=update por migraciones versionadas, configurar respaldo/restauración y validar la política de documentos vencidos y vehículos inactivos.
