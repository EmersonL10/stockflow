# Seguimiento de la rúbrica del Proyecto Integrador

Evaluación de evidencias al 25/10/2026 sobre develop cc264aa. Los 20 puntos son el máximo de la rúbrica, no una calificación asignada por este documento.

| Criterio | Máximo | Evidencia concreta | Cierre pendiente |
| --- | ---: | --- | --- |
| Diseño | 3 | MVC; repositorios JPA como DAO; SRP y DIP como ejemplos SOLID; roles, BCrypt y CSRF | Sustentar tres enfoques con código. No afirmar TDD histórico |
| Recursos Java | 2 | Guava agrupa rutas, POI genera XLSX, Commons normaliza filtros, Logback procesa registros SLF4J | Demostración y capturas explicadas |
| Git | 3 | PR #4 a #10 fusionados a develop según las salidas compartidas; ramas y commits técnicos | Versionar los entregables de esta rama y preparar publicación final |
| Interfaces | 6 | RF01 a RF18 de alcance-trazabilidad.md; pantallas operativas y 84 comprobaciones responsive | Presentar la delimitación al docente; ocho casos QA de registros verificados |
| Producto final | 4 | 77 pruebas Java aprobadas; servicios transaccionales; documentación y trazabilidad | Revisión final del informe y dominio del código |
| Sustentación | 2 | Guía de recorrido y respuestas con clases y pruebas | Ensayo del estudiante y evaluación oral |

No hubo una lista previa comprometida por el estudiante. El alcance actual se delimita de forma explícita en alcance-trazabilidad.md, sin inventar una aprobación anterior. No se declara cobertura de módulos excluidos ni garantía de 20/20.

main, tag y release se preparan tras revisar los entregables. Son parte del flujo elegido; no se presentan como exigencias expresas de esta rúbrica. CI no reporta checks en el PR #10: las pruebas acreditadas son locales. La publicación en GitHub no implica despliegue de la aplicación.
