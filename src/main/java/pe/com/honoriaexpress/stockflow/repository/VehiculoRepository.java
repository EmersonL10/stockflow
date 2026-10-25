package pe.com.honoriaexpress.stockflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.honoriaexpress.stockflow.entity.Vehiculo;

import java.util.Optional;

public interface VehiculoRepository
        extends JpaRepository<Vehiculo, Long> {

    Optional<Vehiculo> findByPlaca(String placa);

    Optional<Vehiculo> findByNumeroTuc(String numeroTuc);

    boolean existsByPlaca(String placa);

    boolean existsByNumeroTuc(String numeroTuc);

    boolean existsByPlacaAndIdNot(
            String placa,
            Long id
    );

    boolean existsByNumeroTucAndIdNot(
            String numeroTuc,
            Long id
    );
}