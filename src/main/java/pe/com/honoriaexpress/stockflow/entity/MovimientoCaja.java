package pe.com.honoriaexpress.stockflow.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "caja_movimientos", uniqueConstraints = {
        @UniqueConstraint(name = "uk_movimiento_salida", columnNames = "salida_id"),
        @UniqueConstraint(name = "uk_movimiento_solicitud", columnNames = "solicitud")})
@Getter @Setter @NoArgsConstructor
public class MovimientoCaja {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "caja_id", nullable = false, updatable = false)
    private CajaSesion caja;
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "salida_id", updatable = false)
    private Salida salida;
    @Column(nullable = false, length = 60, updatable = false)
    private String solicitud;
    @Enumerated(EnumType.STRING) @Column(nullable = false, updatable = false, length = 20)
    private TipoMovimientoCaja tipo;
    @Enumerated(EnumType.STRING) @Column(nullable = false, updatable = false, length = 20)
    private MedioPago medio;
    @Column(nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal importe;
    @Column(nullable = false, length = 120, updatable = false)
    private String concepto;
    @Column(nullable = false, updatable = false)
    private LocalDateTime fecha;
    @Column(nullable = false, length = 100, updatable = false)
    private String registradoPor;
}
