# Guía de sustentación de StockFlow

## Recorrido sugerido de ocho minutos

1. Problema y alcance (45 s). La agencia necesita saber qué vehículo espera, quién sale, qué paquetes lleva y qué ingresos cobra. Mostrar la delimitación de módulos.
2. Arquitectura (60 s). Vista Thymeleaf → controlador → servicio → repositorio → MySQL. Señalar MVC, DAO y ejemplos SRP/DIP. No afirmar que tener pruebas acredita TDD.
3. Viaje y encomienda (150 s). Registrar llegada de hoy, recibir paquete, asignar vehículo y conductor, revisar manifiesto y confirmar salida. Mostrar pago del envío independiente del estado de transporte.
4. Caja (90 s). Abrir con efectivo inicial, cobrar una cuota o encomienda, mostrar total y diferencia entre efectivo y digital. Contar, justificar diferencia si existe y cerrar. Explicar corte automático y arqueo manual.
5. Librerías y seguridad (75 s). Exportar XLSX; mostrar llamadas POI, Guava y Commons. Mostrar un log sin datos sensibles, roles y un POST protegido por CSRF.
6. Pruebas y Git (60 s). Mostrar BUILD SUCCESS de 77 pruebas y un caso de concurrencia. Recorrer rama, commit, push, PR #10 y merge. Explicar que GitHub no reportó checks automáticos.

## Respuestas que debes dominar

- ¿Cuánto ingresa a la agencia por salida? S/ 10 según configuración; no es el pasaje ni el total que recauda el conductor.
- ¿Se puede pagar al regreso? Sí. El cobro se registra con la fecha del pago y en la caja que recibe el dinero.
- ¿Quién lleva una encomienda? El conductor del vehículo asignado al turno; al partir queda vinculada a la salida con manifiesto confirmado.
- ¿Qué pasa si cambia la carga mientras registro la salida? Se compara la lista de IDs y se exige revisarla de nuevo.
- ¿Qué significa total de Caja? Efectivo esperado más movimientos digitales netos de esa sesión, incluido el fondo inicial; no es utilidad ni saldo certificado de banco.
- ¿Qué pasa a medianoche? La caja anterior deja de aceptar movimientos y queda pendiente de arqueo. La persona cuenta y confirma el cierre; no se inventa el efectivo real.
- ¿Qué ocurre si falla el cobro? La transacción revierte el pago y su movimiento; no queda un ingreso parcial.
- ¿Cómo se evitan duplicados? Bloqueos transaccionales y restricciones únicas; mostrar una prueba concurrente.
- ¿Qué parte cumple DAO? Las interfaces Repository de Spring Data JPA, que encapsulan acceso a datos.
- ¿Qué demuestra SOLID? SRP en consulta/exportación/reglas; DIP en controladores que reciben interfaces de servicio. No afirmar automáticamente los cinco principios.
- ¿Qué no hace el sistema? Conciliar bancos, emitir comprobantes fiscales o resolver todos los módulos que aparecen en la base histórica.

## Evidencia de registros completada

Se verificaron ocho casos en QA: registrar, editar, rechazar duplicados e inactivar un conductor ficticio y un vehículo ficticio. Las capturas y resultados están en evidencias-registros. El script verificar-registros.cjs permite reproducirlos. En el ensayo, explica cada secuencia y sus mensajes.

## Entrega

Verificar los datos académicos de la portada ya completada. Revisar que todas las capturas correspondan a la versión presentada. Mantener identificados los datos QA; no publicar credenciales, volcados de bases ni los SQL sintéticos como si fueran registros reales. Publicar el informe mediante el flujo de rama y PR antes de main/tag/release.
