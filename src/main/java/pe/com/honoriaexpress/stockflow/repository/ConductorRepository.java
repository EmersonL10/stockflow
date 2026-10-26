package pe.com.honoriaexpress.stockflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.honoriaexpress.stockflow.entity.Conductor;

import java.util.Optional;

public interface ConductorRepository extends JpaRepository<Conductor, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_READ)
    @org.springframework.data.jpa.repository.Query("select c from Conductor c where c.id = :id")
    Optional<Conductor> buscarParaOperacion(@org.springframework.data.repository.query.Param("id") Long id);

    Optional<Conductor> findByDni(String dni);

    Optional<Conductor> findByLicencia(String licencia);

    boolean existsByDni(String dni);

    boolean existsByLicencia(String licencia);

    boolean existsByDniAndIdNot(String dni, Long id);

    boolean existsByLicenciaAndIdNot(String licencia, Long id);
}
