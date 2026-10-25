# Aplicación de cuatro librerías Java

## Apache POI

`ExportacionSalidasService` usa `XSSFWorkbook` para generar tres hojas: Salidas, Resumen por ruta y Lectura del reporte. Incluye encabezados, filas numéricas para importes, filtros y encabezado inmovilizado. El botón Exportar a Excel del listado solicita GET `/salidas/exportar` con los mismos campos del formulario de consulta.

No exporta la recaudación del transportista como ingreso de agencia. Los históricos sin cuota mantienen su importe vacío. Las cuotas anuladas no se suman en pagadas o pendientes. El resumen agrupa las cuotas de las salidas seleccionadas, no equivale al dinero cobrado durante un día ni a un cierre de Caja.

## Google Guava

`ConsultaSalidasService.agruparPorRuta` construye un `ImmutableListMultimap<String, Salida>`. Permite agrupar varias salidas bajo una ruta para construir la segunda hoja del Excel. La estructura del agrupamiento es inmutable; las entidades contenidas no se vuelven inmutables por este hecho.

## Apache Commons Lang

`ConsultaSalidasService.normalizar` utiliza `StringUtils.defaultString` y `normalizeSpace` para aceptar filtros vacíos o nulos y normalizar espacios. La comparación de placa ignora mayúsculas; la comparación de ruta normaliza los espacios de ambos lados. Se usa la misma selección para el listado y el Excel, sin reescribir los registros históricos. No se presenta esta normalización como una defensa contra SQL injection: el acceso a datos sigue usando JPA.

## Logback

El proveedor incluido por Spring Boot procesa las llamadas SLF4J. El controlador registra conflictos de integridad y el exportador registra únicamente cantidad de filas y rutas al terminar de construir el archivo. El registro se guarda en `logs/stockflow.log` o la ruta definida con `STOCKFLOW_LOG_FILE`.

La configuración rota por día y tamaño de 10 MB, conserva hasta 14 días y aplica un límite de archivo histórico de 100 MB. `logs/` está excluido de Git. El evento de exportación acredita generación del archivo en el servidor, no recepción efectiva por el usuario. No sustituye una auditoría financiera.

## Verificación

`ReporteSalidasTests` abre los bytes reales del Excel con POI y verifica tipos, resumen, históricos, cuotas anuladas, selección de filtros y límite de filas. `SalidasIntegrationTests.excelHttpRespetaFiltrosPermisosYDatosNumericos` verifica descarga, permisos, encabezados y contenido real usando MySQL aislado.

```powershell
.\mvnw.cmd test
.\mvnw.cmd dependency:tree "-Dincludes=org.apache.poi:*,com.google.guava:*,org.apache.commons:commons-lang3,ch.qos.logback:*"
```

El perfil test recrea tablas únicamente en `stockflow_test_codex`. No dirigirlo a una base operativa. Las versiones están declaradas en el POM y Logback está administrado por Spring Boot.

Verificación del 24/10/2026: 37 pruebas aprobadas, sin fallos, errores ni omisiones. Descarga autenticada en Chromium sobre localhost:18080 y base de QA, con cinco anchos de pantalla sin desbordamiento. Evidencias locales en `target/evidencias-librerias`; `scripts/verificar-exportacion.cjs` permite repetir la comprobación contra la instancia aislada. El archivo descargado contiene datos de QA, no datos de producción.

Referencias: [Apache POI](https://poi.apache.org/components/spreadsheet/quick-guide.html), [Guava](https://github.com/google/guava/releases), [Commons Lang](https://commons.apache.org/proper/commons-lang/apidocs/org/apache/commons/lang3/StringUtils.html), [Logback](https://logback.qos.ch/manual/).

La validación posterior del 25/10/2026 amplió la suite general a 77 pruebas aprobadas. El informe y el resumen saneado de Surefire están en entregables y evidencias; la cifra de 37 corresponde a la verificación histórica de la incorporación del reporte, no al total actual.
