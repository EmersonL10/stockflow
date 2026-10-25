package pe.com.honoriaexpress.stockflow.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import pe.com.honoriaexpress.stockflow.entity.*;
import java.util.*;

public interface EncomiendaRepository extends JpaRepository<Encomienda, Long>, JpaSpecificationExecutor<Encomienda> {
    long countByEstadoIn(Collection<EstadoEncomienda> estados);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Encomienda e where e.id = :id")
    Optional<Encomienda> bloquear(@Param("id") Long id);
    boolean existsBySalidaId(Long salidaId);
    boolean existsByTurnoAsignadoIdAndEstado(Long turnoId, EstadoEncomienda estado);
    List<Encomienda> findByTurnoAsignadoIdAndEstadoOrderByIdAsc(Long turnoId, EstadoEncomienda estado);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Encomienda e where e.turnoAsignado.id = :id and e.estado = pe.com.honoriaexpress.stockflow.entity.EstadoEncomienda.ASIGNADA order by e.id")
    List<Encomienda> bloquearAsignadas(@Param("id") Long turnoId);
}
