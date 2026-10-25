package pe.com.honoriaexpress.stockflow.service;

import pe.com.honoriaexpress.stockflow.entity.Vehiculo;

import java.util.List;
import java.util.Optional;

public interface VehiculoService {

    List<Vehiculo> listarTodos();

    Optional<Vehiculo> buscarPorId(Long id);

    Vehiculo guardar(Vehiculo vehiculo);

    boolean existePorPlaca(String placa);

    boolean existePorNumeroTuc(String numeroTuc);

    boolean existePlacaEnOtroVehiculo(
            String placa,
            Long id
    );

    boolean existeTucEnOtroVehiculo(
            String numeroTuc,
            Long id
    );

    void cambiarEstado(Long id);
}