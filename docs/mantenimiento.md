# Plan de mantenimiento y recuperación

## Automatización Windows

`Programar.ps1` registra tareas reales en el Programador de tareas de Windows:

| Tarea | Frecuencia | Resultado |
|---|---|---|
| StockFlow-BackupDiario | Diaria a las 23:30, hora local del equipo | Dump SQL y manifiesto con tamaño, fecha y SHA256. |
| StockFlow-Monitoreo | Cada 5 minutos | Salud y siete métricas; registro estructurado y código de salida. |

Configuración: `pwsh -File scripts/operacion/Programar.ps1 -PuertoMonitoreo 18081`. Retirada: agregar `-Quitar`. No se borran respaldos al retirar las tareas.

La automatización se implementó con el Programador de tareas de Windows. Si el taller exige cron de Linux, será necesario configurar y verificar ese entorno. Las tareas requieren la sesión del usuario iniciada. `StartWhenAvailable` permite recuperar ejecuciones omitidas cuando sea posible.

`Ejecutar-Oculto.vbs` lanza PowerShell sin ventana, espera su finalización y devuelve su código de salida al programador. Estado al 26/11: backup diario habilitado y monitoreo deshabilitado. La reactivación del monitoreo debe coordinarse con el usuario.

## Respaldo

Ejecutar `pwsh -File scripts/operacion/Respaldar.ps1`. Usa `mysqldump --single-transaction`, sin DROP, sin ALTER y sin bloqueo explícito de tablas. No cambiar estructura mientras se toma el dump. La copia incluye las tablas y datos del esquema, no los usuarios ni permisos globales de MySQL.

El archivo se escribe primero con extensión `.partial`. Solo con salida exitosa se convierte en `.sql` y se escribe su manifiesto. No se eliminan backups anteriores si falla. La retención de 14 días solo elimina dumps completos de la base configurada dentro de la carpeta local de respaldos y se ejecuta después de un respaldo exitoso.

Los archivos están en `operacion-local/backups`, excluidos de Git y restringidos al usuario/SYSTEM. **No están cifrados en reposo ni replicados fuera del equipo**; DPAPI protege las credenciales, no el contenido del dump. Antes de usar datos reales: añadir destino externo protegido y cifrado de disco o de respaldos. No publicar SQL con datos en GitHub.

## Restauración de prueba

Ejecutar `pwsh -File scripts/operacion/Restaurar-Prueba.ps1 -Archivo RUTA_DEL_SQL`. Se comprueba SHA256 y se crea una base nueva `stockflow_restore_FECHA`. Nunca restaura sobre stockflow_db ni elimina bases existentes. Se valida importación y conteos de salidas, movimientos y encomiendas. Una igualdad de conteos no demuestra igualdad de todos los valores; complementar con inspección funcional del JAR y revisión de registros seleccionados.

Si la fuente recibe nuevas operaciones después del backup, sus conteos pueden diferir legítimamente: comparar con una instantánea coherente, no atribuirlo automáticamente a una pérdida de datos.

La base restaurada se conserva para inspección. Su eliminación debe ser una acción explícita posterior del responsable, verificando el nombre exacto. Una restauración real por incidente requiere aprobar el punto de recuperación y detener escrituras; el script entregado solo realiza restauraciones de prueba.

## Objetivos y procedimientos

- Objetivo propuesto de pérdida máxima (RPO): hasta 24 h con backup diario exitoso. No es una garantía si el equipo estuvo apagado o la tarea falló.
- Objetivo inicial de recuperación (RTO): 30 min, pendiente de medir con volúmenes reales y un procedimiento completo. El tiempo del pequeño ensayo local no acredita ese objetivo en producción.
- Diario: revisar salida 0 de tareas, antigüedad del último dump y errores del monitoreo.
- Semanal: restaurar una copia, verificar integridad e inspeccionar recorridos; comprobar espacio y retención.
- Por actualización: ejecutar pruebas, generar backup, registrar versión, desplegar, comprobar salud y conservar el JAR anterior.
- Mensual: revisar permisos, dependencias, espacio, copia externa y necesidades de la agencia.

Los respaldos locales y la cuenta root de mantenimiento sirven para la demostración controlada. En operación real se debe usar una cuenta de backup con los permisos mínimos necesarios, y una cuenta distinta para la aplicación.
