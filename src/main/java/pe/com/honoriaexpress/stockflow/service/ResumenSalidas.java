package pe.com.honoriaexpress.stockflow.service;

import java.math.BigDecimal;

public record ResumenSalidas(long realizadas, BigDecimal generadas, BigDecimal pagadas,
                             BigDecimal pendientes, BigDecimal cobradoHoy, long sinCuota) {}
