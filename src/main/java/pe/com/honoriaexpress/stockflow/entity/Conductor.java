package pe.com.honoriaexpress.stockflow.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Entity
@Table(
        name = "conductores",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_conductor_dni",
                        columnNames = "dni"
                ),
                @UniqueConstraint(
                        name = "uk_conductor_licencia",
                        columnNames = "licencia"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conductor {

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
     * DNI
     * ============================================================
     */

    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(
            regexp = "\\d{8}",
            message = "El DNI debe contener exactamente 8 dígitos"
    )
    @Column(
            nullable = false,
            length = 8
    )
    private String dni;


    /*
     * ============================================================
     * NOMBRES
     * ============================================================
     */

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(
            max = 100,
            message = "Los nombres no deben superar los 100 caracteres"
    )
    @Column(
            nullable = false,
            length = 100
    )
    private String nombres;


    /*
     * ============================================================
     * APELLIDOS
     * ============================================================
     */

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(
            max = 100,
            message = "Los apellidos no deben superar los 100 caracteres"
    )
    @Column(
            nullable = false,
            length = 100
    )
    private String apellidos;


    /*
     * ============================================================
     * LICENCIA
     * ============================================================
     */

    @NotBlank(message = "La licencia es obligatoria")
    @Size(
            max = 20,
            message = "La licencia no debe superar los 20 caracteres"
    )
    @Column(
            nullable = false,
            length = 20
    )
    private String licencia;


    /*
     * ============================================================
     * TELÉFONO
     * ============================================================
     */

    @Pattern(
            regexp = "^$|\\d{9}$",
            message = "El teléfono debe contener 9 dígitos"
    )
    @Column(length = 20)
    private String telefono;


    /*
     * ============================================================
     * FECHA DE VENCIMIENTO DE LICENCIA
     * ============================================================
     */

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "fecha_vencimiento_licencia")
    private LocalDate fechaVencimientoLicencia;


    /*
     * ============================================================
     * ESTADO DEL CONDUCTOR
     * ============================================================
     */

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;
}