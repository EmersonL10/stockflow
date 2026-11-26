# Entrega final de StockFlow

Revisión: 26 de noviembre de 2026. Esta matriz relaciona la rúbrica con los archivos y resultados del proyecto.

| Criterio | Trabajo realizado y evidencia | Pendiente |
|---|---|---|
| Pruebas y seguridad (2) | 126 pruebas Java y 20 verificaciones aprobadas. [Reporte](pruebas-seguridad.md). | Incorporar las observaciones del docente y las constancias de los talleres. |
| Despliegue (2) | JAR Maven ejecutado con perfil operacion, esquema validado, puertos locales y arranque/parada documentados | Demostración local; no despliegue en Internet ni servicio de inicio automático de Windows. |
| Monitoreo (5) | Actuator, Micrometer, logs JSON, salud y métricas. [Plan](monitoreo.md). | La tarea periódica está deshabilitada; debe habilitarse para demostrar monitoreo continuo. |
| Mantenimiento (5) | Dump programado, SHA256, retención, restauración aislada y tareas con salida 0 | Programador de Windows equivalente a cron; requiere sesión iniciada. No se afirma cron Linux. Respaldo local sin copia externa. |
| Producto (4) | Módulos, roles, arquitectura y documentación de operación alineados | Alcance sujeto a revisión del docente y aceptación real; corregir el Word final si describe estados anteriores. |
| Sustentación (2) | Procedimientos y evidencia reproducible para explicar implementación | La explicación oral y el dominio individual corresponden al estudiante. |

## Comprobaciones de la entrega

1. Resultado de Maven y un caso de autorización con respuesta y datos sin cambios.
2. Login del JAR, perfil utilizado, validación de esquema y hash del artefacto.
3. Health UP, métrica protegida y muestra del log estructurado sin secretos.
4. Tareas Windows con próxima ejecución y salida 0; manifiesto del dump y base restaurada.
5. Relación entre estos resultados y los módulos de la agencia; límites identificados.

Los archivos para la entrega son el código y las pruebas de `src/`, los scripts de `scripts/operacion/`, los planes de `docs/` y los [resultados de ejecución](evidencias/entrega-final-2026-11-26.json). El informe del avance 3 se conserva en `docs/entregables/` y debe actualizarse con los cambios de esta etapa.

`operacion-local/`, los respaldos, las credenciales y los logs completos permanecen fuera del repositorio. La cuenta MySQL limitada tiene un script preparado, pero todavía no está creada.
