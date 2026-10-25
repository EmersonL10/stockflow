# Alcance y trazabilidad de StockFlow

Alcance delimitado para esta entrega a partir de las funciones implementadas. No se presenta como aprobación previa del docente. Las pruebas y revisiones permiten demostrar requisitos concretos; no se declara un porcentaje de cobertura del curso sin aceptación de esta delimitación.

| ID | Módulo | Requisito | Clase | Evidencia | Estado |
| --- | --- | --- | --- | --- | --- |
| RF01 | Acceso | Autenticación y permisos según rol | SecurityConfig.java | seguridadYCsrF | Implementado |
| RF02 | Conductores | Registrar, editar, consultar e inactivar | ConductorController.java | verificar-registros.cjs (registro, edición, duplicado e inactivación) | Implementado y verificado con ocho casos QA de registros |
| RF03 | Vehículos | Registrar, editar, consultar e inactivar | VehiculoController.java | verificar-registros.cjs (registro, edición, duplicado e inactivación) | Implementado y verificado con ocho casos QA de registros |
| RF04 | Turnos | Llegadas y cola exclusiva del día en Lima | TurnoServiceImpl.java | colaDiariaNoMezclaPendientesAntiguosNiReescribeSuFecha | Implementado |
| RF05 | Salidas | Registrar viaje, ruta libre y validar capacidad | SalidaServiceImpl.java | rechazaExcesoDeCapacidad | Implementado |
| RF06 | Salidas | Permitir elegir un vehículo sin imponer el primero | SalidaServiceImpl.java | permiteElegirVehiculoAunqueNoSeaElPrimero | Implementado |
| RF07 | Cuotas | Generar S/ 10 y cobrar al salir o después | SalidaServiceImpl.java | permitePagarAlRegreso | Implementado |
| RF08 | Historial | Consultar hoy, por fecha y todos los viajes | SalidaController.java | viajesDeHoyPorFechaYTodosNoOcultanElHistorial | Implementado |
| RF09 | Encomiendas | Registrar paquete, remitente y destinatario | EncomiendaServiceImpl.java | formularioHttpProtegeEstadosImportesDeCobroYDatosDeAuditoria | Implementado |
| RF10 | Transporte | Asignar vehículo y conductor antes de salir | EncomiendaServiceImpl.java | asignacionValidaRutaFechaYPermiteRetirarAntesDeSalir | Implementado |
| RF11 | Carga | Confirmar manifiesto y despachar en una transacción | SalidaServiceImpl.java | salidaExigeCargaConfirmadaYDespachaTodosLosPaquetesAtomicamente | Implementado |
| RF12 | Entrega | Confirmar llegada y entregar a destinatario pagado | EncomiendaServiceImpl.java | entregaRequiereDestinoPagoYDocumentoCorrecto | Implementado |
| RF13 | Caja | Apertura, movimientos y cobros vinculados | CajaServiceImpl.java | aperturaMovimientosDigitalesYArqueoConDiferencia | Implementado |
| RF14 | Arqueo | Comparar efectivo, conservar conteo y cerrar | CajaController.java | cierreFallidoConservaConteoYPermiteCorregirMotivo | Implementado |
| RF15 | Corte diario | Bloquear caja anterior y cerrar antes de abrir hoy | CajaSesion.java | corteDiarioMuestraArqueoPendienteYPermiteCerrarAyerYAbrirHoy | Implementado |
| RF16 | Excel | Exportar selección y resumen por ruta | ExportacionSalidasService.java | excelHttpRespetaFiltrosPermisosYDatosNumericos | Implementado |
| RF17 | Inicio | Mostrar indicadores persistidos de la operación | InicioService.java | inicioMuestraDatosRealesSinIndicadoresDeEjemplo | Implementado |
| RF18 | Concurrencia | Impedir pagos duplicados y operaciones parciales | CajaServiceImpl.java | cierreConcurrenteConPagoNoPierdeNiDesvinculaDinero | Implementado |

## Fuera de este alcance

Venta individual de pasajes, mantenimiento de vehículos, sanciones, facturación fiscal, conciliación automática con bancos/billeteras, devoluciones y despliegue productivo. La gestión de cuentas por una interfaz administrativa tampoco se declara implementada. No se atribuyen funciones al sistema por la mera existencia de tablas antiguas.
