package pe.com.honoriaexpress.stockflow.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import pe.com.honoriaexpress.stockflow.entity.CajaSesion;
import java.util.*;

public interface CajaSesionRepository extends JpaRepository<CajaSesion, Long> {
    Optional<CajaSesion> findByClaveActiva(Integer clave);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CajaSesion c where c.claveActiva = 1")
    Optional<CajaSesion> bloquearAbierta();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CajaSesion c where c.id = :id")
    Optional<CajaSesion> bloquearPorId(@Param("id") Long id);
    List<CajaSesion> findAllByOrderByIdDesc(Pageable pagina);
}
