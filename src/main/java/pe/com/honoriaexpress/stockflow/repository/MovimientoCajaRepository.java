package pe.com.honoriaexpress.stockflow.repository;

import org.springframework.data.jpa.repository.*;
import pe.com.honoriaexpress.stockflow.entity.MovimientoCaja;
import java.util.List;

public interface MovimientoCajaRepository extends JpaRepository<MovimientoCaja, Long> {
    @Query("select sum(m.importe) from MovimientoCaja m where m.tipo = pe.com.honoriaexpress.stockflow.entity.TipoMovimientoCaja.INGRESO and m.fecha >= :inicio and m.fecha < :fin")
    java.math.BigDecimal sumarIngresosDelPeriodo(@org.springframework.data.repository.query.Param("inicio") java.time.LocalDateTime inicio,
                                               @org.springframework.data.repository.query.Param("fin") java.time.LocalDateTime fin);
    List<MovimientoCaja> findByFechaGreaterThanEqualAndFechaLessThanOrderByFechaDescIdDesc(
            java.time.LocalDateTime inicio, java.time.LocalDateTime fin, org.springframework.data.domain.Pageable limite);

    List<MovimientoCaja> findByCajaIdOrderByIdDesc(Long id);
    @Query("select count(s) from Salida s where s.estadoCuota = pe.com.honoriaexpress.stockflow.entity.EstadoCuota.PAGADA "
            + "and not exists (select m.id from MovimientoCaja m where m.salida = s)")
    long contarPagosSinCaja();
    boolean existsBySalidaId(Long id);
    boolean existsByEncomiendaId(Long id);
}
