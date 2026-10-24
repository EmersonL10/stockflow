# Seguimiento de la rúbrica del Proyecto Integrador

La rúbrica suma 20 puntos. Esta matriz identifica evidencia y trabajo pendiente; no constituye una nota ni una garantía de evaluación.

| Criterio | Máximo | Evidencia disponible | Pendiente |
| --- | ---: | --- | --- |
| Diseño | 3 | MVC, repositorios como DAO, ejemplos SOLID y seguridad en arquitectura.md | Sustentar con código y recorrido funcional |
| Recursos Java | 2 | POI, Guava, Commons y Logback aplicados, pruebas y librerias-java.md | Capturas y demostración del estudiante |
| Git | 3 | PR anteriores #4 y #5; modificaciones y archivos nuevos locales para esta funcionalidad | El estudiante debe crear rama, commits y PR de este trabajo y documentos; no se ha ejecutado Git automáticamente |
| Interfaces | 6 | Conductores, Vehículos, Turnos, Salidas, exportación y Caja operativa | Confirmar el alcance aprobado, mapear cada requisito y completar módulos comprometidos |
| Producto final | 4 | Código organizado, pruebas y documentación técnica | Alcance completo, documentos académicos alineados, datos de prueba identificados y dominio del estudiante |
| Sustentación | 2 | Guía de preguntas y recorrido | Ensayo individual y demostración |

## Alcance que debe confirmarse

No se dispone aquí de una lista validada de requisitos comprometidos al docente. Caja incluye apertura, ingresos y egresos, vínculo con cobros y cierre por sesión; no incluye conciliación bancaria automática, comprobantes ni devoluciones. Encomiendas y demás funciones no se declaran terminadas.

Agregar cada requisito aprobado con su ID, descripción, pantalla, clase responsable, prueba, estado y evidencia. No asignar porcentajes de cobertura hasta completar ese inventario.

## Preguntas para la sustentación

1. ¿Dónde comienza y termina el registro de una salida? Mostrar formulario, controlador, servicio y repositorio.
2. ¿Qué diferencia existe entre cuota generada, pendiente, pagada y cobrada durante un día?
3. ¿Cómo se impide un cobro duplicado? Mostrar transacción, bloqueo y prueba concurrente.
4. ¿Para qué sirve cada librería? Mostrar sus llamadas y descargar un Excel real.
5. ¿Qué ocurre si un texto exportado empieza por el signo igual? Mostrar su tipo STRING.
6. ¿Por qué el repositorio cumple el papel de DAO? Explicar la separación de acceso a datos.
7. ¿Qué principio SOLID se puede demostrar y dónde? Evitar afirmar TDD sin historial verificable.
8. ¿Cómo llegó un cambio de una rama funcional a develop? Explicar commit, push, PR y merge.
9. ¿Qué funciones siguen pendientes y por qué? Distinguir alcance implementado de propuestas.
