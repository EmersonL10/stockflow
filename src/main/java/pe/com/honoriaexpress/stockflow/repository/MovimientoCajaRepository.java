package pe.com.honoriaexpress.stockflow.repository;

import org.springframework.data.jpa.repository.*;
import pe.com.honoriaexpress.stockflow.entity.MovimientoCaja;
import java.util.List;

public interface MovimientoCajaRepository extends JpaRepository<MovimientoCaja, Long> {
    List<MovimientoCaja> findByCajaIdOrderByIdDesc(Long id);
    @Query("select count(s) from Salida s where s.estadoCuota = pe.com.honoriaexpress.stockflow.entity.EstadoCuota.PAGADA "
            + "and not exists (select m.id from MovimientoCaja m where m.salida = s)")
    long contarPagosSinCaja();
    boolean existsBySalidaId(Long id);
}
