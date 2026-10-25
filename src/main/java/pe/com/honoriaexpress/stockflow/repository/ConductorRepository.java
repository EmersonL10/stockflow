package pe.com.honoriaexpress.stockflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.honoriaexpress.stockflow.entity.Conductor;

import java.util.Optional;

public interface ConductorRepository extends JpaRepository<Conductor, Long> {

    Optional<Conductor> findByDni(String dni);

    Optional<Conductor> findByLicencia(String licencia);

    boolean existsByDni(String dni);

    boolean existsByLicencia(String licencia);

    boolean existsByDniAndIdNot(String dni, Long id);

    boolean existsByLicenciaAndIdNot(String licencia, Long id);
}