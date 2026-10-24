# Arquitectura de StockFlow

StockFlow usa Java 21, Spring Boot, MVC, Thymeleaf y MySQL. La base operativa configurada es `stockflow_db`; las pruebas automatizadas usan `stockflow_test_codex` y las verificaciones de navegador `stockflow_qa_codex`.

## MVC y separación en capas

- Vista: `resources/templates`, `static/css` y `static/js`. Recibe datos del controlador y presenta formularios, listados y resultados. No decide el importe de las cuotas.
- Controlador: `controller`. Recibe solicitudes, controla errores de formulario y selecciona vistas o respuestas de descarga.
- Modelo: entidades, servicios y repositorios. Representa datos, aplica reglas y persiste las operaciones.

Ejemplo: `salidas/formulario.html` → `SalidaController.guardar` → `SalidaServiceImpl.guardar` → `SalidaRepository.saveAndFlush` → MySQL → redirección → consulta y renderizado del listado.

## DAO mediante repositorios

`SalidaRepository` y los demás repositorios encapsulan el acceso a los datos mediante Spring Data JPA. Cumplen la responsabilidad de DAO sin duplicar una clase DAO adicional. Hibernate traduce las operaciones al motor MySQL. `buscarParaCobro` incorpora un bloqueo pesimista dentro de una transacción para proteger cobros y ediciones concurrentes.

## Principios SOLID demostrables

- Responsabilidad única: `ConsultaSalidasService` selecciona registros, `ExportacionSalidasService` construye el Excel y `SalidaServiceImpl` administra reglas operativas y transacciones.
- Inversión de dependencias: los controladores dependen de contratos `SalidaService` y `TurnoService`, que se inyectan por constructor. Los repositorios son interfaces. Las pruebas sustituyen esas dependencias por mocks.
- Segregación de responsabilidades: exportar no modifica una cuota ni hace cobros; guardar no construye una hoja de cálculo.

Estas son evidencias específicas; no se afirma cumplimiento automático de todos los principios SOLID. La existencia de pruebas tampoco acredita por sí sola un proceso TDD histórico.

## Seguridad

Spring Security autentica y aplica roles. BCrypt se usa para contraseñas; los formularios conservan CSRF. `@InitBinder` limita campos aceptados; el servidor decide cuota, estado y usuario del cobro. La descarga requiere Administrador, Operativo o Caja y responde `Cache-Control: no-store`. Los textos exportados se escriben como celdas STRING, nunca como fórmulas. Los logs no incluyen contraseñas, DNI ni contenido libre de formularios.

## Límites actuales

La consulta de Salidas todavía carga el historial antes de filtrar en memoria. El Excel limita la selección a 10000 filas antes de construir el libro, pero eso no limita la consulta inicial. Para volúmenes mayores se requiere filtrado y paginación en la base. El desarrollo usa `ddl-auto=update`; no reemplaza migraciones versionadas ni una estrategia de respaldos.
