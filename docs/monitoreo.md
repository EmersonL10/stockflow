# Plan de monitoreo

## Herramientas y acceso

Spring Boot Actuator y Micrometer publican salud y métricas. `scripts/operacion/Monitorear.ps1` las consulta y registra cada muestra en `operacion-local/operaciones.jsonl` y la última en `monitoreo-ultimo.json`. El Programador de tareas de Windows ejecuta el muestreo cada cinco minutos bajo el usuario que cifró la configuración.

Estado al 26/11: el monitoreo periódico está deshabilitado. El lanzador ya permite ejecutarlo sin abrir ventanas. Hasta habilitar la tarea, las consultas se realizan manualmente con `Monitorear.ps1`.

La administración está en loopback. Las métricas requieren Administrador. No exponer credenciales Basic fuera de loopback sin TLS.

## Métricas y actuación

| Señal | Interpretación | Revisión y respuesta |
|---|---|---|
| `health` | Disponibilidad y conectividad con MySQL | Cada 5 minutos; si no es UP o hay timeout, revisar proceso, MySQL y espacio; escalar al responsable. El script sale con error y registra ERROR. |
| `http.server.requests` | Conteo, tiempo acumulado y máximo de solicitudes | Revisar aumento de 4xx/5xx y tiempos por URI normalizada. Una aplicación recién iniciada puede no tener esta métrica hasta su primera solicitud web. |
| `process.uptime` | Antigüedad del proceso | Descensos inesperados requieren revisar reinicios y errores. |
| `jvm.memory.used` | Memoria JVM utilizada | Analizar tendencia; el valor aislado no indica agotamiento. Comparar con límites de JVM antes de establecer alertas. |
| `process.cpu.usage` | Consumo de CPU del proceso | Si permanece alto durante varios muestreos, correlacionar con carga y consultas. |
| `stockflow.cuotas.pendientes` | Número de cuotas sin cobrar | Revisión diaria por Caja; no equivale a ingreso recibido. |
| `stockflow.vehiculos.activos` | Unidades habilitadas registradas | Contrastar con los registros de operación. |
| `stockflow.cajas.abiertas` | Cajas sin cierre registrado | Si es mayor que uno, investigar; una caja del día anterior requiere arqueo, no cierre automático ficticio. |

Si consultar las siete métricas tarda más de 5 segundos se registra WARN. Ese tiempo es la duración del **muestreo completo**, no la latencia de una página. El script registra fallas y códigos de salida; todavía no envía notificaciones externas. El responsable debe revisar las tareas y los archivos.

## Logs

El perfil de operación genera JSON ECS con marca temporal, nivel y logger. `RegistroHttpFilter` agrega evento, método, estado HTTP, duración e identificador de correlación. Los 4xx usan WARN y los 5xx ERROR; operaciones normales INFO. No registra cuerpo, parámetros, cookies, contraseñas ni nombres de personas.

La rotación existente conserva hasta 14 días con archivos de 10 MB y límite total de 100 MB. Revisar diariamente errores y semanalmente capacidad del almacenamiento. No copiar logs sin revisión a documentación pública.

## Responsable y evidencia

Responsable académico de operación: Nery Emerson Velásquez Pacheco. Responsable de negocio: quien administre la agencia durante la aceptación real. Registrar fecha, evento, diagnóstico, acción y nueva comprobación.

Usar `Verificar-Entrega.ps1` para conservar evidencia de salud, permisos de métricas, logs estructurados y muestreo. Los archivos de resultados deben describir la instancia y la fecha. Actuator aporta monitoreo técnico; los indicadores de Inicio son información de negocio y no lo sustituyen.
