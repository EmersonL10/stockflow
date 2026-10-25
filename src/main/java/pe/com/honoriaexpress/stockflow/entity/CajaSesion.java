package pe.com.honoriaexpress.stockflow.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Un turno de Caja para toda la agencia. La clave única nullable impide dos aperturas activas. */
@Entity
@Table(name = "caja_sesiones", uniqueConstraints = @UniqueConstraint(name = "uk_caja_activa", columnNames = "clave_activa"))
@Getter @Setter @NoArgsConstructor
public class CajaSesion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "clave_activa")
    private Integer claveActiva;
    @Column(nullable = false)
    private LocalDateTime apertura;
    private LocalDateTime cierre;
    @Column(nullable = false, length = 100)
    private String abiertaPor;
    @Column(length = 100)
    private String cerradaPor;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal saldoInicial;
    @Column(precision = 14, scale = 2)
    private BigDecimal efectivoEsperado;
    @Column(precision = 14, scale = 2)
    private BigDecimal efectivoContado;
    @Column(precision = 14, scale = 2)
    private BigDecimal diferencia;
    @Column(length = 250)
    private String observacionCierre;

    public boolean isPendienteArqueo() {
        return isAbierta() && apertura.toLocalDate().isBefore(pe.com.honoriaexpress.stockflow.service.FechaOperativa.hoy());
    }
    public boolean isOperativaHoy() {
        return isAbierta() && apertura.toLocalDate().equals(pe.com.honoriaexpress.stockflow.service.FechaOperativa.hoy());
    }
    public String getEstadoMostrado() {
        return !isAbierta() ? "Cerrada" : isPendienteArqueo() ? "Pendiente de arqueo" : "Abierta hoy";
    }
    public boolean isAbierta() { return cierre == null && Integer.valueOf(1).equals(claveActiva); }
}
