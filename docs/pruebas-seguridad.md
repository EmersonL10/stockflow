# Pruebas de software y seguridad

## Objetivo y alcance

Se comprobaron las reglas operativas, la integración de módulos y la protección de las operaciones de cobro. Los casos se relacionan con RF01–RF18 de [trazabilidad](alcance-trazabilidad.md). Se combinaron pruebas automatizadas, solicitudes al JAR desplegado y consultas a MySQL. Los resultados corresponden a los casos ejecutados; la aceptación de la agencia se evalúa por separado.

Herramientas: JUnit, Mockito, Spring Boot Test, MockMvc, Spring Security Test, consultas SQL de lectura, Actuator y PowerShell. Maven ejecuta las pruebas y Surefire conserva los resultados XML.

## Entornos y procedimiento reproducible

1. Definir las variables de entorno requeridas, sin publicar sus valores.
2. Ejecutar `./mvnw.cmd verify`. Las integraciones usan `stockflow_test` con `create-drop`; nunca dirigirlas a stockflow_db.
3. Generar un backup con `Respaldar.ps1`, verificar su hash y restaurar con `Restaurar-Prueba.ps1` en una base nueva.
4. Iniciar el JAR con el perfil `operacion`, puertos de prueba y `-BaseVerificacion stockflow_restore_FECHA`.
5. Registrar las tareas Windows y ejecutar una vez desde el programador para comprobar el mecanismo; esperar también una ejecución periódica del monitor.
6. Ejecutar `scripts/operacion/Verificar-Entrega.ps1`. Solo admite solicitudes activas si el despliegue registrado usa la base restaurada.
7. Revisar, analizar y publicar únicamente el resultado saneado. No copiar dumps, configuración cifrada, logs completos ni XML con propiedades de entorno.

## Resultados del 26 de noviembre de 2026

Evidencia: [resultados de ejecución](evidencias/entrega-final-2026-11-26.json). El archivo contiene fecha, casos, resultados, métricas y hash del JAR comprobado.

- 126 pruebas Java: 0 fallos, 0 errores y 0 omitidas.
- Revisión posterior del mismo día: Spring Boot actualizado a 4.2.0; propiedades de error migradas a `spring.web.error.*`. Maven verify y las 20 comprobaciones del JAR se repitieron y aprobaron con esa versión. El JSON identifica el hash desplegado.
- Incluyen 16 verificaciones nuevas de monitoreo y seguridad. La suite anterior de 110 permanece como antecedente.
- 20 comprobaciones adicionales sobre el JAR, vistas, tareas y recuperación, incluyendo autenticación real y lectura de ocho pantallas sin modificar operaciones.
- Backup de stockflow_db restaurado en una base independiente con 11 tablas. Conteos críticos coincidentes: 13 salidas, 18 movimientos y 7 encomiendas.
- Ensayo exploratorio de 25 lecturas de `/login`, máximo 5 simultáneas, sin respuestas distintas de 200. Revisar latencias exactas en el JSON. No representa capacidad máxima ni rendimiento de consultas autenticadas.

## Casos de seguridad y análisis

| Caso | Entrada / precondición | Esperado y comprobado | Análisis |
|---|---|---|---|
| Salud pública | GET sin credenciales a health | UP sin componentes internos | Permite detectar caída sin revelar detalles de MySQL. |
| Métricas privadas | GET a metrics sin autenticación | 401 en el JAR real | Impide lectura anónima; no confundir redirección al login con autenticación correcta. |
| Roles de negocio | Operativo o Caja consulta metrics | 403 en MockMvc | Tener sesión no concede permisos administrativos. |
| Administrador | GET de métricas autenticado | 200; tres gauges de negocio consultables | Métricas agregadas sin identificar personas; la credencial solo circula por loopback en el ensayo. |
| Endpoints sensibles | Administrador consulta env/configprops/heapdump/logfile/shutdown | 404 | La autenticación no sustituye reducir la superficie expuesta. |
| CSRF ausente/inválido | Administrador envía POST de cobro sin token o con token inválido | 403 y conteo de movimientos sin cambios en integración | Comprueba rechazo y ausencia de efecto financiero. |
| Rol sin permiso de cobro | Operativo envía POST con token válido | 403 y conteo sin cambios | Separa autorización de protección CSRF. |
| Solicitud real sin token | POST sin sesión/token en copia restaurada | 403; movimientos antes y después: 18 | Complementa MockMvc con el servidor HTTP real, sin tocar el original. |
| Correlación y cabeceras | GET al login con request ID válido | ID de respuesta y `nosniff` | Facilita correlación; los logs omiten cuerpo, parámetros y credenciales. |

Las suites previas también verifican capacidad y datos límite, cuota manipulada, transacciones, rollback, dobles cobros concurrentes, cierre concurrente, transporte inactivo, filtros, fechas y texto literal en Excel. Las integraciones con usuarios simulados comprueban autorización; no sustituyen probar todo el ciclo real de autenticación, recuperación de contraseñas o aceptación de usuarios.

## Hallazgos levantados

1. **Control de acceso a Actuator.** La incorporación del monitoreo requirió una cadena de seguridad propia: salud mínima pública y métricas solo para Administrador. Se verificaron accesos permitidos y rechazados. Es una medida incorporada al diseño, no una vulnerabilidad demostrada de la versión anterior.
2. **Error HTTP terminaba en login.** La prueba del JAR detectó redirecciones que ocultaban 401/403 detrás de un 200 final. Se permitió únicamente el despacho interno de errores en la cadena de la aplicación; la petición directa sigue autorizándose. La nueva ejecución real devuelve los estados esperados y no genera cobros.
3. **Bloqueo del JAR en Windows durante actualización.** El empaquetado no pudo reemplazar un archivo en uso. El arranque ahora copia cada JAR a una carpeta identificada por SHA256 y ejecuta esa copia, separada de target. Las pruebas pasaron antes; después se repitió la construcción completa.
4. **Primera muestra sin métrica HTTP.** Antes de recibir solicitudes web puede no existir `http.server.requests`. El monitor registra esa condición como ausencia de muestras, no como cero inventado ni caída de la aplicación.

## Límites y observaciones pendientes

El alcance de esta ejecución no incluye análisis de vulnerabilidades de dependencias, pruebas de penetración, carga prolongada ni aceptación de usuarios. El despliegue es local. La cuenta MySQL limitada está pendiente de creación; la instancia verificada usa las credenciales configuradas para la prueba.

Las pruebas manuales CP01 y CP02 mostraron una cuota pendiente sin ingreso y un cobro vinculado a su salida. El reintento manual de cobro duplicado quedó sin evidencia completa. La comprobación de duplicados incluida en este reporte corresponde a las pruebas automatizadas.

Para cada defecto nuevo se registrará el caso, requisito, entrada, resultado esperado y obtenido, evidencia, severidad, corrección y repetición de la prueba. Las observaciones del docente se incorporarán junto con la fecha y el resultado de su comprobación.

Referencias de configuración: [Spring Boot Actuator](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html) y [administración HTTP](https://docs.spring.io/spring-boot/reference/actuator/monitoring.html).
