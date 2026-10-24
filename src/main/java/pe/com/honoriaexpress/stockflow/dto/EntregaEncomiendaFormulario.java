package pe.com.honoriaexpress.stockflow.dto;

import jakarta.validation.constraints.*;

public record EntregaEncomiendaFormulario(
        @NotBlank @Size(max = 100) String receptor,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9-]{6,20}") String documento) { }
