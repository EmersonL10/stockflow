package pe.com.honoriaexpress.stockflow.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(
        name = "salidas",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_salida_turno",
                        columnNames = "turno_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Salida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El turno es obligatorio")
    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(
            name = "turno_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_salida_turno")
    )
    private Turno turno;

    @Size(max = 120, message = "La ruta no puede superar los 120 caracteres")
    @Column(name = "ruta_real", length = 120)
    private String rutaReal;

    public String getRutaMostrada() {
        if (rutaReal != null && !rutaReal.isBlank()) return rutaReal;
        return turno.getRutaMostrada();
    }

    @NotNull(message = "La hora real de salida es obligatoria")
    @DateTimeFormat(pattern = "HH:mm")
    @Column(name = "hora_salida_real", nullable = false)
    private LocalTime horaSalidaReal;

    @NotNull(message = "La cantidad de pasajeros es obligatoria")
    @Min(value = 0, message = "La cantidad de pasajeros no puede ser negativa")
    @Column(name = "cantidad_pasajeros", nullable = false)
    private Integer cantidadPasajeros;

    @DecimalMin(value = "0.00", message = "El monto recaudado no puede ser negativo")
    @Digits(integer = 8, fraction = 2, message = "Use hasta 8 enteros y 2 decimales")
    @Column(
            name = "monto_recaudado",
            nullable = true,
            precision = 10,
            scale = 2
    )
    private BigDecimal montoRecaudado;

    // Separada de la recaudación del transportista. Nula en registros históricos no revisados.
    @Column(name = "cuota_agencia", precision = 10, scale = 2)
    private BigDecimal cuotaAgencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_cuota", length = 20)
    private EstadoCuota estadoCuota;

    @Transient
    private boolean pagoAlSalir;

    @Transient
    private boolean cargaConfirmada;
    @Transient
    private String cargaIds;

    @Transient
    @Builder.Default
    private MedioPago medioPago = MedioPago.EFECTIVO;

    private java.time.LocalDateTime fechaPago;

    @Column(length = 60)
    private String pagoRegistradoPor;

    @Builder.Default
    @NotNull(message = "El estado es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSalida estado = EstadoSalida.PROGRAMADA;

    @Size(
            max = 250,
            message = "La observación no puede superar los 250 caracteres"
    )
    @Column(length = 250)
    private String observacion;
}
