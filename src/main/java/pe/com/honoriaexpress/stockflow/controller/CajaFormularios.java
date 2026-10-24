package pe.com.honoriaexpress.stockflow.controller;

import jakarta.validation.constraints.*;
import pe.com.honoriaexpress.stockflow.entity.*;
import java.math.BigDecimal;

/** Formularios de entrada: nunca enlazar las entidades financieras directamente. */
public final class CajaFormularios {
    private CajaFormularios() { }
    public record Apertura(
            @NotNull(message = "Indique el efectivo inicial") @DecimalMin("0.00")
            @Digits(integer = 8, fraction = 2, message = "Use hasta 8 enteros y 2 decimales") BigDecimal saldoInicial) { }
    public record Movimiento(
            @NotNull(message = "Seleccione el tipo de movimiento") TipoMovimientoCaja tipo,
            @NotNull(message = "Seleccione el medio de pago") MedioPago medio,
            @NotNull(message = "Indique el importe") @DecimalMin(value = "0.01", message = "El importe debe ser mayor que cero")
            @Digits(integer = 8, fraction = 2) BigDecimal importe,
            @NotBlank(message = "Indique el concepto") @Size(max = 120) String concepto,
            @NotBlank @Pattern(regexp = "[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}") String solicitud) { }
    public record Cierre(
            @NotNull(message = "Indique el efectivo contado") @DecimalMin("0.00")
            @Digits(integer = 12, fraction = 2) BigDecimal contado,
            @Size(max = 250) String observacion) { }
}
