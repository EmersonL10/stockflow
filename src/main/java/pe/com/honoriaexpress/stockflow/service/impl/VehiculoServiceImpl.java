package pe.com.honoriaexpress.stockflow.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.com.honoriaexpress.stockflow.entity.Vehiculo;
import pe.com.honoriaexpress.stockflow.repository.VehiculoRepository;
import pe.com.honoriaexpress.stockflow.service.VehiculoService;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VehiculoServiceImpl implements VehiculoService {

    private final VehiculoRepository vehiculoRepository;

    @Override
    public List<Vehiculo> listarTodos() {
        return vehiculoRepository.findAll();
    }

    @Override
    public Optional<Vehiculo> buscarPorId(Long id) {
        return vehiculoRepository.findById(id);
    }

    @Override
    public Vehiculo guardar(Vehiculo vehiculo) {
        return vehiculoRepository.save(vehiculo);
    }

    @Override
    public boolean existePorPlaca(String placa) {
        return vehiculoRepository.existsByPlaca(placa);
    }

    @Override
    public boolean existePorNumeroTuc(String numeroTuc) {
        return vehiculoRepository.existsByNumeroTuc(numeroTuc);
    }

    @Override
    public boolean existePlacaEnOtroVehiculo(
            String placa,
            Long id) {

        return vehiculoRepository
                .existsByPlacaAndIdNot(
                        placa,
                        id
                );
    }

    @Override
    public boolean existeTucEnOtroVehiculo(
            String numeroTuc,
            Long id) {

        return vehiculoRepository
                .existsByNumeroTucAndIdNot(
                        numeroTuc,
                        id
                );
    }

    @Override
    public void cambiarEstado(Long id) {

        Vehiculo vehiculo = vehiculoRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Vehículo no encontrado con id: " + id
                        )
                );

        vehiculo.setActivo(
                !vehiculo.getActivo()
        );

        vehiculoRepository.save(vehiculo);
    }
}