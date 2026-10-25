package pe.com.honoriaexpress.stockflow.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Entity
@Table(
        name = "vehiculos",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_vehiculo_placa",
                        columnNames = "placa"
                ),
                @UniqueConstraint(
                        name = "uk_vehiculo_tuc",
                        columnNames = "numero_tuc"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Placa del vehículo.
     * Debe ser única dentro del sistema.
     */
    @NotBlank(message = "La placa es obligatoria")
    @Size(
            max = 10,
            message = "La placa no debe superar los 10 caracteres"
    )
    @Column(
            nullable = false,
            length = 10
    )
    private String placa;

    /*
     * Marca comercial.
     * Ejemplo: Toyota, Hyundai, Nissan.
     */
    @NotBlank(message = "La marca es obligatoria")
    @Size(
            max = 60,
            message = "La marca no debe superar los 60 caracteres"
    )
    @Column(
            nullable = false,
            length = 60
    )
    private String marca;

    /*
     * Modelo del vehículo.
     */
    @NotBlank(message = "El modelo es obligatorio")
    @Size(
            max = 60,
            message = "El modelo no debe superar los 60 caracteres"
    )
    @Column(
            nullable = false,
            length = 60
    )
    private String modelo;

    /*
     * Año de fabricación.
     */
    @NotNull(message = "El año es obligatorio")
    @Min(
            value = 1980,
            message = "El año del vehículo no es válido"
    )
    @Max(
            value = 2100,
            message = "El año del vehículo no es válido"
    )
    @Column(nullable = false)
    private Integer anio;

    /*
     * Color del vehículo.
     */
    @NotBlank(message = "El color es obligatorio")
    @Size(
            max = 40,
            message = "El color no debe superar los 40 caracteres"
    )
    @Column(
            nullable = false,
            length = 40
    )
    private String color;

    /*
     * Capacidad máxima de pasajeros.
     */
    @NotNull(message = "La capacidad es obligatoria")
    @Min(
            value = 1,
            message = "La capacidad debe ser como mínimo 1"
    )
    @Max(
            value = 50,
            message = "La capacidad no puede superar los 50 pasajeros"
    )
    @Column(nullable = false)
    private Integer capacidadPasajeros;

    /*
     * Tarjeta Única de Circulación.
     * Se almacena como dato único para evitar duplicidad.
     */
    @NotBlank(message = "El número TUC es obligatorio")
    @Size(
            max = 30,
            message = "El número TUC no debe superar los 30 caracteres"
    )
    @Column(
            name = "numero_tuc",
            nullable = false,
            length = 30
    )
    private String numeroTuc;

    /*
     * Fecha de vencimiento de la TUC.
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "fecha_vencimiento_tuc")
    private LocalDate fechaVencimientoTuc;

    /*
     * Estado lógico.
     * No eliminaremos físicamente vehículos con historial.
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;
}