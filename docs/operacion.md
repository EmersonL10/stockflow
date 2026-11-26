# Operación de StockFlow

Entorno verificado: Windows, Java 21, MySQL 8 y PowerShell 7. La aplicación se despliega como JAR en el equipo local.

## Construcción y arranque

1. Definir `DB_PASSWORD` y `APP_ADMIN_PASSWORD` en la sesión, sin escribirlos en el repositorio.
2. Ejecutar `./mvnw.cmd verify`. Las pruebas usan **stockflow_test** y recrean sus tablas; no cambiar ese perfil a stockflow_db.
3. Ejecutar `pwsh -File scripts/operacion/Configurar.ps1`. Se guarda configuración local cifrada mediante DPAPI para el usuario actual. Los archivos están excluidos de Git y con permisos restringidos.
4. Preparar previamente el esquema. El perfil `operacion` utiliza `ddl-auto=validate`: no crea ni altera tablas. El usuario de aplicación debe disponer de acceso a los datos de ese esquema.
5. Ejecutar `pwsh -File scripts/operacion/Iniciar.ps1 -Puerto 18080 -PuertoMonitoreo 18081`.
6. Comprobar `http://127.0.0.1:18080/login` y `http://127.0.0.1:18081/actuator/health`. El primero debe responder 200 y el segundo UP. El mensaje de arranque del script no sustituye estas comprobaciones.
7. Para detener el proceso registrado: `pwsh -File scripts/operacion/Detener.ps1`. Para reiniciar, detener y volver a iniciar. El script no detiene otros procesos Java.

Para verificar sobre una copia restaurada agregar `-BaseVerificacion stockflow_restore_FECHA` al arranque. Este parámetro solo acepta el prefijo y fecha de una base de restauración. No confundir esa instancia con la aplicación operativa.

## Configuración y seguridad

- `application-operacion.properties` desactiva SQL detallado, activa caché de vistas y oculta mensajes y trazas de errores.
- Web y administración se enlazan a loopback. El puerto de administración no debe publicarse mediante firewall ni proxy.
- Salud anónima solo informa estado. Métricas requieren Administrador mediante Basic HTTP. En esta demostración HTTP solo se usa en loopback; cualquier exposición remota requiere TLS o un túnel seguro.
- `env`, `configprops`, `logfile`, `heapdump` y `shutdown` no se exponen.
- El resto de formularios conserva protección CSRF. Una redirección o un código 200 final no acredita que una operación haya tenido éxito.
- `Crear-UsuarioAplicacion.ps1` prepara una cuenta con SELECT, INSERT, UPDATE y DELETE sobre el esquema configurado y guarda su contraseña cifrada. Su ejecución requiere autorización del responsable de MySQL. Esta cuenta todavía no está creada. El script no cambia cuentas existentes.
- DPAPI permite tareas del mismo usuario; no es un gestor de secretos para múltiples servidores. No adjuntar configuración, dumps, logs ni contraseñas a GitHub.

## Reversión y disponibilidad

Antes de actualizar: generar respaldo, identificar el JAR anterior y detener la instancia. Instalar solo un JAR cuya verificación haya pasado, arrancar y comprobar salud/login. Si falla, detener y volver al JAR anterior compatible con el esquema. Este proyecto todavía no automatiza migraciones de esquema ni una reversión de datos.

El arranque es manual y la aplicación permanece en segundo plano. Para disponer de inicio automático y funcionamiento independiente de una sesión se requiere configurar un servicio de Windows o un servidor dedicado.
