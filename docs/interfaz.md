# Interfaz operativa

La navegación agrupa Turnos, Salidas y Encomiendas en Operación; Caja en Cobros; y Conductores y Vehículos en Registros. En celular, el botón Menú permite desplegarla. Las acciones siguen respetando el rol autenticado.

## Recorrido principal

1. **Inicio:** accesos a registrar llegada, registrar salida, recibir encomienda y entrar a Caja. Los indicadores consultan datos persistidos: espera del día, salidas del día, envíos activos y cuotas pendientes de todas las fechas. Se actualizan al abrir la página.
2. **Turnos:** la cola permite identificar vehículo y conductor. Registrar salida desde una fila preselecciona ese turno si continúa disponible; el operador puede elegir otro.
3. **Salidas:** el formulario separa viaje y cuota. El medio de pago aparece al elegir pagar al salir. El listado agrupa información del vehículo y viaje; los filtros adicionales, detalle de cuotas y formulario de cobro son desplegables.
4. **Caja:** apertura, movimientos y arqueo conservan sus reglas. El cierre se despliega por separado y mantiene la confirmación definitiva.
5. **Encomiendas:** los formularios agrupan remitente, destinatario y paquete. El detalle prioriza estado y siguiente acción; contactos e historial se despliegan cuando se necesitan. La impresión expande los datos y restaura su estado después.

Las etiquetas visibles de estados son legibles; los valores persistidos de los enumerados se conservan. Las observaciones opcionales y la información secundaria ocupan menos espacio. Las tablas se adaptan a tarjetas en pantallas pequeñas.

## Verificación

- Suite Java: 77 pruebas, sin fallos ni errores.
- `scripts/verificar-interfaz.cjs`: 84 comprobaciones en seis anchos, sin desbordamiento horizontal ni errores JavaScript; incluye menú móvil, filtros, medio de pago condicional y cambio de día sin perder un formulario editado.
- Se verificaron cobros y cierre de Caja, y registro, edición, cobro, despacho, entrega, cancelación e impresión de Encomiendas en el entorno QA aislado.
- Capturas de revisión locales: `target/evidencias-interfaz/`. No se incluyen como archivos de producción.

Estas comprobaciones no sustituyen la revisión del operador con sus tareas habituales ni una auditoría completa de accesibilidad.

## Correcciones del flujo diario

- Fecha operativa compartida en `America/Lima`: la llegada nueva abre con fecha y hora actuales. La fecha no es editable. El servidor rechaza llegadas retroactivas, cambios a la fecha original y salidas nuevas sobre turnos antiguos.
- La cola consulta en base de datos únicamente pendientes de hoy. El historial mantiene las fechas originales. Un aviso informa del cambio de día en formularios; las consultas sin edición se recargan. La comprobación se realiza cada 30 segundos y al volver a la pestaña, según la ejecución del navegador; una pestaña suspendida se comprueba al reactivarla.
- Salidas ofrece Hoy, Por fecha y Todos los viajes. La búsqueda vacía informa si existe historial y ofrece ampliar la consulta. Los viajes aparecen antes del resumen diario; este resumen indica explícitamente su fecha y no cambia con filtros de ruta/placa/pago.
- Inicio incorpora Consultar viajes registrados con acceso a todas las fechas.
- Las encomiendas se asignan a vehículo y conductor antes de salir. El manifiesto de carga se confirma desde Registrar salida. La vista identifica al transportista antes y después del despacho.
- Caja destaca el total registrado, incluido el fondo inicial, y separa efectivo físico y movimientos digitales. Ingresos, egresos y fórmulas quedan en el desglose. El cierre permite verificar lo contado.

Caja destaca ahora un total de sesión, con efectivo y digital separados; ingresos, egresos y fórmulas quedan en Ver cómo se calculó. El cierre muestra diferencias en vivo, conserva valores si hay error y distingue caja operativa de hoy de caja anterior pendiente de arqueo. Ver [cierre guiado y corte diario](caja.md).
