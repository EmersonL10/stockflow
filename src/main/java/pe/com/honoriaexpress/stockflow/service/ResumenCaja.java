package pe.com.honoriaexpress.stockflow.service;

import java.math.BigDecimal;

public record ResumenCaja(BigDecimal ingresos, BigDecimal egresos, BigDecimal ingresosEfectivo,
                          BigDecimal egresosEfectivo, BigDecimal ingresosDigitales,
                          BigDecimal egresosDigitales, BigDecimal efectivoEsperado) {
    public BigDecimal totalRegistrado() { return efectivoEsperado.add(netoDigital()); }
    public BigDecimal neto() { return ingresos.subtract(egresos); }
    public BigDecimal netoDigital() { return ingresosDigitales.subtract(egresosDigitales); }
}
