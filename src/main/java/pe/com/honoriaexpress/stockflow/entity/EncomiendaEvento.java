package pe.com.honoriaexpress.stockflow.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "encomienda_eventos")
@Getter @Setter @NoArgsConstructor
public class EncomiendaEvento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "encomienda_id", nullable = false, updatable = false)
    private Encomienda encomienda;
    @Column(nullable = false, length = 40, updatable = false)
    private String accion;
    @Column(nullable = false, updatable = false)
    private LocalDateTime fecha;
    @Column(nullable = false, length = 100, updatable = false)
    private String usuario;
    @Column(nullable = false, length = 250, updatable = false)
    private String detalle;
}
