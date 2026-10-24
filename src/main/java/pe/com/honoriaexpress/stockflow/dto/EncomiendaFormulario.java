package pe.com.honoriaexpress.stockflow.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

/** Entrada permitida. No contiene código, estados financieros, fechas ni usuarios. */
@Getter @Setter @NoArgsConstructor
public class EncomiendaFormulario {
    @NotBlank @Size(max = 100) private String remitente;
    @NotBlank @Pattern(regexp = "[A-Za-z0-9-]{6,20}", message = "Documento del remitente: use entre 6 y 20 letras, números o guiones")
    private String documentoRemitente;
    @NotBlank @Pattern(regexp = "[0-9+() -]{7,20}", message = "Revise el teléfono del remitente")
    private String telefonoRemitente;
    @NotBlank @Size(max = 100) private String destinatario;
    @NotBlank @Pattern(regexp = "[A-Za-z0-9-]{6,20}", message = "Documento del destinatario: use entre 6 y 20 letras, números o guiones")
    private String documentoDestinatario;
    @NotBlank @Pattern(regexp = "[0-9+() -]{7,20}", message = "Revise el teléfono del destinatario")
    private String telefonoDestinatario;
    @NotBlank @Size(max = 120) private String ruta;
    @NotBlank @Size(max = 250) private String descripcion;
    @NotNull @Min(1) @Max(1000) private Integer bultos = 1;
    @DecimalMin("0.001") @Digits(integer = 5, fraction = 3) private BigDecimal pesoKg;
    @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) private BigDecimal importe;
    @NotBlank @Pattern(regexp = "[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")
    private String solicitud;
}
