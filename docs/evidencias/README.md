# Evidencias de validación

Los datos de las capturas de registros son ficticios y pertenecen a stockflow_qa_codex, nunca a la base operativa. La verificación del 25/10/2026 ejecutó ocho casos: alta, edición, rechazo de duplicados e inactivación de Conductores y Vehículos. resultados.json registra el resultado por caso y las imágenes muestran rechazo de duplicados y estado inactivo.

pruebas-java-2026-10-25.json resume los siete XML de Surefire de la última ejecución acreditada, con 77 pruebas y cero fallos, errores u omisiones. No incluye propiedades de entorno ni credenciales. No sustituye los casos del código fuente ni representa checks automáticos de GitHub.

Para repetir las pruebas Java usar el perfil test contra stockflow_test_codex. Para repetir la verificación de registros ejecutar scripts/verificar-registros.cjs con la instancia QA en localhost:18080 y las credenciales por entorno. No ejecutar esos scripts contra la base operativa.

El PDF incorpora capturas de Inicio, manifiesto y Caja, con explicaciones de cada secuencia. Las imágenes incluidas son de QA y sus cifras corresponden a ejemplos de verificación.
