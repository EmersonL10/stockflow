# StockFlow

Sistema web para la gestión operativa de Honoria Express S.A.C.

Java 21, Spring Boot, Spring MVC, Spring Data JPA, Hibernate, MySQL, Thymeleaf, CSS propio responsive y Maven.

## Ejecución local

Crear stockflow_db en MySQL y configurar DB_PASSWORD y APP_ADMIN_PASSWORD por entorno. Opcionalmente APP_ADMIN_USERNAME y APP_ADMIN_EMAIL para la cuenta inicial. No guardar credenciales en el repositorio.

Ejecutar .\mvnw.cmd spring-boot:run y abrir http://localhost:8080. Revisar application.properties antes de usar otra base; desarrollo usa actualización automática del esquema.

## Salidas y pruebas

Ver [flujo, cobros, resumen diario, permisos y verificación](docs/salidas.md) y [cuota de agencia](docs/cuota-agencia.md).

El listado permite exportar a Excel la selección de Salidas y su resumen por ruta. Ver [cuatro librerías Java aplicadas](docs/librerias-java.md), [arquitectura MVC, DAO y SOLID](docs/arquitectura.md) y [seguimiento de la rúbrica](docs/matriz-rubrica.md).

## Caja

Administrador y Caja pueden abrir una sesión, registrar otros ingresos/egresos y cerrarla con arqueo. Los cobros nuevos de Salidas requieren caja abierta de hoy y crean su ingreso automáticamente, distinguiendo efectivo y medios digitales. Los pagos históricos no se importan automáticamente. Consultar [reglas y verificación de Caja](docs/caja.md).

Las pruebas usan stockflow_test_codex y recrean sus tablas. Crear esa base vacía antes de ejecutar .\mvnw.cmd test.

Los SQL de scripts son manuales. Los archivos demo o sintéticos contienen datos ficticios, aunque su presentación no incluya la palabra demo. No son identidades ni documentos verificados. No cargarlos en bases operativas ni volver a ejecutarlos indiscriminadamente. La aplicación no los ejecuta automáticamente.
