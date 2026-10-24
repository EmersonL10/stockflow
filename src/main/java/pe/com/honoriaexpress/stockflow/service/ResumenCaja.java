package pe.com.honoriaexpress.stockflow.service;

import java.math.BigDecimal;

public record ResumenCaja(BigDecimal ingresos, BigDecimal egresos, BigDecimal ingresosEfectivo,
                          BigDecimal egresosEfectivo, BigDecimal ingresosDigitales,
                          BigDecimal egresosDigitales, BigDecimal efectivoEsperado) { }
