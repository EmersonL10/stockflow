package pe.com.honoriaexpress.stockflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.honoriaexpress.stockflow.entity.Salida;

import java.util.List;

public interface SalidaRepository extends JpaRepository<Salida, Long> {
    @org.springframework.data.jpa.repository.Query("select s from Salida s join fetch s.turno t join fetch t.vehiculo join fetch t.conductor where t.fecha = :fecha or (s.fechaPago >= :inicio and s.fechaPago < :fin) order by s.id desc")
    List<Salida> consultarDia(@org.springframework.data.repository.query.Param("fecha") java.time.LocalDate fecha,
                             @org.springframework.data.repository.query.Param("inicio") java.time.LocalDateTime inicio,
                             @org.springframework.data.repository.query.Param("fin") java.time.LocalDateTime fin);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from Salida s where s.id = :id")
    java.util.Optional<Salida> buscarParaCobro(@org.springframework.data.repository.query.Param("id") Long id);

    List<Salida> findAllByOrderByIdDesc();

    boolean existsByTurnoId(Long turnoId);

    boolean existsByTurnoIdAndIdNot(Long turnoId, Long id);
}
