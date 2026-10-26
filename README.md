
# StockFlow

Sistema web para la gestión operativa y administrativa de Honoria Express S.A.C., desarrollado para centralizar el control de conductores, vehículos, turnos, salidas, encomiendas y movimientos de caja.

## Tecnologías

- Java 21 y Spring Boot
- Spring MVC y Spring Data JPA
- Hibernate y MySQL
- Thymeleaf, HTML, CSS y JavaScript
- Maven, Git y GitHub

## Funcionalidades

- **Conductores:** registro, actualización, consulta e inactivación.
- **Vehículos:** gestión de unidades, propietarios e historial.
- **Turnos:** registro y organización de llegadas.
- **Salidas:** control de viajes, pasajeros y cuota de agencia.
- **Encomiendas:** recepción, asignación, despacho y entrega.
- **Caja:** apertura, cobros, movimientos y cierre con arqueo.
- **Reportes:** consultas por fechas y exportación a Excel.
- **Seguridad:** autenticación y autorización mediante roles.

## Arquitectura

El sistema aplica MVC, DAO y principios SOLID mediante la separación de controladores, servicios, repositorios y vistas.

Documentación técnica:

- [Arquitectura](docs/arquitectura.md)
- [Requisitos y trazabilidad](docs/alcance-trazabilidad.md)
- [Librerías Java](docs/librerias-java.md)

## Ejecución local

**Requisitos:** Java 21, MySQL y Maven.

1. Crear las bases de datos `stockflow_db` y `stockflow_test` en MySQL.
2. Configurar las variables de entorno `DB_PASSWORD` y `APP_ADMIN_PASSWORD`.
3. Ejecutar la aplicación:

```powershell
.\mvnw.cmd spring-boot:run
```

4. Abrir `http://localhost:8080`.

## Pruebas

Ejecutar las pruebas automatizadas:

```powershell
.\mvnw.cmd test
```

Última ejecución verificada: 26 de octubre de 2026.

**Resultado:** 109 pruebas aprobadas, sin fallos ni errores.

## Informe académico

[Informe del proyecto StockFlow](entregables/StockFlow_Informe.docx)
