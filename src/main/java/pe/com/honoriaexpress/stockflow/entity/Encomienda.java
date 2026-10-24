package pe.com.honoriaexpress.stockflow.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "encomiendas", uniqueConstraints = {
        @UniqueConstraint(name = "uk_encomienda_codigo", columnNames = "codigo"),
        @UniqueConstraint(name = "uk_encomienda_solicitud", columnNames = "solicitud")})
@Getter @Setter @NoArgsConstructor
public class Encomienda {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(length = 30)
    private String codigo;
    @Column(nullable = false, length = 36, updatable = false)
    private String solicitud;
    @Column(nullable = false, length = 100)
    private String remitente;
    @Column(nullable = false, length = 20)
    private String documentoRemitente;
    @Column(nullable = false, length = 20)
    private String telefonoRemitente;
    @Column(nullable = false, length = 100)
    private String destinatario;
    @Column(nullable = false, length = 20)
    private String documentoDestinatario;
    @Column(nullable = false, length = 20)
    private String telefonoDestinatario;
    @Column(nullable = false, length = 120)
    private String ruta;
    @Column(nullable = false, length = 250)
    private String descripcion;
    @Column(nullable = false)
    private Integer bultos;
    @Column(precision = 8, scale = 3)
    private BigDecimal pesoKg;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal importe;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private EstadoEncomienda estado;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private EstadoPagoEncomienda estadoPago;
    @Column(nullable = false, updatable = false)
    private LocalDateTime registradaEn;
    private LocalDateTime fechaPago;
    @Column(length = 100)
    private String pagoRegistradoPor;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "salida_id")
    private Salida salida;
    private LocalDateTime entregadaEn;
    @Column(length = 100)
    private String recibidaPor;
    @Column(length = 20)
    private String documentoReceptor;
}
