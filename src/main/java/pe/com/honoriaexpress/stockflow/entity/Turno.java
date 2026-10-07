package pe.com.honoriaexpress.stockflow.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(
        name = "turnos",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_turno_fecha_ruta_orden",
                        columnNames = {
                                "fecha",
                                "sentido_ruta",
                                "numero_turno"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Turno {

    /*
     * ============================================================
     * IDENTIFICADOR
     * ============================================================
     */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    /*
     * ============================================================
     * FECHA DEL TURNO
     * ============================================================
     */

    @NotNull(message = "La fecha del turno es obligatoria")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(nullable = false)
    private LocalDate fecha;


    /*
     * ============================================================
     * ORDEN DEL TURNO
     * ============================================================
     */

    @NotNull(message = "El número de turno es obligatorio")
    @Min(
            value = 1,
            message = "El número de turno debe ser mayor o igual a 1"
    )
    @Column(
            name = "numero_turno",
            nullable = false
    )
    private Integer numeroTurno;


    /*
     * ============================================================
     * SENTIDO DE LA RUTA
     * ============================================================
     */

    @NotNull(message = "El sentido de la ruta es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(
            name = "sentido_ruta",
            nullable = false,
            length = 30
    )
    private SentidoRuta sentidoRuta;


    /*
     * ============================================================
     * HORA PROGRAMADA
     * ============================================================
     */

    @NotNull(message = "La hora programada es obligatoria")
    @DateTimeFormat(pattern = "HH:mm")
    @Column(
            name = "hora_programada",
            nullable = false
    )
    private LocalTime horaProgramada;


    /*
     * ============================================================
     * CONDUCTOR ASIGNADO
     * ============================================================
     */

    @NotNull(message = "Debe seleccionar un conductor")
    @ManyToOne(
            fetch = FetchType.EAGER,
            optional = false
    )
    @JoinColumn(
            name = "conductor_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_turno_conductor"
            )
    )
    private Conductor conductor;


    /*
     * ============================================================
     * VEHÍCULO ASIGNADO
     * ============================================================
     */

    @NotNull(message = "Debe seleccionar un vehículo")
    @ManyToOne(
            fetch = FetchType.EAGER,
            optional = false
    )
    @JoinColumn(
            name = "vehiculo_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_turno_vehiculo"
            )
    )
    private Vehiculo vehiculo;


    /*
     * ============================================================
     * ESTADO
     * ============================================================
     */

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private EstadoTurno estado = EstadoTurno.PROGRAMADO;


    /*
     * ============================================================
     * OBSERVACIÓN
     * ============================================================
     */

    @Size(
            max = 250,
            message = "La observación no debe superar los 250 caracteres"
    )
    @Column(length = 250)
    private String observacion;
}