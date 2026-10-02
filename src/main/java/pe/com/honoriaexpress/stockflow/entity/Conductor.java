package pe.com.honoriaexpress.stockflow.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
    name = "conductores",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_conductor_dni", columnNames = "dni"),
        @UniqueConstraint(name = "uk_conductor_licencia", columnNames = "licencia")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conductor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(
        regexp = "\\d{8}",
        message = "El DNI debe contener exactamente 8 dígitos"
    )
    @Column(nullable = false, length = 8)
    private String dni;

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(
        max = 100,
        message = "Los nombres no deben superar los 100 caracteres"
    )
    @Column(nullable = false, length = 100)
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(
        max = 100,
        message = "Los apellidos no deben superar los 100 caracteres"
    )
    @Column(nullable = false, length = 100)
    private String apellidos;

    @NotBlank(message = "La licencia es obligatoria")
    @Size(
        max = 20,
        message = "La licencia no debe superar los 20 caracteres"
    )
    @Column(nullable = false, length = 20)
    private String licencia;

    @Pattern(
        regexp = "^$|\\d{9}$",
        message = "El teléfono debe contener 9 dígitos"
    )
    @Column(length = 20)
    private String telefono;

    private LocalDate fechaVencimientoLicencia;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;
}